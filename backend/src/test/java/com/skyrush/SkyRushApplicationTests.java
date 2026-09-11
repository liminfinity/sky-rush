package com.skyrush;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkyRushApplicationTests extends PostgresSupport {
  @Autowired MockMvc mvc;
  @Autowired Flyway flyway;
  @Autowired JdbcTemplate jdbc;

  @Test
  void healthEndpointReportsHealthyDatabase() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void openApiIsAvailable() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists());
  }

  @Test
  void restartKeepsHistoryInPublicWhenRoleSchemaExists() {
    // PostgreSQL's default "$user",public path changes after V1 creates skyrush.
    jdbc.execute("CREATE ROLE skyrush LOGIN");
    jdbc.execute("GRANT USAGE ON SCHEMA skyrush TO skyrush");
    jdbc.execute("GRANT SELECT ON ALL TABLES IN SCHEMA public TO skyrush");
    String url = POSTGRES.getJdbcUrl("skyrush", "postgres");
    JdbcTemplate roleJdbc =
        new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(url));
    assertThat(roleJdbc.queryForObject("select current_schema()", String.class))
        .isEqualTo("skyrush");
    assertThat(flyway.getConfiguration().getDefaultSchema()).isEqualTo("public");
    Flyway restarted =
        Flyway.configure()
            .configuration(flyway.getConfiguration())
            .dataSource(url, "skyrush", "")
            .load();
    assertThat(restarted.migrate().migrationsExecuted).isZero();
    assertThat(restarted.info().current().getVersion().toString()).isEqualTo("6");
  }

  @Test
  void initialMigrationIsApplied() {
    assertThat(flyway.info().current().getVersion().toString()).isEqualTo("6");
    assertThat(flyway.info().pending()).isEmpty();
  }
}
