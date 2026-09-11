package com.skyrush.gamemath;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class MultiplierCalculator {
  public BigDecimal base(Instant start, Instant now, BigDecimal rate) {
    long millis = Math.max(0, Duration.between(start, now).toMillis());
    return BigDecimal.ONE
        .add(rate.multiply(BigDecimal.valueOf(millis, 3)))
        .setScale(4, RoundingMode.DOWN);
  }

  public Instant crashAt(Instant start, BigDecimal crash, BigDecimal rate) {
    long millis =
        crash
            .subtract(BigDecimal.ONE)
            .multiply(BigDecimal.valueOf(1000))
            .divide(rate, 0, RoundingMode.CEILING)
            .longValueExact();
    return start.plusMillis(millis);
  }

  public int level(BigDecimal base, List<BigDecimal> thresholds) {
    return (int) thresholds.stream().filter(t -> base.compareTo(t) >= 0).count();
  }

  public BigDecimal effective(BigDecimal base, int booster, boolean active) {
    return base.multiply(BigDecimal.valueOf(active ? booster : 1)).setScale(4, RoundingMode.DOWN);
  }
}
