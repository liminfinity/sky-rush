package com.skyrush.rounds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyrush.PostgresSupport;
import com.skyrush.social.DailyService;
import com.skyrush.social.SocialSettings;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GameplayIntegrationTests.Controls.class)
class SocialIntegrationTests extends PostgresSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired GameplayIntegrationTests.MutableClock clock;
  @Autowired GameplayIntegrationTests.ScriptedRandom random;
  long time;

  @BeforeEach
  void reset() {
    time = 0;
    clock.at(0);
    random.set("0.9", "0");
    jdbc.update("DELETE FROM skyrush.user_presence");
    jdbc.update("DELETE FROM skyrush.player_activity_events");
  }

  MockHttpSession register(String name) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/register")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "username",
                                "p_" + UUID.randomUUID().toString().replace("-", ""),
                                "displayName",
                                name,
                                "password",
                                "password123"))))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  JsonNode getJson(MockHttpSession p, String path) throws Exception {
    return json.readTree(
        mvc.perform(get(path).session(p))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode heartbeat(MockHttpSession p) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/presence/heartbeat")
                    .session(p)
                    .with(csrf())
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  String start(MockHttpSession p) throws Exception {
    random.set("0.9", "0");
    return json.readTree(
            mvc.perform(
                    post("/api/rounds")
                        .session(p)
                        .with(csrf())
                        .contentType("application/json")
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "theme",
                                    "GREEN",
                                    "betOptionId",
                                    "QUAD",
                                    "requestId",
                                    UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("id")
        .asText();
  }

  JsonNode finish(MockHttpSession p) throws Exception {
    String id = start(p);
    clock.at(time + 1000);
    mvc.perform(post("/api/rounds/" + id + "/cashout").session(p).with(csrf()))
        .andExpect(status().isOk());
    time += 60000;
    clock.at(time);
    return getJson(p, "/api/rounds/" + id + "/state");
  }

  Set<String> owned(MockHttpSession p) throws Exception {
    Set<String> ids = new HashSet<>();
    for (var a : getJson(p, "/api/achievements"))
      if (!a.path("unlockedAt").isNull()) ids.add(a.path("id").asText());
    return ids;
  }

  @Test
  void completedGameplayUnlocksMilestonesOnceAndKeepsBobSeparate() throws Exception {
    var a = register("Alice");
    var b = register("Bob");
    var r = finish(a);
    assertThat(owned(a))
        .contains(
            "FIRST_FLIGHT",
            "FIRST_WIN",
            "RISK_TAKER",
            "BOOSTER_HUNTER",
            "BOOSTER_X4",
            "HIGH_FLYER",
            "BIG_WIN");
    assertThat(owned(b)).isEmpty();
    var before = getJson(a, "/api/achievements");
    for (int i = 0; i < 4; i++) getJson(a, "/api/rounds/" + r.path("id").asText() + "/state");
    assertThat(getJson(a, "/api/achievements")).isEqualTo(before);
    finish(a);
    finish(a);
    assertThat(owned(a)).contains("WIN_STREAK_3").doesNotContain("WIN_STREAK_5");
    finish(a);
    finish(a);
    assertThat(owned(a)).contains("WIN_STREAK_5");
  }

  @Test
  void dailySelectionIsDeterministicAndCoversAllSevenTemplates() {
    Set<DailyService.Kind> kinds = new HashSet<>();
    for (int i = 0; i < 7; i++) {
      var date = LocalDate.of(2026, 9, 11).plusDays(i);
      assertThat(DailyService.select(date)).isEqualTo(DailyService.select(date));
      kinds.add(DailyService.select(date));
    }
    assertThat(kinds).hasSize(7);
  }

  @Test
  void dailyProgressRewardsOneFragmentAndSurvivesRetriesAndRollover() throws Exception {
    var a = register("Alice");
    var b = register("Bob");
    assertThat(getJson(a, "/api/daily-challenge").path("kind").asText()).isEqualTo("WIN_ROUNDS");
    finish(a);
    assertThat(getJson(a, "/api/daily-challenge").path("progress").asInt()).isEqualTo(1);
    assertThat(getJson(b, "/api/daily-challenge").path("progress").asInt()).isZero();
    var r = finish(a);
    var d = getJson(a, "/api/daily-challenge");
    assertThat(d.path("completed").asBoolean()).isTrue();
    assertThat(d.path("rewardFragments").asInt()).isEqualTo(1);
    assertThat(getJson(a, "/api/profile/collection").path("lifetimeFragments").asInt())
        .isEqualTo(5);
    assertThat(getJson(a, "/api/wallet").path("skyFragments").asInt()).isEqualTo(5);
    for (int i = 0; i < 5; i++) {
      getJson(a, "/api/rounds/" + r.path("id").asText() + "/state");
      assertThat(getJson(a, "/api/daily-challenge")).isEqualTo(d);
    }
    assertThat(getJson(a, "/api/wallet").path("skyFragments").asInt()).isEqualTo(5);
    // Next round's existing conversion consumes the daily fragment too, without another daily
    // reward.
    finish(a);
    assertThat(getJson(a, "/api/wallet").path("skyFragments").asInt()).isEqualTo(2);
    clock.at(12 * 60 * 60 * 1000);
    var tomorrow = getJson(a, "/api/daily-challenge");
    assertThat(tomorrow.path("date").asText()).isEqualTo("2026-09-12");
    assertThat(tomorrow.path("progress").asInt()).isZero();
    assertThat(tomorrow.path("rewardFragments").asInt()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.daily_challenge_progress WHERE user_id=? AND reward_fragments=1",
                Integer.class,
                UUID.fromString(getJson(a, "/api/auth/me").path("id").asText())))
        .isEqualTo(1);
  }

  @Test
  void everyDailyTemplateUsesPersistedRoundFields() throws Exception {
    for (int day = 0; day < 7; day++) {
      var p = register("Template");
      time = day * 86400000L;
      clock.at(time);
      var initial = getJson(p, "/api/daily-challenge");
      for (int n = 0;
          n < 3 && !getJson(p, "/api/daily-challenge").path("completed").asBoolean();
          n++) finish(p);
      var done = getJson(p, "/api/daily-challenge");
      assertThat(done.path("kind")).isEqualTo(initial.path("kind"));
      assertThat(done.path("completed").asBoolean()).isTrue();
      assertThat(done.path("rewardFragments").asInt()).isEqualTo(1);
    }
  }

  @Test
  void presenceCountsRealUniqueRecentAccountsAndThrottlesWrites() throws Exception {
    var a = register("Alice");
    var b = register("Bob");
    assertThat(getJson(a, "/api/activity").path("online").asInt()).isZero();
    assertThat(heartbeat(a).path("online").asInt()).isEqualTo(1);
    assertThat(heartbeat(a).path("online").asInt()).isEqualTo(1);
    clock.at(10000);
    heartbeat(a);
    assertThat(
            jdbc.queryForObject(
                    "SELECT min(last_seen_at) FROM skyrush.user_presence", java.sql.Timestamp.class)
                .toInstant())
        .isEqualTo(GameplayIntegrationTests.START);
    heartbeat(b);
    clock.at(91000);
    assertThat(getJson(b, "/api/activity").path("online").asInt()).isEqualTo(1);
    assertThat(heartbeat(a).path("online").asInt()).isEqualTo(2);
  }

  @Test
  void feedComesFromRealActionsAndHasNoSecurityFields() throws Exception {
    var a = register("Alice");
    var b = register("Bob");
    assertThat(getJson(b, "/api/activity").path("events")).isEmpty();
    String id = start(a);
    clock.at(1000);
    mvc.perform(post("/api/rounds/" + id + "/cashout").session(a).with(csrf()))
        .andExpect(status().isOk());
    var events = getJson(b, "/api/activity").path("events");
    assertThat(events).hasSize(2);
    assertThat(events)
        .allMatch(
            e ->
                e.path("displayName").asText().equals("Alice")
                    && !e.has("userId")
                    && !e.has("username"));
    assertThat(events)
        .anyMatch(
            e ->
                e.path("kind").asText().equals("CASHOUT")
                    && e.path("value").asText().equals("5.0000"));
    assertThat(events).anyMatch(e -> e.path("kind").asText().equals("BOOSTER"));
    clock.at(60000);
    getJson(a, "/api/rounds/" + id + "/state");
    var feed = getJson(b, "/api/activity").path("events");
    assertThat(feed).anyMatch(e -> e.path("kind").asText().equals("ACHIEVEMENT"));
    var dates = new ArrayList<String>();
    feed.forEach(e -> dates.add(e.path("createdAt").asText()));
    assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder());
    clock.at(3700000);
    assertThat(getJson(b, "/api/activity").path("events")).isEmpty();
  }

  @Test
  void clientCannotForgeSocialStateOrAnotherPresence() throws Exception {
    var p = register("Alice");
    for (String endpoint : List.of("/api/activity", "/api/achievements", "/api/daily-challenge")) {
      mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
      mvc.perform(
              post(endpoint)
                  .session(p)
                  .with(csrf())
                  .contentType("application/json")
                  .content("{\"userId\":\"bob\",\"progress\":999,\"achievementId\":\"FIRST_WIN\"}"))
          .andExpect(status().isMethodNotAllowed());
    }
    mvc.perform(
            post("/api/presence/heartbeat")
                .session(p)
                .with(csrf())
                .contentType("application/json")
                .content("{\"userId\":\"bob\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/presence/heartbeat")
                .session(p)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    assertThat(getJson(p, "/api/activity").path("online").asInt()).isZero();
  }

  @Test
  void concurrentCompletionAwardsDailyFragmentOnlyOnce() throws Exception {
    var a = register("Concurrent");
    finish(a);
    String id = start(a);
    clock.at(time + 1000);
    mvc.perform(post("/api/rounds/" + id + "/cashout").session(a).with(csrf()))
        .andExpect(status().isOk());
    clock.at(time + 60000);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      var futures = new ArrayList<java.util.concurrent.Future<JsonNode>>();
      for (int i = 0; i < 8; i++)
        futures.add(pool.submit(() -> getJson(a, "/api/rounds/" + id + "/state")));
      for (var future : futures)
        assertThat(future.get().path("state").asText()).isEqualTo("COMPLETED_WIN");
    }
    assertThat(getJson(a, "/api/daily-challenge").path("rewardFragments").asInt()).isEqualTo(1);
    assertThat(getJson(a, "/api/profile/collection").path("lifetimeFragments").asInt())
        .isEqualTo(5);
    assertThat(getJson(a, "/api/wallet").path("skyFragments").asInt()).isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.daily_round_contributions WHERE round_id=?",
                Integer.class,
                UUID.fromString(id)))
        .isEqualTo(1);
  }

  @Test
  void invalidSocialParametersFailClearly() {
    assertThatThrownBy(
            () ->
                new SocialSettings(
                    10,
                    new java.math.BigDecimal("200"),
                    8,
                    3,
                    2,
                    150,
                    6,
                    new java.math.BigDecimal("2.5")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new SocialSettings(
                    90,
                    new java.math.BigDecimal("200"),
                    8,
                    0,
                    2,
                    150,
                    6,
                    new java.math.BigDecimal("2.5")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
