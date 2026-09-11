package com.skyrush.profile;

import com.skyrush.auth.AccountService;
import com.skyrush.competition.CompetitionService;
import com.skyrush.gamemath.GameMath;
import com.skyrush.users.CurrentUserService;
import com.skyrush.wallet.WalletService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {
  public record Records(
      long totalRounds,
      long successfulCashouts,
      long losses,
      BigDecimal winRate,
      BigDecimal highestCashoutMultiplier,
      BigDecimal highestCrashMultiplier,
      BigDecimal biggestPayout,
      int highestLevel,
      int strongestBooster,
      long totalPoints,
      long totalFragments,
      long currentWinStreak,
      long bestWinStreak) {}

  public record Profile(
      AccountService.Account account,
      BigDecimal bonusBalance,
      long fragmentBalance,
      CompetitionService.Entry tournament,
      Records records,
      CollectionService.Collection collection) {}

  private final CurrentUserService users;
  private final AccountService accounts;
  private final WalletService wallets;
  private final CompetitionService competition;
  private final CollectionService collection;
  private final JdbcTemplate jdbc;
  private final GameMath math;

  public ProfileService(
      CurrentUserService users,
      AccountService accounts,
      WalletService wallets,
      CompetitionService competition,
      CollectionService collection,
      JdbcTemplate jdbc,
      GameMath math) {
    this.users = users;
    this.accounts = accounts;
    this.wallets = wallets;
    this.competition = competition;
    this.collection = collection;
    this.jdbc = jdbc;
    this.math = math;
  }

  public Profile current() {
    UUID user = users.lock().id();
    return new Profile(
        accounts.me(),
        wallets.get(user).bonusBalance(),
        java.util.Objects.requireNonNull(
            jdbc.queryForObject(
                "SELECT fragments FROM skyrush.reward_progress WHERE user_id=?", Long.class, user),
            "Required database value is null"),
        competition.tournament(false).currentPlayer(),
        records(user),
        collection.current());
  }

  private Records records(UUID user) {
    // Compact history projection; no hidden outcome or unfinished round enters lifetime records.
    var rows =
        jdbc.queryForList(
            """
            SELECT state,cashout_multiplier,crash_base,booster_multiplier,booster_active,payout,
                   completed_level,earned_points,reward_fragments
            FROM skyrush.game_rounds WHERE user_id=? AND completed_at IS NOT NULL
            ORDER BY completed_at,id
            """,
            user);
    long wins = 0, points = 0, streak = 0, best = 0;
    int level = 0, booster = 0;
    BigDecimal cashout = null, crash = null, payout = BigDecimal.ZERO;
    for (var r : rows) {
      boolean win = "COMPLETED_WIN".equals(r.get("state"));
      if (win) {
        wins++;
        streak++;
        best = Math.max(best, streak);
      } else streak = 0;
      if (r.get("cashout_multiplier") != null) {
        var v = (BigDecimal) r.get("cashout_multiplier");
        cashout = cashout == null ? v : cashout.max(v);
      }
      // Reuse the existing game-math output convention, including a previously activated booster.
      var v =
          math.multipliers.effective(
              (BigDecimal) r.get("crash_base"),
              (Integer) r.get("booster_multiplier"),
              (Boolean) r.get("booster_active"));
      crash = crash == null ? v : crash.max(v);
      payout = payout.max((BigDecimal) r.get("payout"));
      level = Math.max(level, (Integer) r.get("completed_level"));
      if ((Boolean) r.get("booster_active"))
        booster = Math.max(booster, (Integer) r.get("booster_multiplier"));
      points += ((Number) r.get("earned_points")).longValue();
    }
    var rate =
        rows.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(wins * 100)
                .divide(BigDecimal.valueOf(rows.size()), 1, RoundingMode.HALF_UP);
    return new Records(
        rows.size(),
        wins,
        rows.size() - wins,
        rate,
        cashout,
        crash,
        payout,
        level,
        booster,
        points,
        collection.earned(user),
        streak,
        best);
  }
}
