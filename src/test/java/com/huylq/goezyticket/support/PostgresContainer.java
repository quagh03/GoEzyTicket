package com.huylq.goezyticket.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

public final class PostgresContainer {

  private static final PostgreSQLContainer INSTANCE =
      new PostgreSQLContainer("postgres:17").withDatabaseName("goezyticket_test");

  static {
    INSTANCE.start();
  }

  private PostgresContainer() {}

  public static PostgreSQLContainer instance() {
    return INSTANCE;
  }

  public static String jdbcUrl() {
    return INSTANCE.getJdbcUrl();
  }

  public static String username() {
    return INSTANCE.getUsername();
  }

  public static String password() {
    return INSTANCE.getPassword();
  }
}
