package com.skyrush.rounds;

import com.skyrush.gameconfig.GameConfiguration;
import com.skyrush.gameconfig.GameTheme;
import com.skyrush.rewards.Reward;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Explicit allowlist: no crash deadline, base crash point, snapshot, or random state. */
public record RoundView(
    UUID id,
    GameTheme theme,
    String betOptionId,
    BigDecimal stake,
    RoundState state,
    Booster booster,
    BigDecimal currentMultiplier,
    int completedLevel,
    List<BigDecimal> levelThresholds,
    long earnedPoints,
    boolean canCashout,
    BigDecimal payout,
    BigDecimal cashoutMultiplier,
    BigDecimal crashMultiplier,
    Reward reward,
    Instant startedAt,
    Instant cashoutAt,
    Instant completedAt,
    Instant serverTime,
    String configVersion,
    GameConfiguration.Points pointsRules,
    OutcomeProof.View integrity,
    com.skyrush.competition.CompetitionService.Ranking ranking) {
  public RoundView {
    levelThresholds = List.copyOf(levelThresholds);
  }
}
