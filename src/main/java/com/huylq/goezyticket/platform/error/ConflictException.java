package com.huylq.goezyticket.platform.error;

import java.util.Map;

public class ConflictException extends DomainException {

  private static final ErrorCategory EXPECTED_ERROR_CATEGORY = ErrorCategory.CONFLICT;

  public ConflictException(ErrorCode errorCode) {
    this(errorCode, null, null, null);
  }

  public ConflictException(ErrorCode errorCode, Map<String, Object> details) {
    this(errorCode, null, details, null);
  }

  public ConflictException(ErrorCode errorCode, String message, Map<String, Object> details) {
    this(errorCode, message, details, null);
  }

  public ConflictException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, null, null, cause);
  }

  protected ConflictException(
      ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
    super(requireCategory(errorCode, EXPECTED_ERROR_CATEGORY), message, details, cause);
  }

}
