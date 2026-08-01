package com.huylq.goezyticket.config.flyway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
public class ModularFlywayMigrationStrategy implements FlywayMigrationStrategy {

  public final ModuleMigrationProperties properties;

  @Override
  public void migrate(Flyway autoConfigured) {
    long startedAt = System.currentTimeMillis();
    log.info("Started flyway migration");

    if (properties.modules().isEmpty()) {
      log.warn("No modules configured under 'goezyticket.migration.modules' "
          + "- no schemas will be"
          + " created and no migration will run");
      return;
    }

    DataSource dataSource = autoConfigured.getConfiguration().getDataSource();

    // App-level infrastructure first: it belongs to no module and modules may come to rely on it.
    migrateInto(dataSource, ModuleMigrationProperties.APP_LOCATION, properties.appSchema());

    log.info("Migrating {} modules schemas: {}",
        properties.modules().size(),
        properties.modules()
    );

    for(String module : properties.modules()) {
      migrateInto(dataSource, module, module);
    }

    log.info("Flyway migration took: {} ms", System.currentTimeMillis() - startedAt);

  }

  /**
   * Runs one migration folder against one schema, with its history table inside that same schema.
   *
   * @param name migration folder under the location prefix, also used as the log label
   * @param schema target schema; equal to {@code name} for business modules
   */
  private void migrateInto(DataSource dataSource, String name, String schema) {
    String location = properties.locationOf(name);

    Flyway flyway =
        Flyway.configure(getClass().getClassLoader())
            .dataSource(dataSource)
            .schemas(schema)
            .defaultSchema(schema)
            .table(properties.historyTable())
            .createSchemas(true)
            .locations(location)
            .baselineOnMigrate(properties.baselineOnMigrate())
            .cleanDisabled(true)
            .load();

    MigrateResult result = flyway.migrate();

    if (result.migrationsExecuted == 0) {
      log.info("[{}] up to date at version {} ({})", name, describe(result), location);
    } else {
      log.info(
          "[{}] applied {} migration(s), now at version {} ({})",
          name,
          result.migrationsExecuted,
          describe(result),
          location);
    }
  }

  private static String describe(MigrateResult result) {
    return result.targetSchemaVersion == null ? "<empty>" : result.targetSchemaVersion;
  }

}
