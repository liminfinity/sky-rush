package com.skyrush.social;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record SocialSettings(
    @Value("${skyrush.social.presence-seconds:90}") int presenceSeconds,
    @Value("${skyrush.social.big-win:200}") BigDecimal bigWin,
    @Value("${skyrush.social.high-level:8}") int highLevel,
    @Value("${skyrush.social.daily-rounds:3}") int dailyRounds,
    @Value("${skyrush.social.daily-wins:2}") int dailyWins,
    @Value("${skyrush.social.daily-points:150}") int dailyPoints,
    @Value("${skyrush.social.daily-level:6}") int dailyLevel,
    @Value("${skyrush.social.daily-cashout:2.5}") BigDecimal dailyCashout) {
  public SocialSettings {
    if (presenceSeconds < 30
        || presenceSeconds > 300
        || bigWin.signum() <= 0
        || highLevel < 1
        || highLevel > 9
        || dailyRounds < 1
        || dailyRounds > 25
        || dailyWins < 1
        || dailyWins > 10
        || dailyPoints < 1
        || dailyPoints > 10000
        || dailyLevel < 1
        || dailyLevel > 9
        || dailyCashout.compareTo(BigDecimal.ONE) < 0
        || dailyCashout.compareTo(BigDecimal.TEN) > 0)
      throw new IllegalArgumentException("Invalid skyrush.social thresholds; see docs/social.md");
  }
}
