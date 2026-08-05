package com.huylq.goezyticket.platform.error;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Finds every {@link ErrorCode} implementation on the classpath.
 *
 * <p>Shared by {@link ErrorCodeCatalogTest} and {@link ErrorCodePlacementTest}. Scanning is what
 * makes those tests future-proof: a module enum added in EP-02 is checked without anyone editing a
 * list here, which is the whole point of R-10 (derive the prefix from the package, never hard-code
 * the module names).
 */
final class ErrorCodeClasspath {

  static final String BASE_PACKAGE = "com.huylq.goezyticket";

  /** Where a COMMON code is allowed to live — the only ErrorCode package inside platform. */
  static final String PLATFORM_ERROR_PACKAGE = BASE_PACKAGE + ".platform.error";

  private ErrorCodeClasspath() {
  }

  /**
   * Production implementations only. Test fixtures (such as the stand-in enum in
   * {@code DomainExceptionTest}) are filtered out by comparing code-source locations, so a fixture
   * that deliberately borrows another module's prefix never fails the catalog rules.
   */
  static List<Class<?>> mainImplementations() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));

    URL testOutput = locationOf(ErrorCodeClasspath.class);

    return scanner.findCandidateComponents(BASE_PACKAGE).stream()
        .map(BeanDefinition::getBeanClassName)
        .<Class<?>>map(
            name -> ClassUtils.resolveClassName(name, ErrorCodeClasspath.class.getClassLoader()))
        .filter(type -> ErrorCode.class.isAssignableFrom(type) && type != ErrorCode.class)
        .filter(type -> !testOutput.equals(locationOf(type)))
        .sorted(Comparator.comparing(Class::getName))
        .toList();
  }

  /** Every declared code of an implementation, in declaration order. */
  static List<ErrorCode> constantsOf(Class<?> type) {
    Object[] constants = type.getEnumConstants();
    if (constants == null) {
      return List.of();
    }
    return Arrays.stream(constants).map(ErrorCode.class::cast).toList();
  }

  /** {@code INVENTORY} out of {@code INVENTORY.SEAT_ALREADY_HELD}; empty when there is no dot. */
  static String prefixOf(String code) {
    int dot = code.indexOf('.');
    return dot < 0 ? "" : code.substring(0, dot);
  }

  /** The package a code with this prefix must be declared in — derived, never a hard-coded list. */
  static String expectedPackagePrefix(String codePrefix) {
    return BASE_PACKAGE + "." + codePrefix.toLowerCase(Locale.ROOT);
  }

  private static URL locationOf(Class<?> type) {
    return type.getProtectionDomain().getCodeSource().getLocation();
  }
}
