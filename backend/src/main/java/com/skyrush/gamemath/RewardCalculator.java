package com.skyrush.gamemath;

import com.skyrush.gameconfig.GameConfiguration.RewardRules;
import java.math.BigDecimal;

public final class RewardCalculator {
  public record Grant(
      int fragmentsGranted, long fragmentsRedeemed, long fragmentsRemaining, BigDecimal bonus) {}

  public Grant calculate(boolean win, long held, RewardRules rules) {
    int grant = win ? rules.fragmentsWin() : rules.fragmentsLoss();
    long total = Math.addExact(held, grant);
    long conversions = total / rules.fragmentsPerBonus();
    long redeemed = conversions * rules.fragmentsPerBonus();
    return new Grant(
        grant,
        redeemed,
        total - redeemed,
        rules.bonusAmount().multiply(BigDecimal.valueOf(conversions)).setScale(2));
  }
}
