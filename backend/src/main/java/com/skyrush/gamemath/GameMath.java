package com.skyrush.gamemath;

import com.skyrush.gameconfig.GameConfiguration;
import com.skyrush.gameconfig.GameTheme;
import java.math.BigDecimal;
import java.time.Instant;

/** Pure evaluation: polling frequency never determines points or crossed levels. */
public final class GameMath {
  public final CrashPointGenerator crashes = new CrashPointGenerator();
  public final BoosterPositionGenerator boosters = new BoosterPositionGenerator();
  public final MultiplierCalculator multipliers = new MultiplierCalculator();
  public final PayoutCalculator payouts = new PayoutCalculator();
  public final RewardCalculator rewards = new RewardCalculator();
  private final PointsCalculator points = new PointsCalculator();

  public record Flight(
      BigDecimal multiplier, int level, boolean boosterActive, long points, boolean crashed) {}

  public Flight evaluate(
      GameConfiguration config,
      GameTheme theme,
      Instant start,
      Instant crashAt,
      BigDecimal crashBase,
      int booster,
      Integer boosterLevel,
      Instant cashoutAt,
      Instant now) {
    boolean crashed = !now.isBefore(crashAt);
    // A boundary simultaneous with crash is not crossed. Resolution is one server millisecond.
    Instant flightTime = crashed ? crashAt.minusMillis(1) : now;
    var thresholds = config.themes().get(theme).levelThresholds();
    BigDecimal base = multipliers.base(start, flightTime, config.growth().multiplierPerSecond());
    int level = multipliers.level(base, thresholds);
    Instant activationCutoff = cashoutAt == null ? flightTime : cashoutAt;
    int activationLevel =
        multipliers.level(
            multipliers.base(start, activationCutoff, config.growth().multiplierPerSecond()),
            thresholds);
    boolean activated = booster > 1 && boosterLevel != null && activationLevel >= boosterLevel;
    return new Flight(
        multipliers.effective(crashed ? crashBase : base, booster, activated),
        level,
        activated,
        points.calculate(level, cashoutAt != null, activated, booster, config.points()),
        crashed);
  }
}
