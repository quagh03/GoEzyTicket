package com.huylq.goezyticket.platform.error;

import static com.huylq.goezyticket.platform.error.ErrorCategory.BAD_REQUEST;
import static com.huylq.goezyticket.platform.error.ErrorCategory.CONFLICT;
import static com.huylq.goezyticket.platform.error.ErrorCategory.INTERNAL;
import static com.huylq.goezyticket.platform.error.ErrorCategory.NOT_FOUND;
import static com.huylq.goezyticket.platform.error.ErrorCategory.VALIDATION;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

  VALIDATION_FAILED("COMMON.VALIDATION_FAILED", VALIDATION, "Validation failed"),

  MALFORMED_REQUEST("COMMON.MALFORMED_REQUEST", BAD_REQUEST, "Request body is malformed"),

  INVALID_PARAMETER("COMMON.INVALID_PARAMETER", BAD_REQUEST, "Invalid parameter"),

  METHOD_NOT_ALLOWED(
      "COMMON.METHOD_NOT_ALLOWED", ErrorCategory.METHOD_NOT_ALLOWED, "Method not allowed"),

  UNSUPPORTED_MEDIA_TYPE(
      "COMMON.UNSUPPORTED_MEDIA_TYPE",
      ErrorCategory.UNSUPPORTED_MEDIA_TYPE,
      "Unsupported Content-Type"),

  ENDPOINT_NOT_FOUND("COMMON.ENDPOINT_NOT_FOUND", NOT_FOUND, "Endpoint not found"),

  UNAUTHENTICATED(
      "COMMON.UNAUTHENTICATED", ErrorCategory.UNAUTHENTICATED, "Authentication required"),

  FORBIDDEN("COMMON.FORBIDDEN", ErrorCategory.FORBIDDEN, "Access denied"),

  DATA_INTEGRITY("COMMON.DATA_INTEGRITY", CONFLICT, "Data integrity violation"),

  RATE_LIMITED("COMMON.RATE_LIMITED", ErrorCategory.RATE_LIMITED, "Rate limit exceeded"),

  INTERNAL_ERROR("COMMON.INTERNAL_ERROR", INTERNAL, "An internal server error occurred");

  private final String code;
  private final ErrorCategory category;
  private final String defaultMessage;

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
