package com.skyrush.gamemath;

import java.math.BigDecimal;
import java.util.List;

public final class BoosterPositionGenerator {
  public Integer generate(int multiplier, List<BigDecimal> weights, RandomProvider random) {
    if (multiplier == 1) return null;
    BigDecimal target =
        weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add).multiply(random.nextUnit());
    BigDecimal cumulative = BigDecimal.ZERO;
    for (int i = 0; i < weights.size(); i++) {
      cumulative = cumulative.add(weights.get(i));
      if (target.compareTo(cumulative) < 0) return i + 1;
    }
    throw new IllegalArgumentException("Random sample or booster weights outside valid range");
  }
}
