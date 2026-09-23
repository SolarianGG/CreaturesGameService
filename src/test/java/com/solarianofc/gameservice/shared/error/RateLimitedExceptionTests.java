package com.solarianofc.gameservice.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;

class RateLimitedExceptionTests {

    private static final String DETAIL = "Too many requests";

    /** {@code Retry-After} takes whole seconds; a partial second rounds up so the client never retries too early. */
    @ParameterizedTest
    @CsvSource({"1, 1", "999, 1", "1000, 1", "1001, 2", "1500, 2", "60000, 60"})
    void knownWaitIsRoundedUpToWholeSeconds(long millis, long seconds) {
        RateLimitedException exception = new RateLimitedException(DETAIL, Duration.ofMillis(millis));

        assertThat(List.of(exception.getRetryAfterSeconds(), exception.getProperties()))
                .containsExactly(Optional.of(seconds), Map.of("retryAfterSeconds", seconds));
    }

    @Test
    void unknownWaitHasNoRetryAfter() {
        RateLimitedException exception = new RateLimitedException(DETAIL);

        assertThat(List.of(exception.getRetryAfterSeconds(), exception.getProperties(), exception.getHeaders()))
                .containsExactly(Optional.empty(), Map.of(), new HttpHeaders());
    }
}
