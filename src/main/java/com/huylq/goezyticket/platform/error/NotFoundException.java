package com.huylq.goezyticket.platform.error;

import java.util.Map;

public class NotFoundException extends DomainException {

  private static final ErrorCategory EXPECTED_ERROR_CATEGORY = ErrorCategory.NOT_FOUND;

  public NotFoundException(ErrorCode errorCode) {
    this(errorCode, null, null, null);
  }

  public NotFoundException(ErrorCode errorCode, Map<String, Object> details) {
    this(errorCode, null, details, null);
  }

  public NotFoundException(ErrorCode errorCode, String message, Map<String, Object> details) {
    this(errorCode, message, details, null);
  }

  public NotFoundException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, null, null, cause);
  }

  protected NotFoundException(
      ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
    super(requireCategory(errorCode, EXPECTED_ERROR_CATEGORY), message, details, cause);
  }

}
