package com.skyrush.social;

import com.skyrush.rounds.GameRound;
import com.skyrush.users.CurrentUserService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementService {
  public record Achievement(
      String id, String name, String description, Instant unlockedAt, UUID roundId) {}

  private final JdbcTemplate jdbc;
  private final CurrentUserService users;
  private final SocialSettings settings;
  private final ActivityService activity;

  public AchievementService(
      JdbcTemplate jdbc,
      CurrentUserService users,
      SocialSettings settings,
      ActivityService activity) {
    this.jdbc = jdbc;
    this.users = users;
    this.settings = settings;
    this.activity = activity;
  }

  private List<Achievement> definitions() {
    return List.of(
        item("FIRST_FLIGHT", "Первый полёт", "Заверши полёт"),
        item("FIRST_WIN", "Есть!", "Забери выигрыш"),
        item("RISK_TAKER", "Рискнул", "Забери на ×3 или выше"),
        item("HIGH_FLYER", "Выше облаков", "Достигни уровня " + settings.highLevel()),
        item("BOOSTER_HUNTER", "Поймал бустер", "Активируй бустер"),
        item("BOOSTER_X4", "На максимум", "Активируй ×4"),
        item("WIN_STREAK_3", "Серия", "3 победы подряд"),
        item("WIN_STREAK_5", "Хладнокровие", "5 побед подряд"),
        item(
            "BIG_WIN",
            "Крупный выигрыш",
            "Выиграй от " + settings.bigWin().stripTrailingZeros().toPlainString() + " бонусов"),
        item("VETERAN_25", "Налетал", "Заверши 25 полётов"),
        item("VETERAN_100", "Сто полётов", "Заверши 100 полётов"));
  }

  private Achievement item(String id, String name, String description) {
    return new Achievement(id, name, description, null, null);
  }

  public List<Achievement> current() {
    var owned =
        jdbc.queryForList(
            "SELECT achievement_id,unlocked_at,round_id FROM skyrush.user_achievements WHERE user_id=?",
            users.id());
    return definitions().stream()
        .map(
            d ->
                owned.stream()
                    .filter(o -> d.id().equals(o.get("achievement_id")))
                    .findFirst()
                    .map(
                        o ->
                            new Achievement(
                                d.id(),
                                d.name(),
                                d.description(),
                                ((Timestamp) o.get("unlocked_at")).toInstant(),
                                (UUID) o.get("round_id")))
                    .orElse(d))
        .toList();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void complete(GameRound round) {
    var rows =
        jdbc.queryForList(
            "SELECT state,cashout_multiplier,completed_level,booster_active,booster_multiplier,payout FROM skyrush.game_rounds WHERE user_id=? AND completed_at IS NOT NULL ORDER BY completed_at,id",
            round.userId);
    long wins = 0, streak = 0, best = 0;
    int level = 0, booster = 0;
    BigDecimal cashout = BigDecimal.ZERO, payout = BigDecimal.ZERO;
    for (var r : rows) {
      if ("COMPLETED_WIN".equals(r.get("state"))) {
        wins++;
        best = Math.max(best, ++streak);
      } else streak = 0;
      if (r.get("cashout_multiplier") != null)
        cashout = cashout.max((BigDecimal) r.get("cashout_multiplier"));
      payout = payout.max((BigDecimal) r.get("payout"));
      level = Math.max(level, (Integer) r.get("completed_level"));
      if ((Boolean) r.get("booster_active"))
        booster = Math.max(booster, (Integer) r.get("booster_multiplier"));
    }
    boolean[] met = {
      rows.size() > 0,
      wins > 0,
      cashout.compareTo(new BigDecimal("3")) >= 0,
      level >= settings.highLevel(),
      booster > 1,
      booster == 4,
      best >= 3,
      best >= 5,
      payout.compareTo(settings.bigWin()) >= 0,
      rows.size() >= 25,
      rows.size() >= 100
    };
    Achievement highlighted = null;
    var defs = definitions();
    for (int i = 0; i < defs.size(); i++)
      if (met[i]) {
        var d = defs.get(i);
        int inserted =
            jdbc.update(
                "INSERT INTO skyrush.user_achievements VALUES(?,?,?,?) ON CONFLICT DO NOTHING",
                round.userId,
                d.id(),
                Timestamp.from(round.completedAt),
                round.id);
        if (inserted > 0) highlighted = d;
      }
    // At most one achievement feed item per completed round, even when several milestones unlock.
    if (highlighted != null)
      activity.emit(round.userId, round.id, "ACHIEVEMENT", highlighted.name(), round.completedAt);
  }
}
