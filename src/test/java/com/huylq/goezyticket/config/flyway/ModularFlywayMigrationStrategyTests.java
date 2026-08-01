package com.huylq.goezyticket.config.flyway;

import com.huylq.goezyticket.support.TestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The strategy's behaviour when it is handed nothing to do.
 *
 * <p>Worth a test rather than a shrug: an unset {@code goezyticket.migration.modules} is a plausible
 * misconfiguration — a typo'd prefix, a profile that forgot to inherit it — and the strategy must
 * fail visibly in the log rather than throw a {@link NullPointerException} halfway through creating
 * schemas, leaving the database in a state nobody planned.
 */
@DisplayName("ModularFlywayMigrationStrategy with nothing configured")
class ModularFlywayMigrationStrategyTests {

  private static final String UNUSED_SCHEMA = "guard_app";

  @Test
  @DisplayName("warns and touches nothing when no modules are configured")
  void warnsAndTouchesNothingWhenNoModulesAreConfigured() {
    ModuleMigrationProperties properties =
        new ModuleMigrationProperties(
            List.of(), UNUSED_SCHEMA, "classpath:db/migration", "flyway_schema_history", false);

    assertThatCode(() -> new ModularFlywayMigrationStrategy(properties).migrate(TestDatabase.dataSourceCarrier()))
        .doesNotThrowAnyException();

    // Nothing at all, not even the app schema: with no modules configured the whole run is a
    // no-op. Startup then fails later on `ddl-auto: validate` complaining about a missing
    // event_publication - see the note in the class javadoc of the strategy.
    assertThat(TestDatabase.schemaExists(UNUSED_SCHEMA)).isFalse();
  }

  @Test
  @DisplayName("treats a null module list the same as an empty one")
  void treatsNullModuleListTheSameAsEmpty() {
    ModuleMigrationProperties properties =
        new ModuleMigrationProperties(
            null, UNUSED_SCHEMA, "classpath:db/migration", "flyway_schema_history", false);

    assertThatCode(() -> new ModularFlywayMigrationStrategy(properties).migrate(TestDatabase.dataSourceCarrier()))
        .doesNotThrowAnyException();

    assertThat(TestDatabase.schemaExists(UNUSED_SCHEMA)).isFalse();
  }
}
