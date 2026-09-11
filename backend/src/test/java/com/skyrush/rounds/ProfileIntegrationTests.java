package com.skyrush.rounds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyrush.PostgresSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class ProfileIntegrationTests extends PostgresSupport {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired GameplayIntegrationTests.MutableClock clock;
  @Autowired GameplayIntegrationTests.ScriptedRandom random;
  long time;

  @BeforeEach
  void reset() {
    time = 0;
    clock.at(0);
    random.set("0.9", "0");
  }

  MockHttpSession register() throws Exception {
    var result =
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
                                "Player",
                                "password",
                                "password123"))))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  JsonNode read(MockHttpSession p, String path) throws Exception {
    return json.readTree(
        mvc.perform(get(path).session(p))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode start(MockHttpSession p, String bet) throws Exception {
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
                                bet,
                                "requestId",
                                UUID.randomUUID()))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode finish(MockHttpSession p, boolean win, String bet) throws Exception {
    String id = start(p, bet).path("id").asText();
    clock.at(time + 1000);
    if (win)
      mvc.perform(post("/api/rounds/" + id + "/cashout").session(p).with(csrf()))
          .andExpect(status().isOk());
    time += 60000;
    clock.at(time);
    return read(p, "/api/rounds/" + id + "/state");
  }

  @Test
  void emptyProfileIsPrivateAndDoesNotRevealActiveOutcome() throws Exception {
    mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/profile/collection")).andExpect(status().isUnauthorized());
    var a = register();
    var b = register();
    String bId = read(b, "/api/profile").path("account").path("id").asText();
    start(a, "BASIC");
    var p = read(a, "/api/profile?userId=" + bId);
    assertThat(p.path("account").path("id").asText()).isNotEqualTo(bId);
    assertThat(p.path("records").path("totalRounds").asInt()).isZero();
    assertThat(p.path("records").path("highestCrashMultiplier").isNull()).isTrue();
    assertThat(p.path("collection").path("lifetimeFragments").asInt()).isZero();
  }

  @Test
  void lifetimeRecordsAndStreakUseCompletedServerResults() throws Exception {
    var p = register();
    List<JsonNode> results =
        List.of(
            finish(p, true, "TRIPLE"),
            finish(p, true, "QUAD"),
            finish(p, false, "BASIC"),
            finish(p, true, "BASIC"));
    var profile = read(p, "/api/profile");
    var r = profile.path("records");
    assertThat(r.path("totalRounds").asInt()).isEqualTo(4);
    assertThat(r.path("successfulCashouts").asInt()).isEqualTo(3);
    assertThat(r.path("losses").asInt()).isEqualTo(1);
    assertThat(r.path("winRate").decimalValue()).isEqualByComparingTo("75");
    assertThat(r.path("currentWinStreak").asInt()).isEqualTo(1);
    assertThat(r.path("bestWinStreak").asInt()).isEqualTo(2);
    assertThat(r.path("highestCashoutMultiplier").decimalValue()).isEqualByComparingTo("5");
    assertThat(r.path("biggestPayout").decimalValue()).isEqualByComparingTo("200");
    assertThat(r.path("strongestBooster").asInt()).isEqualTo(4);
    assertThat(r.path("highestCrashMultiplier").decimalValue())
        .isEqualByComparingTo(
            results.stream()
                .map(x -> x.path("crashMultiplier").decimalValue())
                .max(BigDecimal::compareTo)
                .orElseThrow());
    assertThat(r.path("highestLevel").asInt())
        .isEqualTo(
            results.stream().mapToInt(x -> x.path("completedLevel").asInt()).max().orElseThrow());
    assertThat(r.path("totalPoints").asLong())
        .isEqualTo(results.stream().mapToLong(x -> x.path("earnedPoints").asLong()).sum());
    assertThat(r.path("totalFragments").asInt()).isEqualTo(8);
    assertThat(profile.path("fragmentBalance").asInt()).isEqualTo(3);
    assertThat(profile.path("collection").path("lifetimeFragments").asInt()).isEqualTo(8);
    assertThat(
            results.stream()
                .mapToInt(x -> x.path("reward").path("fragmentsRedeemed").asInt())
                .sum())
        .isEqualTo(5);
    finish(p, false, "BASIC");
    assertThat(read(p, "/api/profile").path("records").path("currentWinStreak").asInt()).isZero();
  }

  @Test
  void unlockAtLifetimeThresholdIsPersistentUniqueAndOwned() throws Exception {
    var a = register();
    var b = register();
    finish(a, false, "BASIC");
    finish(a, false, "BASIC");
    finish(a, true, "BASIC");
    var before = read(a, "/api/profile/collection");
    assertThat(before.path("fragmentsToNext").asInt()).isEqualTo(1);
    var round = finish(a, true, "BASIC");
    var collection = read(a, "/api/profile/collection");
    var unlocked = new ArrayList<JsonNode>();
    collection.path("items").forEach(unlocked::add);
    var skin =
        unlocked.stream()
            .filter(i -> i.path("id").asText().equals("constellations"))
            .findFirst()
            .orElseThrow();
    assertThat(skin.path("unlocked").asBoolean()).isTrue();
    assertThat(skin.path("unlockedRoundId").asText()).isEqualTo(round.path("id").asText());
    for (int i = 0; i < 3; i++) {
      read(a, "/api/rounds/" + round.path("id").asText() + "/state");
      assertThat(read(a, "/api/profile/collection")).isEqualTo(collection);
    }
    String id = read(a, "/api/profile").path("account").path("id").asText();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.user_cosmetic_unlocks WHERE user_id=? AND cosmetic_id='constellations'",
                Integer.class,
                UUID.fromString(id)))
        .isEqualTo(1);
    assertThat(read(b, "/api/profile").path("records").path("totalRounds").asInt()).isZero();
    assertThat(read(b, "/api/profile/collection").path("lifetimeFragments").asInt()).isZero();
  }

  @Test
  void equipRequiresUnlockedItemCsrfAndRejectsAuthorityFields() throws Exception {
    var a = register();
    var b = register();
    mvc.perform(
            post("/api/profile/cosmetics/equip")
                .session(a)
                .with(csrf())
                .contentType("application/json")
                .content("{\"cosmeticId\":\"bronze\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COSMETIC_LOCKED"));
    for (String body :
        List.of(
            "{\"cosmeticId\":\"plain\",\"userId\":\"other\"}",
            "{\"cosmeticId\":\"plain\",\"lifetimeFragments\":999}",
            "{\"cosmeticId\":\"plain\",\"points\":999}",
            "{\"cosmeticId\":\"\"}"))
      mvc.perform(
              post("/api/profile/cosmetics/equip")
                  .session(a)
                  .with(csrf())
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/profile/cosmetics/equip")
                .session(a)
                .contentType("application/json")
                .content("{\"cosmeticId\":\"plain\"}"))
        .andExpect(status().isForbidden());
    for (int i = 0; i < 5; i++) finish(a, true, "BASIC");
    var before = read(a, "/api/wallet");
    for (String item : List.of("constellations", "bronze", "bronze"))
      mvc.perform(
              post("/api/profile/cosmetics/equip")
                  .session(a)
                  .with(csrf())
                  .contentType("application/json")
                  .content("{\"cosmeticId\":\"" + item + "\"}"))
          .andExpect(status().isOk());
    var c = read(a, "/api/profile/collection");
    assertThat(c.path("balloonSkin").asText()).isEqualTo("constellations");
    assertThat(c.path("profileFrame").asText()).isEqualTo("bronze");
    assertThat(read(a, "/api/wallet")).isEqualTo(before);
    assertThat(read(b, "/api/profile/collection").path("profileFrame").asText()).isEqualTo("plain");
    assertThat(read(a, "/api/profile").path("collection")).isEqualTo(c);
  }
}
