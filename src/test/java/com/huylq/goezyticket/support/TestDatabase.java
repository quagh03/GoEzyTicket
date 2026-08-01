package com.huylq.goezyticket.support;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;

/**
 * Plain JDBC access to the shared container, for tests that inspect the database without booting a
 * Spring context.
 */
public final class TestDatabase {

  private TestDatabase() {}

  /**
   * Stands in for the {@link Flyway} instance Spring Boot autoconfigures. The strategy takes only
   * the {@code DataSource} off it and never migrates it.
   */
  public static Flyway dataSourceCarrier() {
    return Flyway.configure(TestDatabase.class.getClassLoader())
        .dataSource(
            PostgresContainer.jdbcUrl(), PostgresContainer.username(), PostgresContainer.password())
        .load();
  }

  public static boolean schemaExists(String schema) {
    return !query("select nspname from pg_namespace where nspname = '%s'".formatted(schema))
        .isEmpty();
  }

  public static boolean tableExists(String schema, String table) {
    return !query(
            """
            select table_name from information_schema.tables
             where table_schema = '%s' and table_name = '%s'
            """
                .formatted(schema, table))
        .isEmpty();
  }

  public static List<String> appliedVersions(String schema, String historyTable) {
    return query(
        """
        select version from "%s"."%s"
         where success = true and version is not null
         order by installed_rank
        """
            .formatted(schema, historyTable));
  }

  private static List<String> query(String sql) {
    try (Connection connection =
            DriverManager.getConnection(
                PostgresContainer.jdbcUrl(),
                PostgresContainer.username(),
                PostgresContainer.password());
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet rs = statement.executeQuery()) {

      List<String> rows = new ArrayList<>();
      while (rs.next()) {
        rows.add(rs.getString(1));
      }
      return rows;
    } catch (SQLException e) {
      return fail("Query failed: " + sql, e);
    }
  }
}
