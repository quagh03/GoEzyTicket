package com.huylq.goezyticket.platform.error;

import com.huylq.goezyticket.platform.web.ErrorCategoryHttpMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AC-07 and AC-08 — an exception carries the full error context, and a subclass refuses a code whose
 * category it cannot represent.
 *
 * <p>Pure unit test: no Spring context. The HTTP status is derived through
 * {@link ErrorCategoryHttpMapper} exactly the way the handler (US-00.6-01) will derive it, so the
 * "status comes from category(), never from the exception type" rule is already exercised here.
 */
@DisplayName("DomainException")
class DomainExceptionTest {

  /**
   * Stands in for {@code inventory/api/InventoryErrorCode} until T-04 lands. Declared here on
   * purpose: platform must never learn a business code, not even for its own tests.
   */
  private enum FixtureErrorCode implements ErrorCode {
    SEAT_ALREADY_HELD("INVENTORY.SEAT_ALREADY_HELD", ErrorCategory.CONFLICT, "Seat is already held");

    private final String code;
    private final ErrorCategory category;
    private final String defaultMessage;

    FixtureErrorCode(String code, ErrorCategory category, String defaultMessage) {
      this.code = code;
      this.category = category;
      this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
      return code;
    }

    @Override
    public ErrorCategory category() {
      return category;
    }

    @Override
    public String defaultMessage() {
      return defaultMessage;
    }
  }

  @Nested
  @DisplayName("carries enough context to render the error response")
  class CarriesContext {

