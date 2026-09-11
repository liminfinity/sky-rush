package com.skyrush.auth;

import com.skyrush.shared.GameException;
import com.skyrush.users.CurrentUserService;
import com.skyrush.users.DemoUserService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  public record Account(
      UUID id, String username, String displayName, Instant createdAt, boolean evaluator) {}

  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final Clock clock;
  private final CurrentUserService current;
  private final BigDecimal initial;
  private final String dummyHash;

  public AccountService(
      JdbcTemplate jdbc,
      PasswordEncoder passwords,
      Clock clock,
      CurrentUserService current,
      @Value("${skyrush.demo-initial-balance}") BigDecimal initial) {
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.clock = clock;
    this.current = current;
    this.initial = initial;
    dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  public Account me() {
    return get(current.id());
  }

  private Account get(UUID id) {
    return jdbc.queryForObject(
        "SELECT id,username,display_name,created_at FROM skyrush.users WHERE id=?",
        (r, n) ->
            new Account(
                id,
                r.getString("username"),
                r.getString("display_name"),
                r.getTimestamp("created_at").toInstant(),
                id.equals(DemoUserService.DEMO_ID)),
        id);
  }

  @Transactional
  public Account register(String username, String displayName, String password) {
    UUID id = UUID.randomUUID();
    try {
      jdbc.update(
          "INSERT INTO skyrush.users(id,username,display_name,password_hash,created_at) VALUES(?,?,?,?,?)",
          id,
          username.toLowerCase(java.util.Locale.ROOT),
          displayName.strip(),
          passwords.encode(password),
          Timestamp.from(clock.instant()));
    } catch (org.springframework.dao.DuplicateKeyException ex) {
      throw new GameException(409, "USERNAME_TAKEN", "Это имя пользователя уже занято");
    }
    jdbc.update("INSERT INTO skyrush.wallets(user_id,balance) VALUES(?,?)", id, initial);
    jdbc.update("INSERT INTO skyrush.reward_progress(user_id,fragments) VALUES(?,0)", id);
    return get(id);
  }

  public Account login(String username, String password) {
    var rows =
        jdbc.queryForList(
            "SELECT id,password_hash FROM skyrush.users WHERE username=?",
            username.toLowerCase(java.util.Locale.ROOT));
    String hash = rows.isEmpty() ? dummyHash : (String) rows.getFirst().get("password_hash");
    boolean matches = passwords.matches(password, hash);
    if (rows.isEmpty() || !matches)
      throw new GameException(401, "LOGIN_FAILED", "Неверное имя пользователя или пароль");
    return get((UUID) rows.getFirst().get("id"));
  }
}
