package com.huylq.goezyticket.platform.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * AC-01 and AC-09 — R-03/R-04/R-05: platform defines the shape, modules declare the codes, and a
 * code enum sits in the module's public API package.
 *
 * <p>These are the rules that turn discipline into a red build. They are expressed as a classpath
 * scan rather than ArchUnit only because ArchUnit is not a dependency yet (T-06); the rule content is
 * the same and moving it later changes no assertion.
 */
@Tag("architecture")
@DisplayName("ErrorCode placement")
class ErrorCodePlacementTest {

  private static final List<Class<?>> IMPLEMENTATIONS = ErrorCodeClasspath.mainImplementations();

  /** A code enum is a public contract, like a gRPC status — never an internal detail (D-05). */
  private static final String API_PACKAGE_SUFFIX = ".api";

  @Test
  @DisplayName("platform declares exactly one implementation, CommonErrorCode")
  void platformDeclaresExactlyOneImplementation() {
    List<Class<?>> inPlatform =
        IMPLEMENTATIONS.stream()
            .filter(type -> type.getPackageName().startsWith(ErrorCodeClasspath.BASE_PACKAGE + ".platform"))
            .toList();

    assertThat(inPlatform)
        .describedAs(
            """
            platform must define the shape of an error code, not the catalog. A second implementation
            here means every module recompiles when any module adds a code — the coupling US-00.6-02
            exists to prevent. Declare the code in <module>/api/<Module>ErrorCode instead.""")
        .containsExactly(CommonErrorCode.class);
  }

  @Test
  @DisplayName("a module's code enum lives in <module>/api, never in domain or infra")
  void moduleCodeEnumsLiveInTheApiPackage() {
    assertSoftly(
        softly ->
            IMPLEMENTATIONS.stream()
                .filter(type -> type != CommonErrorCode.class)
                .forEach(
                    type -> {
                      String packageName = type.getPackageName();
                      softly
                          .assertThat(packageName)
                          .describedAs(
                              """
                              %s is declared in %s. Move it to the module's api package: a code is
                              part of the module's published contract, so callers must be able to
                              reference it without reaching into domain or infra (D-05).""",
                              type.getSimpleName(), packageName)
                          .endsWith(API_PACKAGE_SUFFIX);
                    }));
  }

  @Test
  @DisplayName("platform/error exposes the shape only — the interface plus its one enum")
  void platformErrorExposesTheShapeOnly() {
    // Guards the direction of the dependency: platform knows ErrorCode and ErrorCategory, and
    // nothing about who implements them.
    assertThat(ErrorCode.class.isInterface()).isTrue();
    assertThat(CommonErrorCode.class.getPackageName())
        .isEqualTo(ErrorCodeClasspath.PLATFORM_ERROR_PACKAGE);
    assertThat(ErrorCode.class.getPackageName())
        .isEqualTo(ErrorCodeClasspath.PLATFORM_ERROR_PACKAGE);
  }
}
