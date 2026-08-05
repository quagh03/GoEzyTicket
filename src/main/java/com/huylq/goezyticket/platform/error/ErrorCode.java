package com.huylq.goezyticket.platform.error;

import java.io.Serializable;

/**
 * The shape of an error code. Every module declares its own implementation in {@code <module>/api};
 * platform never lists them.
 *
 * <p>Extends {@link Serializable} because a code is carried as a field of {@link DomainException},
 * and a {@link Throwable} is serializable by contract. This costs implementations nothing: they are
 * enums (R-03), and enums are serializable already.
 */
public interface ErrorCode extends Serializable {
  String code();

  ErrorCategory category();

  String defaultMessage();
}
