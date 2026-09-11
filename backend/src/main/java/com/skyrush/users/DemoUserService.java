package com.skyrush.users;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoUserService implements ApplicationRunner {
  public static final UUID DEMO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final boolean enabled;
  private final BigDecimal initialBalance;

  public DemoUserService(
      JdbcTemplate jdbc,
      Clock clock,
      @Value("${skyrush.demo-enabled}") boolean enabled,
      @Value("${skyrush.demo-initial-balance}") BigDecimal initialBalance) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.enabled = enabled;
    this.initialBalance = initialBalance;
    if (initialBalance.signum() <= 0
        || initialBalance.scale() > 2
        || initialBalance.compareTo(new BigDecimal("1000000000")) > 0)
      throw new IllegalArgumentException(
          "Demo initial balance must be positive, at most 1 billion, with at most two decimal places");
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled) return;
    // Startup seeding is idempotent; an existing demo wallet is never reset.
    jdbc.update(
        "INSERT INTO skyrush.users(id, display_name, created_at,username,password_hash) VALUES (?, 'SkyRush Demo', ?, 'demo', '*') ON CONFLICT (id) DO NOTHING",
        DEMO_ID,
        Timestamp.from(clock.instant()));
    jdbc.update(
        "UPDATE skyrush.users SET password_hash=? WHERE id=? AND password_hash='*'",
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("demo12345"),
        DEMO_ID);
    jdbc.update(
        "INSERT INTO skyrush.wallets(user_id, balance) VALUES (?, ?) ON CONFLICT (user_id) DO NOTHING",
        DEMO_ID,
        initialBalance);
    jdbc.update(
        "INSERT INTO skyrush.reward_progress(user_id, fragments) VALUES (?, 0) ON CONFLICT (user_id) DO NOTHING",
        DEMO_ID);
  }
}
