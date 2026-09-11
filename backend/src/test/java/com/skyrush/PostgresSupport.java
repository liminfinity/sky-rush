package com.skyrush;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** A real PostgreSQL 17 process, disposable and bound to a random local port. */
public abstract class PostgresSupport {
  static final EmbeddedPostgres POSTGRES = start();

  private static EmbeddedPostgres start() {
    try {
      return EmbeddedPostgres.builder().start();
    } catch (Exception ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
    registry.add("spring.datasource.username", () -> "postgres");
    registry.add("spring.datasource.password", () -> "");
  }
}
