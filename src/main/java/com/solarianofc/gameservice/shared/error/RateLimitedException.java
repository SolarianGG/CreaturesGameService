package com.solarianofc.gameservice.shared.error;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * 429 {@code RATE_LIMITED}. With a known wait the response carries {@code Retry-After} and {@code retryAfterSeconds}
 * (D-160).
 */
public class RateLimitedException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final @Nullable Long retryAfterSeconds;

    public RateLimitedException(String detail) {
        this(detail, null);
    }

    public RateLimitedException(String detail, @Nullable Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, CommonErrorCode.RATE_LIMITED, detail);
        this.retryAfterSeconds = retryAfter == null ? null : roundUpToSeconds(retryAfter);
    }

    /** The wait in whole seconds, rounded up so the client never retries too early; empty when unknown. */
    public Optional<Long> getRetryAfterSeconds() {
        return Optional.ofNullable(retryAfterSeconds);
    }

    @Override
    public Map<String, Object> getProperties() {
        return retryAfterSeconds == null ? Map.of() : Map.of("retryAfterSeconds", retryAfterSeconds);
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfterSeconds != null) {
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }
        return headers;
    }

    private static long roundUpToSeconds(Duration wait) {
        long seconds = wait.toSeconds();
        return wait.equals(Duration.ofSeconds(seconds)) ? seconds : seconds + 1;
    }
}
