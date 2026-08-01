package com.huylq.goezyticket.config.flyway;

import com.huylq.goezyticket.support.PostgresContainer;
import com.huylq.goezyticket.support.TestDatabase;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The property that justifies one Flyway instance per module: two modules may both number a
 * migration {@code V1}.
 *
 * <p>Both halves matter. The first test shows the arrangement works; the second shows what the
 * obvious alternative — a single Flyway across all module folders — actually does, which is refuse
 * to start. Without that contrast the design looks like needless machinery, and someone will
 * "simplify" it back into a shared history table and a per-module version-lane convention.
 *
 * <p>Runs the strategy directly against the container rather than through a Spring context: the
 * behaviour under test is the strategy's, and no application wiring is involved.
 */
@DisplayName("Per-module Flyway instances isolate version numbers")
class ModularFlywayVersionIsolationTests {

  private static final String LOCATION_PREFIX = "classpath:db/collision";
  private static final String HISTORY_TABLE = "flyway_schema_history";

  @Test
  @DisplayName("two modules may both define V1")
  void twoModulesMayBothDefineV1() {
    ModuleMigrationProperties properties =
        new ModuleMigrationProperties(
            List.of("alpha", "beta"),
            // Its own app schema so this test cannot collide with the integration test, which
            // shares the same container and migrates into `public`.
            "collision_app",
            LOCATION_PREFIX,
            HISTORY_TABLE,
            false);

    new ModularFlywayMigrationStrategy(properties).migrate(TestDatabase.dataSourceCarrier());

    // Same version, same filename, different schemas - and both applied.
    assertThat(TestDatabase.appliedVersions("alpha", HISTORY_TABLE)).containsExactly("1");
    assertThat(TestDatabase.appliedVersions("beta", HISTORY_TABLE)).containsExactly("1");
    assertThat(TestDatabase.tableExists("alpha", "widget")).isTrue();
    assertThat(TestDatabase.tableExists("beta", "widget")).isTrue();
  }

  @Test
  @DisplayName("a single Flyway across both folders refuses to start")
  void aSingleFlywayAcrossBothFoldersRefusesToStart() {
    Flyway single =
        Flyway.configure(getClass().getClassLoader())
            .dataSource(
                PostgresContainer.jdbcUrl(),
                PostgresContainer.username(),
                PostgresContainer.password())
            .schemas("collision_single")
            .defaultSchema("collision_single")
            .createSchemas(true)
            .locations(LOCATION_PREFIX + "/alpha", LOCATION_PREFIX + "/beta")
            .load();

    assertThatThrownBy(single::migrate)
        .isInstanceOf(FlywayException.class)
        .hasMessageContaining("more than one migration with version");
  }
}
