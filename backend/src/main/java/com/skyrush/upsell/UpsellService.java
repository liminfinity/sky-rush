package com.skyrush.upsell;

import com.skyrush.gameconfig.PrototypeConfigService;
import com.skyrush.gamemath.TicketQuoteCalculator;
import com.skyrush.rounds.RoundRepository;
import com.skyrush.rounds.RoundState;
import com.skyrush.shared.GameException;
import com.skyrush.users.CurrentUserService;
import com.skyrush.wallet.WalletService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpsellService {
  public record Offer(
      UUID id,
      UUID roundId,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal total,
      Instant expiresAt,
      String status,
      long remainingSeconds) {}

  public record View(Offer offer, long tickets) {}

  private final JdbcTemplate jdbc;
  private final CurrentUserService users;
  private final WalletService wallets;
  private final RoundRepository rounds;
  private final PrototypeConfigService configs;
  private final Clock clock;

  public UpsellService(
      JdbcTemplate jdbc,
      CurrentUserService users,
      WalletService wallets,
      RoundRepository rounds,
      PrototypeConfigService configs,
      Clock clock) {
    this.jdbc = jdbc;
    this.users = users;
    this.wallets = wallets;
    this.rounds = rounds;
    this.configs = configs;
    this.clock = clock;
  }

  public View offer(UUID sessionId, UUID roundId) {
    UUID user = users.lock().id();
    var prior =
        jdbc.query(
            "SELECT * FROM skyrush.ticket_offers WHERE user_id=? AND session_id=?",
            this::map,
            user,
            sessionId);
    if (!prior.isEmpty())
      return new View(
          prior.getFirst().roundId().equals(roundId) ? prior.getFirst() : null, inventory(user));
    var r =
        rounds
            .find(user, roundId)
            .orElseThrow(() -> new GameException(404, "ROUND_NOT_FOUND", "Round not found"));
    var rules = configs.current().configuration().upsell();
    if (!rules.enabled()
        || r.state != RoundState.COMPLETED_WIN
        || r.payout.compareTo(rules.minWinAmount()) < 0
        || java.util.Objects.requireNonNull(
                jdbc.queryForObject(
                    "SELECT count(*) FROM skyrush.ticket_offers WHERE round_id=? AND status='PURCHASED'",
                    Integer.class,
                    roundId),
                "Required database value is null")
            > 0) return new View(null, inventory(user));
    int quantity =
        TicketQuoteCalculator.quantity(r.payout, r.stake, wallets.get(user).bonusBalance(), rules);
    if (quantity < 1) return new View(null, inventory(user));
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO skyrush.ticket_offers(id,user_id,session_id,round_id,quantity,unit_price,total,expires_at,status) VALUES(?,?,?,?,?,?,?,?, 'OFFERED')",
        id,
        user,
        sessionId,
        roundId,
        quantity,
        rules.ticketPrice(),
        rules.ticketPrice().multiply(BigDecimal.valueOf(quantity)),
        Timestamp.from(clock.instant().plusSeconds(rules.popupTimeoutSeconds())));
    return new View(required(user, id), inventory(user));
  }

  public View decide(UUID id, UUID sessionId, boolean accept) {
    UUID user = users.lock().id();
    Offer offer = required(user, id);
    UUID owner =
        java.util.Objects.requireNonNull(
            jdbc.queryForObject(
                "SELECT session_id FROM skyrush.ticket_offers WHERE id=?", UUID.class, id),
            "Required database value is null");
    if (!owner.equals(sessionId))
      throw new GameException(409, "OFFER_SESSION", "Offer belongs to another browser session");
    if (offer.status().equals("PURCHASED"))
      return new View(offer, inventory(user)); // Idempotent retry, including after timeout.
    if (!offer.status().equals("OFFERED"))
      throw new GameException(409, "OFFER_CLOSED", "Offer has expired or was declined");
    if (accept) {
      if (java.util.Objects.requireNonNull(
              jdbc.queryForObject(
                  "SELECT count(*) FROM skyrush.ticket_offers WHERE round_id=? AND status='PURCHASED'",
                  Integer.class,
                  offer.roundId()),
              "Required database value is null")
          > 0)
        throw new GameException(409, "OFFER_CLOSED", "Tickets for this round already purchased");
      if (wallets.get(user).bonusBalance().compareTo(offer.total()) < 0)
        throw new GameException(
            409, "INSUFFICIENT_BALANCE", "Not enough bonus balance for tickets");
      int count =
          jdbc.update(
              "UPDATE skyrush.wallets SET balance=balance-? WHERE user_id=? AND balance>=?",
              offer.total(),
              user,
              offer.total());
      if (count != 1) throw new IllegalStateException("Wallet invariant violated");
      jdbc.update(
          "UPDATE skyrush.ticket_offers SET status='PURCHASED',purchased_at=? WHERE id=?",
          Timestamp.from(clock.instant()),
          id);
    } else jdbc.update("UPDATE skyrush.ticket_offers SET status='DECLINED' WHERE id=?", id);
    return new View(required(user, id), inventory(user));
  }

  public View inventory() {
    UUID user = users.get().id();
    return new View(null, inventory(user));
  }

  private long inventory(UUID user) {
    return java.util.Objects.requireNonNull(
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(quantity),0) FROM skyrush.ticket_offers WHERE user_id=? AND status='PURCHASED'",
            Long.class,
            user),
        "Required database value is null");
  }

  private Offer required(UUID user, UUID id) {
    return jdbc
        .query("SELECT * FROM skyrush.ticket_offers WHERE user_id=? AND id=?", this::map, user, id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new GameException(404, "OFFER_NOT_FOUND", "Offer not found"));
  }

  private Offer map(ResultSet rs, int row) throws SQLException {
    Instant expires = rs.getTimestamp("expires_at").toInstant();
    String status = rs.getString("status");
    if (status.equals("OFFERED") && !clock.instant().isBefore(expires)) status = "EXPIRED";
    return new Offer(
        rs.getObject("id", UUID.class),
        rs.getObject("round_id", UUID.class),
        rs.getInt("quantity"),
        rs.getBigDecimal("unit_price"),
        rs.getBigDecimal("total"),
        expires,
        status,
        Math.max(0, (Duration.between(clock.instant(), expires).toMillis() + 999) / 1000));
  }
}
