package com.skyrush.wallet;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class WalletService {
  private final JdbcTemplate jdbc;

  public WalletService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Wallet get(UUID userId) {
    return new Wallet(
        userId,
        jdbc.queryForObject(
            "SELECT balance FROM skyrush.wallets WHERE user_id = ?", BigDecimal.class, userId));
  }

  /** Caller holds the user lock. Unique ledger entries additionally prevent duplicate effects. */
  public void apply(UUID userId, UUID roundId, String kind, BigDecimal amount, Instant now) {
    jdbc.update(
        "INSERT INTO skyrush.wallet_entries(id, user_id, round_id, kind, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        userId,
        roundId,
        kind,
        amount,
        Timestamp.from(now));
    int changed =
        jdbc.update(
            "UPDATE skyrush.wallets SET balance = balance + ? WHERE user_id = ? AND balance + ? >= 0",
            amount,
            userId,
            amount);
    if (changed != 1) throw new IllegalStateException("Wallet invariant violated");
  }
}
