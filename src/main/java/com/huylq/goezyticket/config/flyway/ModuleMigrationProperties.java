package com.huylq.goezyticket.config.flyway;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "goezyticket.migration")
public record ModuleMigrationProperties(
    List<String> modules,
    @DefaultValue("public") String appSchema,
    @DefaultValue("classpath:db/migration") String locationPrefix,
    @DefaultValue("flyway_schema_history") String historyTable,
    @DefaultValue("false") boolean baselineOnMigrate) {

  /**
   * Folder for tables that belong to no business module - currently Spring Modulith's event
   * registry. Runs against {@link #appSchema()} rather than a schema of its own.
   */
  public static final String APP_LOCATION = "app";

  public ModuleMigrationProperties {
    modules = modules == null ? List.of() : List.copyOf(modules);
  }

  public String locationOf(String module) {
    return locationPrefix + "/" + module;
  }

}
