package com.skyrush.profile;

import com.skyrush.shared.GameException;
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
@Transactional
public class CollectionService {
  public record Item(
      String id,
      String kind,
      String name,
      long fragmentsRequired,
      boolean unlocked,
      boolean equipped,
      Instant unlockedAt,
      UUID unlockedRoundId) {}

  public record Collection(
      long lifetimeFragments,
      long nextThreshold,
      long fragmentsToNext,
      String nextItemName,
      String balloonSkin,
      String profileFrame,
      List<Item> items) {
    public Collection {
      items = List.copyOf(items);
    }
  }

  private final JdbcTemplate jdbc;
  private final CurrentUserService users;
  private final Clock clock;

  public CollectionService(JdbcTemplate jdbc, CurrentUserService users, Clock clock) {
    this.jdbc = jdbc;
    this.users = users;
    this.clock = clock;
  }

  long earned(UUID user) {
    return java.util.Objects.requireNonNull(
        jdbc.queryForObject(
            "SELECT (SELECT COALESCE(SUM(reward_fragments),0) FROM skyrush.game_rounds WHERE user_id=? AND completed_at IS NOT NULL)+(SELECT COALESCE(SUM(reward_fragments),0) FROM skyrush.daily_challenge_progress WHERE user_id=?)",
            Long.class,
            user,
            user),
        "Required database value is null");
  }

  /** Called after the completed round has been saved, under the existing user lock/transaction. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void unlockForRound(UUID user, UUID round, Instant now) {
    sync(user, round, now);
  }

  private void sync(UUID user, UUID round, Instant now) {
    jdbc.update(
        """
            INSERT INTO skyrush.user_cosmetic_unlocks(user_id,cosmetic_id,unlocked_at,round_id)
            SELECT ?,id,?,CASE WHEN fragments_required=0 THEN NULL ELSE ?::uuid END
            FROM skyrush.cosmetic_items WHERE fragments_required<=?
            ON CONFLICT(user_id,cosmetic_id) DO NOTHING
            """,
        user,
        Timestamp.from(now),
        round,
        earned(user));
  }

  public Collection current() {
    UUID user = users.lock().id();
    sync(user, null, clock.instant());
    return view(user);
  }

  public Collection equip(String cosmeticId) {
    UUID user = users.lock().id();
    sync(user, null, clock.instant());
    var kinds =
        jdbc.queryForList(
            "SELECT kind FROM skyrush.cosmetic_items WHERE id=?", String.class, cosmeticId);
    if (kinds.isEmpty()) throw new GameException(404, "COSMETIC_NOT_FOUND", "Cosmetic not found");
    if (java.util.Objects.requireNonNull(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.user_cosmetic_unlocks WHERE user_id=? AND cosmetic_id=?",
                Long.class,
                user,
                cosmeticId),
            "Required database value is null")
        == 0) throw new GameException(409, "COSMETIC_LOCKED", "Cosmetic is locked");
    jdbc.update(
        """
            INSERT INTO skyrush.user_equipped_cosmetics(user_id,kind,cosmetic_id) VALUES(?,?,?)
            ON CONFLICT(user_id,kind) DO UPDATE SET cosmetic_id=EXCLUDED.cosmetic_id
            """,
        user,
        kinds.getFirst(),
        cosmeticId);
    return view(user);
  }

  private Collection view(UUID user) {
    var items =
        jdbc.query(
            """
            SELECT c.*,u.unlocked_at,u.round_id,
                CASE WHEN e.cosmetic_id IS NULL THEN c.fragments_required=0 ELSE e.cosmetic_id=c.id END AS equipped
            FROM skyrush.cosmetic_items c
            LEFT JOIN skyrush.user_cosmetic_unlocks u ON u.cosmetic_id=c.id AND u.user_id=?
            LEFT JOIN skyrush.user_equipped_cosmetics e ON e.kind=c.kind AND e.user_id=?
            ORDER BY c.fragments_required,c.id
            """,
            (r, n) ->
                new Item(
                    r.getString("id"),
                    r.getString("kind"),
                    r.getString("name"),
                    r.getLong("fragments_required"),
                    r.getTimestamp("unlocked_at") != null,
                    r.getBoolean("equipped"),
                    r.getTimestamp("unlocked_at") == null
                        ? null
                        : r.getTimestamp("unlocked_at").toInstant(),
                    r.getObject("round_id", UUID.class)),
            user,
            user);
    long earned = earned(user);
    Item next = items.stream().filter(i -> !i.unlocked()).findFirst().orElse(null);
    String skin =
        items.stream()
            .filter(i -> i.equipped() && i.kind().equals("BALLOON"))
            .findFirst()
            .orElseThrow()
            .id();
    String frame =
        items.stream()
            .filter(i -> i.equipped() && i.kind().equals("FRAME"))
            .findFirst()
            .orElseThrow()
            .id();
    return new Collection(
        earned,
        next == null ? earned : next.fragmentsRequired(),
        next == null ? 0 : next.fragmentsRequired() - earned,
        next == null ? null : next.name(),
        skin,
        frame,
        items);
  }
}
