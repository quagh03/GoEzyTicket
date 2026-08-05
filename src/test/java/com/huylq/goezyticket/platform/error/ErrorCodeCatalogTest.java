package com.huylq.goezyticket.platform.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * AC-04 — the error catalog is globally consistent, without platform knowing any module.
 *
 * <p>Scans the classpath instead of reading a list, so every enum added later (EP-02..EP-13) is
 * covered for free. Tagged {@code architecture} so it can be moved to its own CI job (US-00.7-01)
 * if the scan ever shows up in build times.
 */
@Tag("architecture")
@DisplayName("ErrorCode catalog")
class ErrorCodeCatalogTest {

  /** Naming convention {@code <MODULE>.<REASON>}. */
  private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z]+\\.[A-Z0-9_]+$");

  private static final List<Class<?>> IMPLEMENTATIONS = ErrorCodeClasspath.mainImplementations();

  @Test
  @DisplayName("the scan itself finds something — otherwise every rule below passes vacuously")
  void theScanFindsSomething() {
    assertThat(IMPLEMENTATIONS)
        .describedAs(
            "classpath scan of %s found no ErrorCode implementation; the rules below would be"
                + " meaningless. Check the scanner, not the enums.",
            ErrorCodeClasspath.BASE_PACKAGE)
        .isNotEmpty()
        .contains(CommonErrorCode.class);
  }

  @Test
  @DisplayName("every implementation is an enum with at least one constant")
  void everyImplementationIsAnEnum() {
    assertSoftly(
        softly ->
            IMPLEMENTATIONS.forEach(
                type -> {
                  softly
                      .assertThat(type.isEnum())
                      .describedAs(
                          "%s implements ErrorCode but is not an enum. Codes must be a closed set so"
                              + " the catalog can be enumerated (printErrorCodes) and reviewed.",
                          type.getName())
                      .isTrue();
                  softly
                      .assertThat(ErrorCodeClasspath.constantsOf(type))
                      .describedAs("%s declares no code", type.getName())
                      .isNotEmpty();
                }));
  }

  @Test
  @DisplayName("every code matches the <MODULE>.<REASON> convention")
  void everyCodeMatchesTheNamingConvention() {
    assertSoftly(
        softly ->
            forEachCode(
                (type, code) ->
                    softly
                        .assertThat(code.code())
                        .describedAs(
                            "%s.%s does not follow <MODULE>.<REASON>",
                            type.getSimpleName(), ((Enum<?>) code).name())
                        .matches(CODE_FORMAT.pattern())));
  }

  @Test
  @DisplayName("no two codes collide, anywhere on the classpath")
  void noTwoCodesCollide() {
    Map<String, List<String>> owners = new LinkedHashMap<>();
    forEachCode(
        (type, code) ->
            owners
                .computeIfAbsent(code.code(), key -> new ArrayList<>())
                .add(type.getName() + "." + ((Enum<?>) code).name()));

    Map<String, List<String>> duplicates =
        owners.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(LinkedHashMap::new, (map, e) -> map.put(e.getKey(), e.getValue()), Map::putAll);

    assertThat(duplicates)
        .describedAs(
            "these codes are declared more than once. A code is a public contract: two owners means"
                + " a client cannot tell which failure it is looking at.")
        .isEmpty();
  }

  @Test
  @DisplayName("the prefix matches the module package that declares the enum")
  void thePrefixMatchesTheDeclaringModule() {
    assertSoftly(
        softly ->
            forEachCode(
                (type, code) -> {
                  String prefix = ErrorCodeClasspath.prefixOf(code.code());
                  String packageName = type.getPackageName();

                  if ("COMMON".equals(prefix)) {
                    softly
                        .assertThat(packageName)
                        .describedAs(
                            "%s uses the COMMON prefix but is declared outside platform. COMMON is"
                                + " reserved for %s.",
                            code.code(), ErrorCodeClasspath.PLATFORM_ERROR_PACKAGE)
                        .isEqualTo(ErrorCodeClasspath.PLATFORM_ERROR_PACKAGE);
                    return;
                  }

                  softly
                      .assertThat(packageName)
                      .describedAs(
                          """
                          %s is declared in %s. The prefix is derived from the package, not from a
                          list: a code prefixed %s must live under %s so it travels with its module
                          when the module is extracted into a service.""",
                          code.code(),
                          packageName,
                          prefix,
                          ErrorCodeClasspath.expectedPackagePrefix(prefix))
                      .startsWith(ErrorCodeClasspath.expectedPackagePrefix(prefix) + ".");
                }));
  }

  @Test
  @DisplayName("every code declares a category and a non-blank default message")
  void everyCodeIsFullyPopulated() {
    assertSoftly(
        softly ->
            forEachCode(
                (type, code) -> {
                  String where = type.getSimpleName() + "." + ((Enum<?>) code).name();
                  softly
                      .assertThat(code.category())
                      .describedAs("%s has no category, so no HTTP status can be derived", where)
                      .isNotNull();
                  softly
                      .assertThat(code.defaultMessage())
                      .describedAs("%s has no default message", where)
                      .isNotBlank();
                }));
  }

  private static void forEachCode(java.util.function.BiConsumer<Class<?>, ErrorCode> assertion) {
    IMPLEMENTATIONS.forEach(
        type -> ErrorCodeClasspath.constantsOf(type).forEach(code -> assertion.accept(type, code)));
  }
}
