package com.huylq.goezyticket.platform.error;

import java.util.Map;

public class UnauthenticatedException extends DomainException {

  private static final ErrorCategory EXPECTED_ERROR_CATEGORY = ErrorCategory.UNAUTHENTICATED;

  public UnauthenticatedException(ErrorCode errorCode) {
    this(errorCode, null, null, null);
  }

  public UnauthenticatedException(ErrorCode errorCode, Map<String, Object> details) {
    this(errorCode, null, details, null);
  }

  public UnauthenticatedException(
      ErrorCode errorCode, String message, Map<String, Object> details) {
    this(errorCode, message, details, null);
  }

  public UnauthenticatedException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, null, null, cause);
  }

  public UnauthenticatedException(
      ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
    super(requireCategory(errorCode, EXPECTED_ERROR_CATEGORY), message, details, cause);
  }

}
