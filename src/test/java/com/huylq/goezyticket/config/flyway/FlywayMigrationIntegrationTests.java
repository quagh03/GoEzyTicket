package com.huylq.goezyticket.config.flyway;

import com.huylq.goezyticket.support.PostgresContainer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the checklist item "migrations run cleanly from an empty database to head" (plan §8), and
 * that the per-module layout of §2.6 actually holds in the database rather than only on paper.
 *
 * <p>The container starts empty, so reaching a running context at all means Flyway created every
 * schema unaided. {@code ddl-auto} is forced to {@code validate} — that is what makes this a real
 * test: Hibernate refuses to start unless the migrated schema matches the mapped entities, which is
 * precisely the failure that {@code update} used to hide.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("test")
@DisplayName("Flyway migrates an empty database to head")
class FlywayMigrationIntegrationTests {

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", PostgresContainer::jdbcUrl);
    registry.add("spring.datasource.username", PostgresContainer::username);
    registry.add("spring.datasource.password", PostgresContainer::password);
  }

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ModuleMigrationProperties properties;
  @Autowired private FlywayMigrationStrategy strategy;
  @Autowired private Flyway autoConfiguredFlyway;

  @Test
  @DisplayName("configured modules match the module diagram in the plan")
  void configuredModulesMatchThePlan() {
    // Guards against a module being added to the codebase but forgotten here - in which case its
    // schema would silently never be created.
    assertThat(properties.modules())
        .containsExactly(
            "identity", "catalog", "inventory", "ordering", "payment", "ticketing", "notification");
  }

  @Test
  @DisplayName("every module gets its own schema")
  void everyModuleGetsItsOwnSchema() {
    for (String module : properties.modules()) {
      assertThat(schemaExists(module)).as("schema %s", module).isTrue();
    }
  }

  @Test
  @DisplayName("every module's history table lives inside its own schema")
  void everyModuleKeepsItsHistoryTableInItsOwnSchema() {
    // The whole point of one Flyway instance per module: a module's migration history travels with
    // it, so extraction in semester 2 is a datasource change rather than a table split.
    for (String module : properties.modules()) {
      assertThat(tableExists(module, properties.historyTable()))
          .as("%s.%s", module, properties.historyTable())
          .isTrue();
    }
  }

  @Test
  @DisplayName("no module leaks its history into the app schema")
  void noModuleLeaksItsHistoryIntoTheAppSchema() {
    // A single shared history table in `public` is exactly the arrangement this design replaces.
    List<String> tablesInAppSchema = tablesIn(properties.appSchema());

    assertThat(tablesInAppSchema)
        .contains("event_publication", properties.historyTable())
        .doesNotContain("identity_flyway_schema_history");
    assertThat(historyRowCount(properties.appSchema()))
        .as("app schema history should only record app migrations")
        .isEqualTo(1);
  }

  @Test
  @DisplayName("the app schema owns Spring Modulith's event registry")
  void appSchemaOwnsTheEventPublicationRegistry() {
    assertThat(tableExists(properties.appSchema(), "event_publication")).isTrue();

    // Widened from Hibernate's default varchar(255): a serialized event with a couple of UUIDs and
    // an email exceeds that, and the failure would only surface at insert time under load.
    String type =
        jdbc.queryForObject(
            """
            select data_type from information_schema.columns
             where table_schema = ? and table_name = 'event_publication'
               and column_name = 'serialized_event'
            """,
            String.class,
            properties.appSchema());

    assertThat(type).isEqualTo("text");
  }

  @Test
  @DisplayName("re-running the migration changes nothing")
  void reRunningTheMigrationChangesNothing() {
    // Startup must be repeatable: an app restart, or a second instance booting, must not re-apply
    // or re-validate its way into a failure.
    List<Integer> before = historyRowCounts();

    strategy.migrate(autoConfiguredFlyway);

    assertThat(historyRowCounts()).isEqualTo(before);
  }

  private boolean schemaExists(String schema) {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from pg_namespace where nspname = ?", Integer.class, schema);
    return count != null && count > 0;
  }

  private boolean tableExists(String schema, String table) {
    Integer count =
        jdbc.queryForObject(
            """
            select count(*) from information_schema.tables
             where table_schema = ? and table_name = ?
            """,
            Integer.class,
            schema,
            table);
    return count != null && count > 0;
  }

  private List<String> tablesIn(String schema) {
    return jdbc.queryForList(
        "select table_name from information_schema.tables where table_schema = ?",
        String.class,
        schema);
  }

  private int historyRowCount(String schema) {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from \"%s\".\"%s\"".formatted(schema, properties.historyTable()),
            Integer.class);
    return count == null ? 0 : count;
  }

  private List<Integer> historyRowCounts() {
    return java.util.stream.Stream.concat(
            java.util.stream.Stream.of(properties.appSchema()), properties.modules().stream())
        .map(this::historyRowCount)
        .toList();
  }
}
