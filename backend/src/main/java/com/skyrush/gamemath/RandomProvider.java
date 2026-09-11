package com.skyrush.gamemath;

import java.math.BigDecimal;

@FunctionalInterface
public interface RandomProvider {
  /** Uniform sample in [0, 1). Implementations must be safe for concurrent callers. */
  BigDecimal nextUnit();
}
