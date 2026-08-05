package com.huylq.goezyticket.platform.error;

import java.util.Map;

public class ForbiddenException extends DomainException {

  private static final ErrorCategory EXPECTED_ERROR_CATEGORY = ErrorCategory.FORBIDDEN;

  public ForbiddenException(ErrorCode errorCode) {
    this(errorCode, null, null, null);
  }

  public ForbiddenException(ErrorCode errorCode, Map<String, Object> details) {
    this(errorCode, null, details, null);
  }

  public ForbiddenException(ErrorCode errorCode, String message, Map<String, Object> details) {
    this(errorCode, message, details, null);
  }

  public ForbiddenException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, null, null, cause);
  }

  public ForbiddenException(
      ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
    super(requireCategory(errorCode, EXPECTED_ERROR_CATEGORY), message, details, cause);
  }

}
