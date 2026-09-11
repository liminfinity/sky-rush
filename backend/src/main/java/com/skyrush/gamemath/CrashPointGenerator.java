package com.skyrush.gamemath;

import com.skyrush.gameconfig.GameConfiguration.CrashModel;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CrashPointGenerator {
  public BigDecimal generate(CrashModel model, RandomProvider random) {
    return model
        .minMultiplier()
        .add(model.maxMultiplier().subtract(model.minMultiplier()).multiply(random.nextUnit()))
        .setScale(4, RoundingMode.DOWN);
  }
}
