package com.huylq.goezyticket.platform.web;

import com.huylq.goezyticket.platform.error.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * AC-06 — the category to HTTP mapping is total and intentional.
 *
 * <p>The mapper is an exhaustive {@code switch} with no {@code default}, so forgetting a new
 * {@link ErrorCategory} value fails at <em>compile</em> time. These tests keep the runtime side
 * honest: nothing silently collapses to 500, and the 4xx/5xx split agrees with
 * {@link ErrorCategory#fault()}.
 */
@DisplayName("ErrorCategoryHttpMapper")
class ErrorCategoryHttpMapperTest {

  /**
   * The mapping documented in ADR-004 (12 rows), written as wire status codes rather than
   * {@link HttpStatus} constants: the number is the contract a client sees, and Spring has more than
   * one constant for 422 (UNPROCESSABLE_ENTITY / UNPROCESSABLE_CONTENT).
   */
  private static final Map<ErrorCategory, Integer> EXPECTED =
      Map.ofEntries(
          Map.entry(ErrorCategory.VALIDATION, 422),
          Map.entry(ErrorCategory.BAD_REQUEST, 400),
          Map.entry(ErrorCategory.UNAUTHENTICATED, 401),
          Map.entry(ErrorCategory.FORBIDDEN, 403),
          Map.entry(ErrorCategory.NOT_FOUND, 404),
          Map.entry(ErrorCategory.METHOD_NOT_ALLOWED, 405),
          Map.entry(ErrorCategory.CONFLICT, 409),
          Map.entry(ErrorCategory.UNSUPPORTED_MEDIA_TYPE, 415),
          Map.entry(ErrorCategory.RATE_LIMITED, 429),
          Map.entry(ErrorCategory.INTERNAL, 500),
          Map.entry(ErrorCategory.UNAVAILABLE, 503),
          Map.entry(ErrorCategory.TIMEOUT, 504));

  @Test
  @DisplayName("every category has an explicit, expected status — none is left unmapped")
  void everyCategoryHasAnExpectedStatus() {
    assertThat(EXPECTED.keySet())
        .describedAs(
            """
            A new ErrorCategory value was added without a row here. Add the mapping in
            ErrorCategoryHttpMapper (the switch has no default branch, so it will not compile
            otherwise) and record the row — including the gRPC column for HK2 — in ADR-004.""")
        .containsExactlyInAnyOrder(ErrorCategory.values());
  }

  @ParameterizedTest
  @EnumSource(ErrorCategory.class)
  @DisplayName("maps each category to its documented status without throwing")
  void mapsEachCategoryToItsDocumentedStatus(ErrorCategory category) {
    assertThatCode(() -> ErrorCategoryHttpMapper.toHttpStatus(category)).doesNotThrowAnyException();

    HttpStatus status = ErrorCategoryHttpMapper.toHttpStatus(category);

    assertThat(status).isNotNull();
    assertThat(status.value()).isEqualTo(EXPECTED.get(category));
  }

  @Test
  @DisplayName("no two categories collapse onto the same status")
  void noTwoCategoriesCollapseOntoTheSameStatus() {
    Map<Integer, Long> perStatus =
        Arrays.stream(ErrorCategory.values())
            .collect(
                Collectors.groupingBy(
                    category -> ErrorCategoryHttpMapper.toHttpStatus(category).value(),
                    Collectors.counting()));

    assertThat(perStatus)
        .describedAs("distinct categories must stay distinguishable over HTTP")
        .allSatisfy((status, count) -> assertThat(count).isEqualTo(1L));
  }

  @ParameterizedTest
  @EnumSource(ErrorCategory.class)
  @DisplayName("caller faults map to 4xx and server faults to 5xx")
  void faultClassificationAgreesWithTheStatusSeries(ErrorCategory category) {
    HttpStatus status = ErrorCategoryHttpMapper.toHttpStatus(category);

    // The handler decides log level and stack-trace printing from fault(); a category whose fault()
    // disagrees with its status would log a client mistake as a server incident, or hide a real one.
    assertThat(status.is4xxClientError()).isEqualTo(category.isCallerFault());
    assertThat(status.is5xxServerError()).isEqualTo(category.isServerFault());
    assertThat(category.isCallerFault()).isNotEqualTo(category.isServerFault());
  }
}
