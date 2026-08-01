package com.huylq.goezyticket.config.flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests — no Spring, no database. */
@DisplayName("ModuleMigrationProperties")
class ModuleMigrationPropertiesTests {

  @Test
  @DisplayName("derives a module's location from the prefix")
  void derivesLocationFromPrefix() {
    ModuleMigrationProperties properties = properties(List.of("ordering"));

    assertThat(properties.locationOf("ordering")).isEqualTo("classpath:db/migration/ordering");
    assertThat(properties.locationOf(ModuleMigrationProperties.APP_LOCATION))
        .isEqualTo("classpath:db/migration/app");
  }

  @Test
  @DisplayName("treats a missing module list as empty rather than failing")
  void treatsMissingModuleListAsEmpty() {
    // The strategy warns and skips on an empty list; a NullPointerException at startup would be a
    // far worse way to learn the property is unset.
    ModuleMigrationProperties properties = properties(null);

    assertThat(properties.modules()).isEmpty();
  }

  @Test
  @DisplayName("defends its module list against later mutation")
  void defendsItsModuleListAgainstMutation() {
    List<String> mutable = new ArrayList<>(List.of("identity"));
    ModuleMigrationProperties properties = properties(mutable);

    mutable.add("sneaked-in");

    assertThat(properties.modules()).containsExactly("identity");
    assertThatThrownBy(() -> properties.modules().add("also-not-allowed"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static ModuleMigrationProperties properties(List<String> modules) {
    return new ModuleMigrationProperties(
        modules, "public", "classpath:db/migration", "flyway_schema_history", false);
  }
}
