package com.skyrush.rounds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyrush.PostgresSupport;
import com.skyrush.users.DemoUserService;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GameplayIntegrationTests.Controls.class)
class AccountIntegrationTests extends PostgresSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder passwords;
  @Autowired GameplayIntegrationTests.MutableClock clock;
  @Autowired RoundService rounds;

  @BeforeEach
  void reset() {
    clock.at(0);
    jdbc.update("DELETE FROM skyrush.wallet_entries");
    jdbc.update("DELETE FROM skyrush.game_rounds");
  }

  record Player(String username, String id, MockHttpSession session) {}

  Player register() throws Exception {
    String username = "p_" + UUID.randomUUID().toString().replace("-", "");
    var r =
        mvc.perform(
                post("/api/auth/register")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "username",
                                username,
                                "displayName",
                                username,
                                "password",
                                "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();
    return new Player(
        username,
        json.readTree(r.getResponse().getContentAsString()).path("id").asText(),
        (MockHttpSession) r.getRequest().getSession(false));
  }

  JsonNode start(Player p) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/rounds")
                    .session(p.session())
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        "{\"theme\":\"GREEN\",\"betOptionId\":\"BASIC\",\"requestId\":\""
                            + UUID.randomUUID()
                            + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void registrationHashesPasswordSeedsSeparateWalletAndRejectsDuplicate() throws Exception {
    var p = register();
    String hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM skyrush.users WHERE id=?",
            String.class,
            UUID.fromString(p.id()));
    assertThat(hash).startsWith("$2");
    assertThat(passwords.matches("password123", hash)).isTrue();
    mvc.perform(get("/api/wallet").session(p.session()))
        .andExpect(jsonPath("$.wallet.bonusBalance").value(1000));
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "username",
                            p.username().toUpperCase(),
                            "displayName",
                            "Other",
                            "password",
                            "password123"))))
        .andExpect(status().isConflict());
  }

  @Test
  void loginFailureSuccessMeAndLogout() throws Exception {
    var p = register();
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("username", p.username(), "password", "wrongpass"))))
        .andExpect(status().isUnauthorized());
    var r =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("username", p.username(), "password", "password123"))))
            .andExpect(status().isOk())
            .andReturn();
    var session = (MockHttpSession) r.getRequest().getSession(false);
    mvc.perform(get("/api/auth/me").session(session))
        .andExpect(jsonPath("$.id").value(p.id()))
        .andExpect(jsonPath("$.evaluator").value(false));
    mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
    assertThat(session.isInvalid()).isTrue();
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousPrivateApisAndMissingCsrfAreRejected() throws Exception {
    for (String path :
        List.of(
            "/api/wallet",
            "/api/history",
            "/api/rounds/active",
            "/api/upsell/tickets",
            "/api/tournament",
            "/api/users/demo")) mvc.perform(get(path)).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void registrationValidationAndNoAuthorityFields() throws Exception {
    for (var body :
        List.of(
            Map.of("username", "x", "displayName", "Name", "password", "password123"),
            Map.of("username", "valid_name", "displayName", " ", "password", "password123"),
            Map.of("username", "valid_name", "displayName", "Name", "password", "short"),
            Map.of(
                "username",
                "valid_name",
                "displayName",
                "Name",
                "password",
                "password123",
                "evaluator",
                true)))
      mvc.perform(
              post("/api/auth/register")
                  .with(csrf())
                  .contentType("application/json")
                  .content(json.writeValueAsString(body)))
          .andExpect(status().isBadRequest());
  }

  @Test
  void aliceCannotReadCashoutOrPurchaseBobsRoundAndCannotEditAdmin() throws Exception {
    var alice = register();
    var bob = register();
    var r = start(bob);
    String id = r.path("id").asText();
    mvc.perform(get("/api/rounds/" + id + "/state").session(alice.session()))
        .andExpect(status().isNotFound());
    clock.at(1000);
    mvc.perform(post("/api/rounds/" + id + "/cashout").session(alice.session()).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/upsell/offers")
                .session(alice.session())
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(Map.of("roundId", id, "sessionId", UUID.randomUUID()))))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/wallet?userId=" + bob.id()).session(alice.session()))
        .andExpect(jsonPath("$.wallet.userId").value(alice.id()))
        .andExpect(jsonPath("$.wallet.bonusBalance").value(1000));
    mvc.perform(get("/api/wallet").session(bob.session()))
        .andExpect(jsonPath("$.wallet.bonusBalance").value(990));
    mvc.perform(get("/api/admin/game-config").session(alice.session()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/rounds/active").session(alice.session()))
        .andExpect(jsonPath("$.round").isEmpty());
    clock.at(60000);
    rounds.settle(UUID.fromString(id));
    mvc.perform(get("/api/history").session(alice.session()))
        .andExpect(jsonPath("$.rounds").isEmpty());
    mvc.perform(get("/api/history").session(bob.session()))
        .andExpect(jsonPath("$.rounds[0].id").value(id));
  }

  @Test
  void sharedLeaderboardUsesPersistedPointsAndDeterministicOrdering() throws Exception {
    var a = register();
    var b = register();
    var ar = start(a);
    clock.at(1000);
    rounds.settle(UUID.fromString(ar.path("id").asText()));
    var br = start(b);
    clock.at(1500);
    rounds.settle(UUID.fromString(br.path("id").asText()));
    var board =
        json.readTree(
            mvc.perform(
                    get("/api/tournament/current/leaderboard?masked=false").session(b.session()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var rows = new ArrayList<JsonNode>();
    board.path("entries").forEach(rows::add);
    var aRow =
        rows.stream().filter(x -> x.path("id").asText().equals(a.id())).findFirst().orElseThrow();
    assertThat(aRow.path("points").asLong()).isEqualTo(10);
    assertThat(board.path("currentPlayer").path("points").asLong()).isZero();
    assertThat(aRow.path("position").asInt())
        .isLessThan(board.path("currentPlayer").path("position").asInt());
    assertThat(rows.stream().map(x -> x.path("points").asLong()).toList())
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(rows).allMatch(x -> !x.has("simulated"));
    clock.at(3000);
    rounds.settle(UUID.fromString(ar.path("id").asText()));
    mvc.perform(get("/api/tournament?masked=false").session(b.session()))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(a.username())));
    mvc.perform(
            post("/api/tournament")
                .session(b.session())
                .with(csrf())
                .contentType("application/json")
                .content("{\"points\":999}"))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(
            post("/api/rounds")
                .session(b.session())
                .with(csrf())
                .contentType("application/json")
                .content("{\"userId\":\"" + a.id() + "\",\"points\":999}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void tiedScoresHaveStableOrderAndMaskingDoesNotHideCurrentPlayer() throws Exception {
    var a = register();
    var b = register();
    String endpoint = "/api/tournament?masked=true";
    String first =
        mvc.perform(get(endpoint).session(b.session()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String second =
        mvc.perform(get(endpoint).session(b.session()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readTree(first).path("entries"))
        .isEqualTo(json.readTree(second).path("entries"));
    var rows = json.readTree(first).path("entries");
    int aRank = 0, bRank = 0;
    for (var row : rows) {
      if (row.path("id").asText().equals(a.id())) {
        aRank = row.path("position").asInt();
        assertThat(row.path("name").asText()).endsWith("***");
      }
      if (row.path("id").asText().equals(b.id())) {
        bRank = row.path("position").asInt();
        assertThat(row.path("name").asText()).isEqualTo(b.username());
      }
    }
    assertThat(aRank).isPositive();
    assertThat(aRank < bRank).isEqualTo(a.id().compareTo(b.id()) < 0);
  }

  @Test
  void loginRotatesSessionAndRealCsrfTokenWorks() throws Exception {
    var bootstrap = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
    var old = (MockHttpSession) bootstrap.getRequest().getSession(false);
    var token = json.readTree(bootstrap.getResponse().getContentAsString());
    var logged =
        mvc.perform(
                post("/api/auth/login")
                    .session(old)
                    .header(token.path("headerName").asText(), token.path("token").asText())
                    .contentType("application/json")
                    .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(old.isInvalid()).isTrue();
    var fresh = (MockHttpSession) logged.getRequest().getSession(false);
    assertThat(fresh.getId()).isNotEqualTo(old.getId());
    mvc.perform(get("/api/auth/me").session(fresh)).andExpect(status().isOk());
    mvc.perform(
            post("/api/auth/logout")
                .session(fresh)
                .header(token.path("headerName").asText(), token.path("token").asText()))
        .andExpect(status().isForbidden());
  }

  @Test
  void seededDemoLoginPreservesLegacyId() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(DemoUserService.DEMO_ID.toString()))
        .andExpect(jsonPath("$.evaluator").value(true));
  }
}
