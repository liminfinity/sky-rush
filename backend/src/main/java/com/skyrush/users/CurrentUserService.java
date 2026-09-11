package com.skyrush.users;

import com.skyrush.shared.GameException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
  private final JdbcTemplate jdbc;

  public CurrentUserService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public UUID id() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser"))
      throw new GameException(401, "UNAUTHENTICATED", "Войдите в аккаунт");
    try {
      return UUID.fromString(auth.getName());
    } catch (IllegalArgumentException ex) {
      throw new GameException(401, "UNAUTHENTICATED", "Войдите в аккаунт");
    }
  }

  public User get() {
    return lookup(id(), false);
  }

  public User lock() {
    return lock(id());
  }

  /** Internal background processing only; never bound to a client-supplied identity. */
  public User lock(UUID id) {
    return lookup(id, true);
  }

  private User lookup(UUID id, boolean lock) {
    return jdbc
        .query(
            "SELECT id,display_name,created_at FROM skyrush.users WHERE id=?"
                + (lock ? " FOR UPDATE" : ""),
            (rs, n) ->
                new User(
                    rs.getObject("id", UUID.class),
                    rs.getString("display_name"),
                    rs.getTimestamp("created_at").toInstant()),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new GameException(401, "UNAUTHENTICATED", "Аккаунт не найден"));
  }
}
