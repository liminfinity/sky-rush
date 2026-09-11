package com.skyrush.gamemath;

import com.skyrush.gameconfig.PrototypeConfiguration.Upsell;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Optional simulated product budget. No change to round payout. */
public final class TicketQuoteCalculator {
  public static int quantity(
      BigDecimal payout, BigDecimal stake, BigDecimal balance, Upsell rules) {
    BigDecimal budget = payout.multiply(rules.payoutFraction()).min(stake).min(balance);
    return budget
        .divide(rules.ticketPrice(), 0, RoundingMode.DOWN)
        .min(BigDecimal.valueOf(rules.maxTickets()))
        .intValueExact();
  }
}
