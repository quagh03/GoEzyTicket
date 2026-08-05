package com.huylq.goezyticket.platform.error;

public class ErrorContractViolationException extends RuntimeException {
  ErrorContractViolationException(String message) {
    super(message);
  }
}
