package com.skyrush.gamemath;

import com.skyrush.gameconfig.GameConfiguration.Points;

public final class PointsCalculator {
  public long calculate(
      int level, boolean cashedOut, boolean boosterActive, int booster, Points rules) {
    return (long) level * rules.pointsPerLevel()
        + (cashedOut ? rules.pointsCashoutBonus() : 0)
        + (boosterActive ? rules.boosterActivationBonuses().get(booster) : 0);
  }
}
