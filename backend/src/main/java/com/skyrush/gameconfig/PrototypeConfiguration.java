package com.skyrush.gameconfig;

import java.math.BigDecimal;

/**
 * Optional modules are separate from immutable gameplay snapshots. Offers keep their own price
 * snapshot.
 */
public record PrototypeConfiguration(
    String gameId, String name, String type, boolean active, Upsell upsell) {
  public record Upsell(
      boolean enabled,
      BigDecimal minWinAmount,
      int popupTimeoutSeconds,
      BigDecimal ticketPrice,
      int maxTickets,
      BigDecimal payoutFraction) {}

  public void validate() {
    require(
        gameId != null && gameId.matches("[a-z0-9-]{1,32}"),
        "game_id: use 1–32 lowercase letters/digits/hyphens");
    require(name != null && !name.isBlank() && name.length() <= 80, "name: 1–80 characters");
    require("BALLOON_CRASH".equals(type), "type must be BALLOON_CRASH");
    require(upsell != null, "upsell is required");
    money(upsell.minWinAmount(), "min_win_amount");
    money(upsell.ticketPrice(), "ticket_price");
    require(
        upsell.popupTimeoutSeconds() >= 3 && upsell.popupTimeoutSeconds() <= 60,
        "popup_timeout_seconds: 3–60");
    require(upsell.maxTickets() >= 1 && upsell.maxTickets() <= 100, "max_tickets: 1–100");
    require(
        upsell.payoutFraction() != null
            && upsell.payoutFraction().signum() > 0
            && upsell.payoutFraction().compareTo(BigDecimal.ONE) <= 0
            && upsell.payoutFraction().stripTrailingZeros().scale() <= 4,
        "payout_fraction: >0 to 1, at most four decimals");
  }

  private static void money(BigDecimal v, String n) {
    require(
        v != null
            && v.signum() > 0
            && v.compareTo(new BigDecimal("1000000")) <= 0
            && v.stripTrailingZeros().scale() <= 2,
        n + ": 0.01–1000000, at most two decimals");
  }

  private static void require(boolean b, String m) {
    if (!b) throw new IllegalArgumentException(m);
  }
}
