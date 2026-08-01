package com.huylq.goezyticket.config.flyway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ModuleMigrationProperties.class)
public class MigrationConfiguration {

  @Bean
  public FlywayMigrationStrategy modularFlywayMigrationStrategy(ModuleMigrationProperties properties) {
    return new ModularFlywayMigrationStrategy(properties);
  }

}
