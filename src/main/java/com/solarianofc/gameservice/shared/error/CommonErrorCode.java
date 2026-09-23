package com.solarianofc.gameservice.shared.error;

/** Error codes shared by all modules (D-151). */
public enum CommonErrorCode implements ErrorCode {
    VALIDATION_ERROR,
    MALFORMED_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    CONFLICT,
    RATE_LIMITED,
    INTERNAL_ERROR;

    @Override
    public String code() {
        return name();
    }
}
