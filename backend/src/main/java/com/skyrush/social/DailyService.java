package com.skyrush.social;

import com.skyrush.rounds.GameRound;
import com.skyrush.rounds.RoundState;
import com.skyrush.users.CurrentUserService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyService {
  public enum Kind {
    PLAY_ROUNDS,
    WIN_ROUNDS,
    EARN_POINTS,
    ACTIVATE_BOOSTER,
    ACTIVATE_X3_OR_HIGHER,
    REACH_LEVEL,
    CASHOUT_ABOVE
  }

  public record Daily(
      LocalDate date,
      String kind,
      String description,
      BigDecimal progress,
      BigDecimal target,
      boolean completed,
      int rewardFragments,
      UUID rewardRoundId) {}

  private final JdbcTemplate jdbc;
  private final CurrentUserService users;
  private final Clock clock;
  private final SocialSettings settings;

  public DailyService(
      JdbcTemplate jdbc, CurrentUserService users, Clock clock, SocialSettings settings) {
    this.jdbc = jdbc;
    this.users = users;
    this.clock = clock;
    this.settings = settings;
  }

  public static Kind select(LocalDate date) {
    return Kind.values()[Math.floorMod(date.toEpochDay(), Kind.values().length)];
  }

  private BigDecimal target(Kind kind) {
    return switch (kind) {
      case PLAY_ROUNDS -> BigDecimal.valueOf(settings.dailyRounds());
      case WIN_ROUNDS -> BigDecimal.valueOf(settings.dailyWins());
      case EARN_POINTS -> BigDecimal.valueOf(settings.dailyPoints());
      case REACH_LEVEL -> BigDecimal.valueOf(settings.dailyLevel());
      case CASHOUT_ABOVE -> settings.dailyCashout();
      default -> BigDecimal.ONE;
    };
  }

  private void define(LocalDate date) {
    var kind = select(date);
    jdbc.update(
        "INSERT INTO skyrush.daily_challenges VALUES(?,?,?) ON CONFLICT DO NOTHING",
        date,
        kind.name(),
        target(kind));
  }

  @Transactional
  public Daily current() {
    LocalDate day = clock.instant().atZone(ZoneOffset.UTC).toLocalDate();
    define(day);
    return view(users.id(), day);
  }

  private Daily view(UUID user, LocalDate day) {
    return jdbc.queryForObject(
        """
  SELECT d.*,COALESCE(p.progress,0) AS progress,COALESCE(p.completed,false) AS completed,
   COALESCE(p.reward_fragments,0) AS reward_fragments,p.reward_round_id
  FROM skyrush.daily_challenges d LEFT JOIN skyrush.daily_challenge_progress p ON p.challenge_date=d.challenge_date AND p.user_id=? WHERE d.challenge_date=?
  """,
        (r, n) ->
            new Daily(
                day,
                r.getString("kind"),
                description(Kind.valueOf(r.getString("kind")), r.getBigDecimal("target")),
                r.getBigDecimal("progress"),
                r.getBigDecimal("target"),
                r.getBoolean("completed"),
                r.getInt("reward_fragments"),
                r.getObject("reward_round_id", UUID.class)),
        user,
        day);
  }

  private String description(Kind kind, BigDecimal target) {
    String n = target.stripTrailingZeros().toPlainString().replace('.', ',');
    return switch (kind) {
      case PLAY_ROUNDS -> "Заверши полёты: " + n;
      case WIN_ROUNDS -> "Победы: " + n;
      case EARN_POINTS -> "Набери " + n + " очков";
      case ACTIVATE_BOOSTER -> "Поймай бустер";
      case ACTIVATE_X3_OR_HIGHER -> "Поймай бустер ×3 или ×4";
      case REACH_LEVEL -> "Достигни уровня " + n;
      case CASHOUT_ABOVE -> "Забери на ×" + n + " или выше";
    };
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void complete(GameRound r) {
    LocalDate day = r.completedAt.atZone(ZoneOffset.UTC).toLocalDate();
    define(day);
    jdbc.update(
        "INSERT INTO skyrush.daily_challenge_progress(user_id,challenge_date,progress) VALUES(?,?,0) ON CONFLICT DO NOTHING",
        r.userId,
        day);
    if (jdbc.update(
            "INSERT INTO skyrush.daily_round_contributions VALUES(?,?,?) ON CONFLICT DO NOTHING",
            r.id,
            r.userId,
            day)
        == 0) return;
    var d = view(r.userId, day);
    if (d.completed()) return;
    Kind kind = Kind.valueOf(d.kind());
    BigDecimal value =
        switch (kind) {
          case PLAY_ROUNDS -> BigDecimal.ONE;
          case WIN_ROUNDS -> r.state == RoundState.COMPLETED_WIN ? BigDecimal.ONE : BigDecimal.ZERO;
          case EARN_POINTS -> BigDecimal.valueOf(r.earnedPoints);
          case ACTIVATE_BOOSTER -> r.boosterActive ? BigDecimal.ONE : BigDecimal.ZERO;
          case ACTIVATE_X3_OR_HIGHER ->
              r.boosterActive && r.boosterMultiplier >= 3 ? BigDecimal.ONE : BigDecimal.ZERO;
          case REACH_LEVEL -> BigDecimal.valueOf(r.completedLevel);
          case CASHOUT_ABOVE -> r.cashoutMultiplier == null ? BigDecimal.ZERO : r.cashoutMultiplier;
        };
    BigDecimal progress =
        (kind == Kind.REACH_LEVEL || kind == Kind.CASHOUT_ABOVE
                ? d.progress().max(value)
                : d.progress().add(value))
            .min(d.target());
    boolean done = progress.compareTo(d.target()) >= 0;
    jdbc.update(
        "UPDATE skyrush.daily_challenge_progress SET progress=?,completed=?,reward_fragments=?,completed_at=?,reward_round_id=? WHERE user_id=? AND challenge_date=?",
        progress,
        done,
        done ? 1 : 0,
        done ? Timestamp.from(r.completedAt) : null,
        done ? r.id : null,
        r.userId,
        day);
    if (done)
      jdbc.update(
          "UPDATE skyrush.reward_progress SET fragments=fragments+1 WHERE user_id=?", r.userId);
  }
}
