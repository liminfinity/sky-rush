package com.skyrush.gamemath;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Random;

public final class JavaRandomProvider implements RandomProvider {
  private final Random random;

  public JavaRandomProvider() {
    random = new SecureRandom();
  }

  public JavaRandomProvider(long seed) {
    random = new Random(seed);
  }

  @Override
  public synchronized BigDecimal nextUnit() {
    return BigDecimal.valueOf(random.nextDouble());
  }
}
