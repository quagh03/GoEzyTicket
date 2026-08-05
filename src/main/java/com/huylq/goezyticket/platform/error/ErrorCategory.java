package com.huylq.goezyticket.platform.error;

public enum ErrorCategory {

  VALIDATION(Fault.CALLER),

  BAD_REQUEST(Fault.CALLER),

  UNAUTHENTICATED(Fault.CALLER),

  FORBIDDEN(Fault.CALLER),

  NOT_FOUND(Fault.CALLER),

  METHOD_NOT_ALLOWED(Fault.CALLER),

  CONFLICT(Fault.CALLER),

  UNSUPPORTED_MEDIA_TYPE(Fault.CALLER),

  RATE_LIMITED(Fault.CALLER),

  INTERNAL(Fault.SERVER),

  UNAVAILABLE(Fault.SERVER),

  TIMEOUT(Fault.SERVER);

  public enum Fault { CALLER, SERVER }

  private final Fault fault;

  ErrorCategory(Fault fault) {
    this.fault = fault;
  }

  public Fault fault() {
    return fault;
  }

  /** Client failure: log WARN, <strong>DO NOT PRINT</strong> stack trace. */
  public boolean isCallerFault() {
    return fault == Fault.CALLER;
  }

  /** Server failure: log ERROR, <strong>PRINT</strong> stack trace. */
  public boolean isServerFault() {
    return fault == Fault.SERVER;
  }

}
