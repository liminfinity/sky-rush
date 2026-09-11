package com.skyrush.rewards;

import com.skyrush.gameconfig.GameConfiguration.RewardRules;
import com.skyrush.gamemath.GameMath;
import com.skyrush.wallet.WalletService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class RewardService {
  private final JdbcTemplate jdbc;
  private final GameMath math;
  private final WalletService wallets;

  public RewardService(JdbcTemplate jdbc, GameMath math, WalletService wallets) {
    this.jdbc = jdbc;
    this.math = math;
    this.wallets = wallets;
  }

  public long fragments(UUID userId) {
    return java.util.Objects.requireNonNull(
        jdbc.queryForObject(
            "SELECT fragments FROM skyrush.reward_progress WHERE user_id = ?", Long.class, userId),
        "Required database value is null");
  }

  public Reward grant(UUID userId, UUID roundId, boolean win, RewardRules rules, Instant now) {
    var grant = math.rewards.calculate(win, fragments(userId), rules);
    jdbc.update(
        "UPDATE skyrush.reward_progress SET fragments = ? WHERE user_id = ?",
        grant.fragmentsRemaining(),
        userId);
    if (grant.bonus().signum() > 0)
      wallets.apply(userId, roundId, "FRAGMENT_BONUS", grant.bonus(), now);
    return new Reward(grant.fragmentsGranted(), grant.fragmentsRedeemed(), grant.bonus());
  }
}
