package com.huylq.goezyticket.platform.error;

import java.util.Map;

public class ValidationException extends DomainException {

  private static final ErrorCategory EXPECTED_ERROR_CATEGORY = ErrorCategory.VALIDATION;

  public ValidationException(ErrorCode errorCode) {
    this(errorCode, null, null, null);
  }

  public ValidationException(ErrorCode errorCode, Map<String, Object> details) {
    this(errorCode, null, details, null);
  }

  public ValidationException(ErrorCode errorCode, String message, Map<String, Object> details) {
    this(errorCode, message, details, null);
  }

  public ValidationException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, null, null, cause);
  }

  public ValidationException(
      ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
    super(requireCategory(errorCode, EXPECTED_ERROR_CATEGORY), message, details, cause);
  }

}
