package com.huylq.goezyticket.platform.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-01 and AC-05 — platform only defines the <em>shape</em> of an error code.
 *
 * <p>Pure unit test: no Spring, no classpath scan.
 */
@DisplayName("CommonErrorCode")
class CommonErrorCodeTest {

  /** §4.4 classification line — a code is COMMON only if it means the same thing in every service. */
  private static final int MAX_CONSTANTS = 15;

  /**
   * Nouns that belong to a single module. Their presence in platform is the exact coupling
   * US-00.6-02 exists to prevent.
   */
  private static final List<String> BUSINESS_NOUNS =
      List.of("seat", "order", "payment", "ticket", "show", "event", "user");

  @Test
  @DisplayName("stays within the ceiling of 15 constants")
  void staysUnderTheHardCeiling() {
    assertThat(CommonErrorCode.values())
        .describedAs(
            """
            CommonErrorCode has %d constants, ceiling is %d.
            Do not raise the ceiling: apply the classification line (design doc §4.4) instead —
            "if this module became its own service, would that service need this code, with exactly
            the same meaning?" Yes for every service -> COMMON. Otherwise -> the module's own enum
            in <module>/api.""",
            CommonErrorCode.values().length, MAX_CONSTANTS)
        .hasSizeLessThanOrEqualTo(MAX_CONSTANTS);
  }

  @ParameterizedTest
  @EnumSource(CommonErrorCode.class)
  @DisplayName("names no module concept — only cross-cutting failures")
  void carriesNoBusinessSemantics(CommonErrorCode errorCode) {
    String haystack = (errorCode.name() + " " + errorCode.code()).toLowerCase(Locale.ROOT);

    assertThat(BUSINESS_NOUNS)
        .describedAs(
            """
            %s reads like a business error. A module-specific code must live in that module's own
            enum (e.g. inventory/api/InventoryErrorCode), so adding one never touches platform and
            the code follows the module when it is extracted into a service in HK2.""",
            errorCode.name())
        .noneSatisfy(noun -> assertThat(haystack).contains(noun));
  }

  @ParameterizedTest
  @EnumSource(CommonErrorCode.class)
  @DisplayName("code() is COMMON.<CONSTANT NAME>, so the catalog cannot drift from the source")
  void codeMirrorsTheConstantName(CommonErrorCode errorCode) {
    assertThat(errorCode.code()).isEqualTo("COMMON." + errorCode.name());
  }

  @ParameterizedTest
  @EnumSource(CommonErrorCode.class)
  @DisplayName("every constant declares a category and a non-blank default message")
  void everyConstantIsFullyPopulated(CommonErrorCode errorCode) {
    assertThat(errorCode.category()).isNotNull();
    assertThat(errorCode.defaultMessage()).isNotBlank();
  }

  @Test
  @DisplayName("no two constants share a code")
  void noTwoConstantsShareACode() {
    assertThat(Arrays.stream(CommonErrorCode.values()).map(CommonErrorCode::code))
        .doesNotHaveDuplicates();
  }
}
