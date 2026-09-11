package com.skyrush.rounds;

import com.skyrush.gameconfig.GameConfiguration;
import com.skyrush.gameconfig.GameTheme;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Internal persistence model, never serialized by a controller. No game calculations here. */
public class GameRound {
  public String proofSalt;
  public String proofHash;
  public UUID id;
  public UUID userId;
  public UUID requestId;
  public GameTheme theme;
  public String betOptionId;
  public BigDecimal stake;
  public int boosterMultiplier;
  public Integer boosterLevel;
  public String configVersion;
  public GameConfiguration configuration;
  public BigDecimal crashBase;
  public Instant startedAt;
  public Instant crashAt;
  public RoundState state = RoundState.ACTIVE;
  public Instant cashoutAt;
  public BigDecimal cashoutMultiplier;
  public BigDecimal payout = new BigDecimal("0.00");
  public Instant completedAt;
  public int completedLevel;
  public boolean boosterActive;
  public long earnedPoints;
  public int rewardFragments;
  public long rewardRedeemed;
  public BigDecimal rewardBonus = new BigDecimal("0.00");
}
