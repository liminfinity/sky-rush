package com.skyrush;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ProfileMigrationTests extends PostgresSupport {
  @Test
  void legacyRewardsUnlockCollectionWithoutChangingPlayerData() {
    var root =
        new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl("postgres", "postgres")));
    root.execute("CREATE DATABASE profile_legacy_test");
    String url = POSTGRES.getJdbcUrl("postgres", "profile_legacy_test");
    var jdbc = new JdbcTemplate(new DriverManagerDataSource(url));
    Flyway.configure()
        .dataSource(url, "postgres", "")
        .defaultSchema("public")
        .target("4")
        .load()
        .migrate();
    UUID user = UUID.randomUUID(), round = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO skyrush.users(id,display_name,username,password_hash,created_at) VALUES(?, 'Legacy','legacy_player','*',now())",
        user);
    jdbc.update("INSERT INTO skyrush.wallets VALUES(?,777)", user);
    jdbc.update("INSERT INTO skyrush.reward_progress VALUES(?,1)", user);
    jdbc.update(
        """
            INSERT INTO skyrush.game_rounds(id,user_id,request_id,theme,bet_option_id,stake,booster_multiplier,
              config_version,config_snapshot,crash_base,started_at,crash_at,state,completed_at,earned_points,reward_fragments,reward_redeemed,reward_bonus)
            VALUES(?,?,?,'GREEN','BASIC',10,1,'legacy','{}',2,now()-interval '1 minute',now(),'COMPLETED_LOSS',now(),99,6,5,5)
            """,
        round,
        user,
        UUID.randomUUID());
    var tables = List.of("users", "wallets", "reward_progress", "game_rounds");
    var before = new HashMap<String, List<Map<String, Object>>>();
    tables.forEach(t -> before.put(t, jdbc.queryForList("SELECT * FROM skyrush." + t)));
    var migration =
        Flyway.configure()
            .dataSource(url, "postgres", "")
            .defaultSchema("public")
            .target("5")
            .load();
    assertThat(migration.migrate().migrationsExecuted).isEqualTo(1);
    migration.validate();
    tables.forEach(
        t -> assertThat(jdbc.queryForList("SELECT * FROM skyrush." + t)).isEqualTo(before.get(t)));
    assertThat(
            jdbc.queryForList(
                "SELECT cosmetic_id FROM skyrush.user_cosmetic_unlocks WHERE user_id=? ORDER BY cosmetic_id",
                String.class,
                user))
        .containsExactly("classic", "constellations", "plain");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.user_cosmetic_unlocks WHERE round_id IS NOT NULL",
                Integer.class))
        .isZero();
    assertThat(migration.migrate().migrationsExecuted).isZero();
  }
}
