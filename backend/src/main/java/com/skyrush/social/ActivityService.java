package com.skyrush.social;

import com.skyrush.users.CurrentUserService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
  public record Event(UUID id, String displayName, String kind, String value, Instant createdAt) {}

  public record View(long online, int presenceWindowSeconds, List<Event> events) {
    public View {
      events = List.copyOf(events);
    }
  }

  private final JdbcTemplate jdbc;
  private final CurrentUserService users;
  private final Clock clock;
  private final SocialSettings settings;

  public ActivityService(
      JdbcTemplate jdbc, CurrentUserService users, Clock clock, SocialSettings settings) {
    this.jdbc = jdbc;
    this.users = users;
    this.clock = clock;
    this.settings = settings;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void emit(UUID user, UUID round, String kind, String value, Instant at) {
    jdbc.update(
        "INSERT INTO skyrush.player_activity_events VALUES(?,?,?,?,?,?,?) ON CONFLICT(event_key) DO NOTHING",
        UUID.randomUUID(),
        user,
        round,
        round + ":" + kind,
        kind,
        value,
        Timestamp.from(at));
  }

  @Transactional
  public View heartbeat() {
    UUID user = users.id();
    Instant now = clock.instant();
    jdbc.update(
        """
   INSERT INTO skyrush.user_presence VALUES(?,?) ON CONFLICT(user_id) DO UPDATE SET last_seen_at=EXCLUDED.last_seen_at
   WHERE skyrush.user_presence.last_seen_at<=EXCLUDED.last_seen_at-interval '20 seconds'
   """,
        user,
        Timestamp.from(now));
    return current();
  }

  public View current() {
    Instant now = clock.instant();
    long count =
        java.util.Objects.requireNonNull(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.user_presence p JOIN skyrush.users u ON u.id=p.user_id WHERE p.last_seen_at>=? AND u.password_hash<>'*'",
                Long.class,
                Timestamp.from(now.minusSeconds(settings.presenceSeconds()))),
            "Required database value is null");
    var events =
        jdbc.query(
            """
   SELECT e.id,u.display_name,e.kind,e.value,e.created_at FROM skyrush.player_activity_events e
   JOIN skyrush.users u ON u.id=e.user_id WHERE e.created_at>=? AND u.password_hash<>'*'
   ORDER BY e.created_at DESC,e.id DESC LIMIT 5
   """,
            (r, n) ->
                new Event(
                    r.getObject("id", UUID.class),
                    r.getString("display_name"),
                    r.getString("kind"),
                    r.getString("value"),
                    r.getTimestamp("created_at").toInstant()),
            Timestamp.from(now.minusSeconds(3600)));
    return new View(count, settings.presenceSeconds(), events);
  }
}