    @Test
    @DisplayName("keeps the module's code, the details, and a 409 derived from the category")
    void keepsCodeDetailsAndCategoryDerivedStatus() {
      DomainException exception =
          new ConflictException(FixtureErrorCode.SEAT_ALREADY_HELD, Map.of("seatId", "A-12"));

      assertThat(exception.getErrorCode().code()).isEqualTo("INVENTORY.SEAT_ALREADY_HELD");
      assertThat(exception.getDetails()).containsExactly(Map.entry("seatId", "A-12"));
      assertThat(ErrorCategoryHttpMapper.toHttpStatus(exception.getErrorCode().category()))
          .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("the status follows the code's category, not the exception type")
    void statusFollowsTheCategoryNotTheType() {
      // Two different types, each mapped through its code's category — so the handler never needs
      // an instanceof chain over exception classes.
      DomainException forbidden = new ForbiddenException(CommonErrorCode.FORBIDDEN);
      DomainException unauthenticated = new UnauthenticatedException(CommonErrorCode.UNAUTHENTICATED);

      assertThat(ErrorCategoryHttpMapper.toHttpStatus(forbidden.getErrorCode().category()))
          .isEqualTo(HttpStatus.FORBIDDEN);
      assertThat(ErrorCategoryHttpMapper.toHttpStatus(unauthenticated.getErrorCode().category()))
          .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("falls back to the code's default message when none is given")
    void fallsBackToTheDefaultMessage() {
      DomainException exception = new ConflictException(FixtureErrorCode.SEAT_ALREADY_HELD);

      assertThat(exception.getMessage())
          .isEqualTo(FixtureErrorCode.SEAT_ALREADY_HELD.defaultMessage());
    }

    @Test
    @DisplayName("prefers an explicit message over the default")
    void prefersAnExplicitMessage() {
      DomainException exception =
          new ConflictException(
              FixtureErrorCode.SEAT_ALREADY_HELD, "Seat A-12 is held by someone else", Map.of());

      assertThat(exception.getMessage()).isEqualTo("Seat A-12 is held by someone else");
    }

    @Test
    @DisplayName("keeps the cause for the server-fault log path")
    void keepsTheCause() {
      RuntimeException cause = new RuntimeException("duplicate key");

      DomainException exception = new ConflictException(CommonErrorCode.DATA_INTEGRITY, cause);

      assertThat(exception).hasCause(cause);
      assertThat(exception.getMessage()).isEqualTo(CommonErrorCode.DATA_INTEGRITY.defaultMessage());
    }

    @Test
    @DisplayName("treats absent details as an empty map, never null")
    void treatsAbsentDetailsAsEmpty() {
      // The handler serialises details unconditionally; a null here would be an NPE per error.
      DomainException exception = new ConflictException(FixtureErrorCode.SEAT_ALREADY_HELD);

      assertThat(exception.getDetails()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("copies the details and hands out an unmodifiable view")
    void copiesTheDetails() {
      Map<String, Object> mutable = new HashMap<>(Map.of("seatId", "A-12"));

      DomainException exception = new ConflictException(FixtureErrorCode.SEAT_ALREADY_HELD, mutable);
      mutable.put("sneakedIn", "after the throw");

      Map<String, Object> details = exception.getDetails();

      assertThat(details).containsOnlyKeys("seatId");
      assertThatThrownBy(() -> details.put("alsoNotAllowed", "x"))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("validates the code's category at construction time")
  class ValidatesCategory {

    @ParameterizedTest(name = "{0} accepts a {1} code")
    @MethodSource("com.huylq.goezyticket.platform.error.DomainExceptionTest#subclasses")
    @DisplayName("accepts a code whose category it represents, on every constructor")
    void acceptsAMatchingCode(
        String label, ErrorCategory expected, List<Function<ErrorCode, DomainException>> constructors) {
      ErrorCode matching = commonCodeWithCategory(expected);

      assertThat(constructors).isNotEmpty();
      constructors.forEach(
          create ->
              assertThatCode(() -> create.apply(matching))
                  .describedAs("%s must accept %s", label, matching.code())
                  .doesNotThrowAnyException());
    }

    @ParameterizedTest(name = "{0} rejects a code that is not {1}")
    @MethodSource("com.huylq.goezyticket.platform.error.DomainExceptionTest#subclasses")
    @DisplayName("rejects a mismatched code from every constructor overload")
    void rejectsAMismatchedCodeFromEveryConstructor(
        String label, ErrorCategory expected, List<Function<ErrorCode, DomainException>> constructors) {
      // INTERNAL is no convenience subclass's category, so it is a mismatch for all of them.
      ErrorCode mismatched = CommonErrorCode.INTERNAL_ERROR;

      assertThat(constructors).isNotEmpty();
      constructors.forEach(
          create ->
              assertThatThrownBy(() -> create.apply(mismatched))
                  .describedAs("%s must reject %s", label, mismatched.code())
                  // AC-08 words this as IllegalArgumentException; the implementation throws the more
                  // specific ErrorContractViolationException (a plain RuntimeException today).
                  .isInstanceOf(ErrorContractViolationException.class)
                  // The message must name the offending code and both categories, so the fix is
                  // obvious from the stack trace alone.
                  .hasMessageContaining(mismatched.code())
                  .hasMessageContaining(mismatched.category().name())
                  .hasMessageContaining(expected.name()));
    }

    @Test
    @DisplayName("fails at the constructor, not at the point the response is rendered")
    void notFoundRejectsAConflictCode() {
      Map<String, Object> details = Map.of("seatId", "A-12");

      assertThatThrownBy(() -> new NotFoundException(FixtureErrorCode.SEAT_ALREADY_HELD, details))
          .isInstanceOf(ErrorContractViolationException.class)
          .hasMessageContaining("INVENTORY.SEAT_ALREADY_HELD")
          .hasMessageContaining("CONFLICT")
          .hasMessageContaining("NOT_FOUND");
    }

    @Test
    @DisplayName("rejects a null code instead of failing later inside defaultMessage()")
    void rejectsANullCode() {
      assertThatThrownBy(() -> new ConflictException(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("errorCode");
    }
  }

  /** Every convenience subclass, its category, and each public constructor it offers. */
  static Stream<Arguments> subclasses() {
    return Stream.of(
        Arguments.of(
            "ValidationException",
            ErrorCategory.VALIDATION,
            List.<Function<ErrorCode, DomainException>>of(
                ValidationException::new,
                code -> new ValidationException(code, Map.of("field", "email")),
                code -> new ValidationException(code, "explicit message", Map.of()),
                code -> new ValidationException(code, new RuntimeException("cause")))),
        Arguments.of(
            "UnauthenticatedException",
            ErrorCategory.UNAUTHENTICATED,
            List.<Function<ErrorCode, DomainException>>of(
                UnauthenticatedException::new,
                code -> new UnauthenticatedException(code, Map.of("scheme", "bearer")),
                code -> new UnauthenticatedException(code, "explicit message", Map.of()),
                code -> new UnauthenticatedException(code, new RuntimeException("cause")))),
        Arguments.of(
            "ForbiddenException",
            ErrorCategory.FORBIDDEN,
            List.<Function<ErrorCode, DomainException>>of(
                ForbiddenException::new,
                code -> new ForbiddenException(code, Map.of("role", "buyer")),
                code -> new ForbiddenException(code, "explicit message", Map.of()),
                code -> new ForbiddenException(code, new RuntimeException("cause")))),
        Arguments.of(
            "NotFoundException",
            ErrorCategory.NOT_FOUND,
            List.<Function<ErrorCode, DomainException>>of(
                NotFoundException::new,
                code -> new NotFoundException(code, Map.of("id", "42")),
                code -> new NotFoundException(code, "explicit message", Map.of()),
                code -> new NotFoundException(code, new RuntimeException("cause")))),
        Arguments.of(
            "ConflictException",
            ErrorCategory.CONFLICT,
            List.<Function<ErrorCode, DomainException>>of(
                ConflictException::new,
                code -> new ConflictException(code, Map.of("seatId", "A-12")),
                code -> new ConflictException(code, "explicit message", Map.of()),
                code -> new ConflictException(code, new RuntimeException("cause")))));
  }

  /** A COMMON code for each category the convenience subclasses cover. */
  private static ErrorCode commonCodeWithCategory(ErrorCategory category) {
    return Stream.of(CommonErrorCode.values())
        .filter(code -> code.category() == category)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no CommonErrorCode has category " + category));
  }
}
