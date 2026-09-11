package com.skyrush.competition;

import com.skyrush.users.CurrentUserService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompetitionService {
  public record Entry(String id, String name, long points, boolean currentPlayer, int position) {}

  public record Ranking(List<Entry> entries) {
    public Ranking {
      entries = List.copyOf(entries);
    }
  }

  public record Tournament(
      String name,
      String status,
      Instant endsAt,
      long remainingSeconds,
      List<Entry> entries,
      Entry currentPlayer) {
    public Tournament {
      entries = List.copyOf(entries);
    }
  }

  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final CurrentUserService users;

  public CompetitionService(JdbcTemplate jdbc, Clock clock, CurrentUserService users) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.users = users;
  }

  private Instant start() {
    return clock
        .instant()
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant();
  }

  private List<Entry> entries(UUID current, boolean masked) {
    Instant start = start();
    return jdbc.query(
        """
            SELECT u.id,u.display_name,COALESCE(s.points,0) AS points
            FROM skyrush.users u LEFT JOIN (
                SELECT user_id,SUM(earned_points) AS points FROM skyrush.game_rounds
                WHERE started_at>=? AND started_at<? GROUP BY user_id
            ) s ON s.user_id=u.id
            WHERE u.password_hash<>'*'
            ORDER BY points DESC,u.created_at,u.id
            """,
        (r, n) -> {
          UUID id = r.getObject("id", UUID.class);
          boolean own = id.equals(current);
          String name = r.getString("display_name");
          if (masked && !own)
            name =
                name.substring(
                        0,
                        name.offsetByCodePoints(
                            0, Math.min(2, name.codePointCount(0, name.length()))))
                    + "***";
          return new Entry(id.toString(), name, r.getLong("points"), own, n + 1);
        },
        Timestamp.from(start),
        Timestamp.from(start.plusSeconds(86400)));
  }

  /**
   * Same transaction sees this round's freshly persisted points. Nearby real players retain global
   * ranks.
   */
  public Ranking live(UUID current) {
    var all = entries(current, false);
    int index = 0;
    for (int i = 0; i < all.size(); i++) if (all.get(i).currentPlayer()) index = i;
    int from = Math.max(0, index - 2);
    return new Ranking(List.copyOf(all.subList(from, Math.min(all.size(), from + 5))));
  }

  public Tournament tournament(boolean masked) {
    var all = entries(users.id(), masked);
    Instant end = start().plusSeconds(86400);
    return new Tournament(
        "Небесный день",
        "ACTIVE",
        end,
        Math.max(0, Duration.between(clock.instant(), end).toSeconds()),
        all,
        all.stream().filter(Entry::currentPlayer).findFirst().orElseThrow());
  }
}
