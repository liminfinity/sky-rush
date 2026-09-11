package com.skyrush.gamemath;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PayoutCalculator {
  public BigDecimal calculate(BigDecimal stake, BigDecimal multiplier) {
    return stake.multiply(multiplier).setScale(2, RoundingMode.DOWN);
  }
}
