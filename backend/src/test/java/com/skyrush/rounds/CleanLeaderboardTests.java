package com.skyrush.rounds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyrush.users.DemoUserService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Own fresh PostgreSQL instance: other integration fixtures cannot contaminate startup assertions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CleanLeaderboardTests {
  static final EmbeddedPostgres DB = startDatabase();

  private static EmbeddedPostgres startDatabase() {
    try {
      return EmbeddedPostgres.builder().start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> DB.getJdbcUrl("postgres", "postgres"));
    r.add("spring.datasource.username", () -> "postgres");
    r.add("spring.datasource.password", () -> "");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired DemoUserService demo;

  MockHttpSession login() throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @Test
  void freshStartupAndRepeatedSeedingContainOnlyRealDemoAccount() throws Exception {
    demo.run(null);
    for (String table :
        java.util.List.of(
            "user_achievements",
            "daily_challenge_progress",
            "user_presence",
            "player_activity_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM skyrush." + table, Long.class)).isZero();
    mvc.perform(get("/api/activity").session(login()))
        .andExpect(jsonPath("$.online").value(0))
        .andExpect(jsonPath("$.events").isEmpty());
    assertThat(jdbc.queryForList("SELECT username FROM skyrush.users", String.class))
        .containsExactly("demo");
    mvc.perform(get("/api/tournament?masked=false").session(login()))
        .andExpect(jsonPath("$.entries.length()").value(1))
        .andExpect(jsonPath("$.entries[0].id").value(DemoUserService.DEMO_ID.toString()))
        .andExpect(jsonPath("$.entries[0].points").value(0))
        .andExpect(jsonPath("$.entries[0].simulated").doesNotExist());
  }

  @Test
  void registrationAddsExactlyThoseAccountsWithoutInjectedCompetitors() throws Exception {
    for (String name : new String[] {"alice", "bob"}) {
      mvc.perform(
              post("/api/auth/register")
                  .with(csrf())
                  .contentType("application/json")
                  .content(
                      json.writeValueAsString(
                          Map.of(
                              "username", name, "displayName", name, "password", "password123"))))
          .andExpect(status().isOk());
    }
    var response =
        mvc.perform(get("/api/tournament?masked=false").session(login()))
            .andExpect(status().isOk())
            .andReturn();
    var entries = json.readTree(response.getResponse().getContentAsString()).path("entries");
    var ids = new java.util.ArrayList<String>();
    entries.forEach(e -> ids.add(e.path("id").asText()));
    assertThat(ids)
        .containsExactlyInAnyOrderElementsOf(
            jdbc.queryForList("SELECT id::text FROM skyrush.users", String.class));
    assertThat(ids).hasSize(3);
    assertThat(entries).allMatch(e -> e.path("points").asInt() == 0 && !e.has("simulated"));
  }
}
