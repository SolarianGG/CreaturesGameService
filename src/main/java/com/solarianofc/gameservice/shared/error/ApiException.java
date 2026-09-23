package com.solarianofc.gameservice.shared.error;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Base of every error a module reports to the client (D-152): the global handler answers with the status, the error
 * code and the detail, plus {@link #getProperties()} and {@link #getHeaders()}. The detail goes to the client, so it
 * must not contain internals. An expected client error, so no stack trace is recorded.
 */
public abstract class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    private final transient ErrorCode errorCode;

    private final String detail;

    protected ApiException(HttpStatus status, ErrorCode errorCode, String detail) {
        super(detail, null, false, false);
        this.status = status;
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** The client-facing {@code detail}; the same text as the exception message. */
    public String getDetail() {
        return detail;
    }

    /** Extra {@code ProblemDetail} properties, e.g. {@code retryAfterSeconds}; none by default. */
    public Map<String, Object> getProperties() {
        return Map.of();
    }

    /** Extra response headers, e.g. {@code Retry-After}; none by default. */
    public HttpHeaders getHeaders() {
        return new HttpHeaders();
    }
}
