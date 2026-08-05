package com.huylq.goezyticket.platform.error;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException implements Serializable {
  @Serial
  private static final long serialVersionUID = 1905122041950251207L;

  private final transient ErrorCode errorCode;
  private final transient Map<String, Object> details;

  protected DomainException(ErrorCode errorCode, String message,
                            Map<String, Object> details, Throwable cause) {
    super(message != null ? message : errorCode.defaultMessage(), cause);
    this.errorCode = errorCode;
    this.details = details == null ? Map.of() : Map.copyOf(details);
  }

  protected static ErrorCode requireCategory(ErrorCode errorCode, ErrorCategory expected) {
    Objects.requireNonNull(errorCode, "errorCode");
    if (errorCode.category() != expected) {
      throw new ErrorContractViolationException(
          "%s Invalid category %s, cannot use with %sException (request %s)"
              .formatted(errorCode.code(), errorCode.category(),
                  expected.name(), expected));
    }
    return errorCode;
  }

}
