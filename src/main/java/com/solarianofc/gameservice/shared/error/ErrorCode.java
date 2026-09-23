package com.solarianofc.gameservice.shared.error;

/**
 * Machine-readable error code, sent as the {@code errorCode} of a {@code ProblemDetail}. {@code shared} declares the
 * common codes in {@link CommonErrorCode}; each module declares its own enum (D-158).
 */
// Implemented by enums only, not meant as a lambda target (D-163).
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ErrorCode {

    /** The code in upper snake case, e.g. {@code VALIDATION_ERROR}. */
    String code();
}
