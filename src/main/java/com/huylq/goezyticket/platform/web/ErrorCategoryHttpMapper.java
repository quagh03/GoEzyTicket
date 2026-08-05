package com.huylq.goezyticket.platform.web;

import com.huylq.goezyticket.platform.error.ErrorCategory;
import org.springframework.http.HttpStatus;

public final class ErrorCategoryHttpMapper {

  private ErrorCategoryHttpMapper() {
  }

  public static HttpStatus toHttpStatus(ErrorCategory category) {
    return switch (category) {
      // 422
      case VALIDATION -> HttpStatus.UNPROCESSABLE_ENTITY;
      // 400
      case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
      // 401
      case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
      // 403
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      // 404
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      // 405
      case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
      // 409
      case CONFLICT -> HttpStatus.CONFLICT;
      // 415
      case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      // 429
      case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
      // 500
      case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
      // 503
      case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      // 504
      case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
    };
  }

}
