package com.skyrush.gameconfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable value graph, also persisted as each round's configuration snapshot. */
public record GameConfiguration(
    Map<GameTheme, Theme> themes,
    List<BetOption> betOptions,
    CrashModel crashModel,
    Growth growth,
    Points points,
    RewardRules reward) {
  public GameConfiguration {
    themes = Map.copyOf(themes);
    betOptions = List.copyOf(betOptions);
  }

  public record Theme(List<BigDecimal> levelThresholds, List<BigDecimal> boosterPositionWeights) {
    public Theme {
      levelThresholds = List.copyOf(levelThresholds);
      boosterPositionWeights = List.copyOf(boosterPositionWeights);
    }
  }

  public record CrashModel(BigDecimal minMultiplier, BigDecimal maxMultiplier) {}

  public record Growth(BigDecimal multiplierPerSecond) {}

  public record Points(
      int pointsPerLevel, int pointsCashoutBonus, Map<Integer, Integer> boosterActivationBonuses) {
    public Points {
      boosterActivationBonuses = Map.copyOf(boosterActivationBonuses);
    }
  }

  public record RewardRules(
      int fragmentsWin, int fragmentsLoss, int fragmentsPerBonus, BigDecimal bonusAmount) {}

  public void validate() {
    require(
        themes.keySet().equals(Set.of(GameTheme.RED, GameTheme.GREEN)),
        "themes must contain RED and GREEN only");
    if (crashModel == null || growth == null || points == null || reward == null) {
      throw new IllegalArgumentException("all model sections are required");
    }
    decimal(
        crashModel.minMultiplier(),
        "min_multiplier",
        new BigDecimal("1.0001"),
        new BigDecimal("100"),
        4);
    decimal(
        crashModel.maxMultiplier(),
        "max_multiplier",
        crashModel.minMultiplier(),
        new BigDecimal("100"),
        4);
    require(
        crashModel.maxMultiplier().compareTo(crashModel.minMultiplier()) > 0,
        "max_multiplier must exceed min_multiplier");
    decimal(
        growth.multiplierPerSecond(),
        "multiplier_per_second",
        new BigDecimal("0.01"),
        new BigDecimal("100"),
        4);
    for (var entry : themes.entrySet()) {
      var theme = entry.getValue();
      int size = entry.getKey() == GameTheme.RED ? 12 : 9;
      require(
          theme.levelThresholds().size() == size,
          entry.getKey() + " must have " + size + " levels");
      require(
          theme.boosterPositionWeights().size() == size,
          "one booster weight per level is required");
      BigDecimal previous = BigDecimal.ONE;
      for (BigDecimal threshold : theme.levelThresholds()) {
        decimal(
            threshold, "level threshold", new BigDecimal("1.0001"), crashModel.maxMultiplier(), 4);
        require(threshold.compareTo(previous) > 0, "level thresholds must strictly increase");
        previous = threshold;
      }
      BigDecimal sum = BigDecimal.ZERO;
      for (var weight : theme.boosterPositionWeights()) {
        decimal(weight, "booster weight", BigDecimal.ZERO, new BigDecimal("1000000"), 4);
        sum = sum.add(weight);
      }
      require(sum.signum() > 0, "booster weights must have positive sum");
    }
    require(betOptions.size() == 4, "exactly four bet options required");
    require(
        betOptions.stream().map(BetOption::id).distinct().count() == 4,
        "bet option IDs must be unique");
    require(
        betOptions.stream()
            .map(BetOption::boosterMultiplier)
            .collect(java.util.stream.Collectors.toSet())
            .equals(Set.of(1, 2, 3, 4)),
        "bets must offer x1, x2, x3, x4");
    for (var bet : betOptions) {
      require(
          bet.id() != null && bet.id().matches("[A-Z][A-Z0-9_]{0,31}"), "invalid bet option ID");
      decimal(bet.stake(), "stake", new BigDecimal("0.01"), new BigDecimal("1000000"), 2);
    }
    count(points.pointsPerLevel(), "points_per_level");
    count(points.pointsCashoutBonus(), "points_cashout_bonus");
    require(
        points.boosterActivationBonuses().keySet().equals(Set.of(1, 2, 3, 4)),
        "point bonuses need keys 1, 2, 3, 4");
    points.boosterActivationBonuses().values().forEach(v -> count(v, "booster bonus"));
    require(points.boosterActivationBonuses().get(1) == 0, "x1 cannot grant booster points");
    count(reward.fragmentsWin(), "fragments_win");
    count(reward.fragmentsLoss(), "fragments_loss");
    require(reward.fragmentsWin() > 0 || reward.fragmentsLoss() > 0, "reward must grant fragments");
    require(
        reward.fragmentsPerBonus() > 0 && reward.fragmentsPerBonus() <= 1000000,
        "fragments_per_bonus must be 1..1000000");
    decimal(
        reward.bonusAmount(), "bonus_amount", new BigDecimal("0.01"), new BigDecimal("1000000"), 2);
  }

  private static void count(int value, String name) {
    require(value >= 0 && value <= 1000000, name + " must be 0..1000000");
  }

  private static void decimal(
      BigDecimal value, String name, BigDecimal min, BigDecimal max, int scale) {
    require(
        value != null
            && value.compareTo(min) >= 0
            && value.compareTo(max) <= 0
            && value.stripTrailingZeros().scale() <= scale,
        name + " has invalid range or precision");
  }

  private static void require(boolean valid, String message) {
    if (!valid) throw new IllegalArgumentException(message);
  }
}
