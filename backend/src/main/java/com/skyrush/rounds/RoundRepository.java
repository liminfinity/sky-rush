package com.skyrush.rounds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyrush.gameconfig.GameConfiguration;
import com.skyrush.gameconfig.GameTheme;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoundRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public RoundRepository(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Optional<UUID> owner(UUID id) {
    return jdbc
        .query(
            "SELECT user_id FROM skyrush.game_rounds WHERE id=?",
            (r, n) -> r.getObject(1, UUID.class),
            id)
        .stream()
        .findFirst();
  }

  public java.util.List<UUID> unfinished() {
    return jdbc.query(
        "SELECT id FROM skyrush.game_rounds WHERE active_user_id IS NOT NULL",
        (r, n) -> r.getObject(1, UUID.class));
  }

  public Optional<GameRound> find(UUID userId, UUID id) {
    return jdbc
        .query(
            "SELECT * FROM skyrush.game_rounds WHERE user_id = ? AND id = ? FOR UPDATE",
            this::map,
            userId,
            id)
        .stream()
        .findFirst();
  }

  public Optional<GameRound> request(UUID userId, UUID requestId) {
    return jdbc
        .query(
            "SELECT * FROM skyrush.game_rounds WHERE user_id = ? AND request_id = ?",
            this::map,
            userId,
            requestId)
        .stream()
        .findFirst();
  }

  public Optional<GameRound> active(UUID userId) {
    return jdbc
        .query(
            "SELECT * FROM skyrush.game_rounds WHERE active_user_id = ? FOR UPDATE",
            this::map,
            userId)
        .stream()
        .findFirst();
  }

  public List<GameRound> history(UUID userId, int limit, int offset) {
    return jdbc.query(
        "SELECT * FROM skyrush.game_rounds WHERE user_id = ? AND completed_at IS NOT NULL ORDER BY completed_at DESC, id DESC LIMIT ? OFFSET ?",
        this::map,
        userId,
        limit,
        offset);
  }

  public List<UUID> due(Instant now) {
    return jdbc.query(
        "SELECT id FROM skyrush.game_rounds WHERE state IN ('ACTIVE', 'CASHED_OUT') AND crash_at <= ? ORDER BY crash_at LIMIT 100",
        (rs, i) -> rs.getObject(1, UUID.class),
        ts(now));
  }

  public void insert(GameRound r) {
    jdbc.update(
        """
                INSERT INTO skyrush.game_rounds(id,user_id,request_id,active_user_id,theme,bet_option_id,stake,
                    booster_multiplier,booster_level,config_version,config_snapshot,crash_base,started_at,crash_at,state,proof_salt,proof_hash)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
        r.id,
        r.userId,
        r.requestId,
        r.userId,
        r.theme.name(),
        r.betOptionId,
        r.stake,
        r.boosterMultiplier,
        r.boosterLevel,
        r.configVersion,
        encode(r.configuration),
        r.crashBase,
        ts(r.startedAt),
        ts(r.crashAt),
        r.state.name(),
        r.proofSalt,
        r.proofHash);
  }

  public void save(GameRound r) {
    jdbc.update(
        """
                UPDATE skyrush.game_rounds SET active_user_id=?,state=?,cashout_at=?,cashout_multiplier=?,payout=?,
                    completed_at=?,completed_level=?,booster_active=?,earned_points=?,reward_fragments=?,reward_redeemed=?,reward_bonus=?
                WHERE id=?
                """,
        r.state.completed() ? null : r.userId,
        r.state.name(),
        ts(r.cashoutAt),
        r.cashoutMultiplier,
        r.payout,
        ts(r.completedAt),
        r.completedLevel,
        r.boosterActive,
        r.earnedPoints,
        r.rewardFragments,
        r.rewardRedeemed,
        r.rewardBonus,
        r.id);
  }

  private GameRound map(ResultSet rs, int row) throws SQLException {
    GameRound r = new GameRound();
    r.proofSalt = rs.getString("proof_salt");
    r.proofHash = rs.getString("proof_hash");
    r.id = rs.getObject("id", UUID.class);
    r.userId = rs.getObject("user_id", UUID.class);
    r.requestId = rs.getObject("request_id", UUID.class);
    r.theme = GameTheme.valueOf(rs.getString("theme"));
    r.betOptionId = rs.getString("bet_option_id");
    r.stake = rs.getBigDecimal("stake");
    r.boosterMultiplier = rs.getInt("booster_multiplier");
    r.boosterLevel = (Integer) rs.getObject("booster_level");
    r.configVersion = rs.getString("config_version");
    try {
      r.configuration = json.readValue(rs.getString("config_snapshot"), GameConfiguration.class);
    } catch (JsonProcessingException ex) {
      throw new SQLException("Invalid stored configuration snapshot", ex);
    }
    r.crashBase = rs.getBigDecimal("crash_base");
    r.startedAt = instant(rs, "started_at");
    r.crashAt = instant(rs, "crash_at");
    r.state = RoundState.valueOf(rs.getString("state"));
    r.cashoutAt = instant(rs, "cashout_at");
    r.cashoutMultiplier = rs.getBigDecimal("cashout_multiplier");
    r.payout = rs.getBigDecimal("payout");
    r.completedAt = instant(rs, "completed_at");
    r.completedLevel = rs.getInt("completed_level");
    r.boosterActive = rs.getBoolean("booster_active");
    r.earnedPoints = rs.getLong("earned_points");
    r.rewardFragments = rs.getInt("reward_fragments");
    r.rewardRedeemed = rs.getLong("reward_redeemed");
    r.rewardBonus = rs.getBigDecimal("reward_bonus");
    return r;
  }

  private String encode(GameConfiguration config) {
    try {
      return json.writeValueAsString(config);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static Timestamp ts(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private static Instant instant(ResultSet rs, String name) throws SQLException {
    Timestamp value = rs.getTimestamp(name);
    return value == null ? null : value.toInstant();
  }
}
