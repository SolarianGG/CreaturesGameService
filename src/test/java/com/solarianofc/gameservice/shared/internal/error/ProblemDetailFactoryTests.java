package com.solarianofc.gameservice.shared.internal.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solarianofc.gameservice.shared.error.CommonErrorCode;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

class ProblemDetailFactoryTests {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Test
    void problemCarriesTypeTitleCodeInstanceAndTheCurrentTraceId() {
        ProblemDetailFactory factory = new ProblemDetailFactory(tracerWithCurrentTrace(TRACE_ID));

        ProblemDetail problem = factory.create(
                HttpStatus.BAD_REQUEST,
                CommonErrorCode.VALIDATION_ERROR,
                "Request contains invalid fields",
                "/api/v1/auth/register");

        assertThat(Arrays.asList(
                        problem.getType(),
                        problem.getTitle(),
                        problem.getStatus(),
                        problem.getDetail(),
                        problem.getInstance(),
                        problem.getProperties()))
                .isEqualTo(List.of(
                        URI.create("https://gameservice.local/problems/validation-error"),
                        "Bad Request",
                        400,
                        "Request contains invalid fields",
                        URI.create("/api/v1/auth/register"),
                        Map.of("errorCode", "VALIDATION_ERROR", "traceId", TRACE_ID)));
    }

    /** D-155: without a current span the field is omitted, not null. */
    @Test
    void problemWithoutACurrentSpanHasNoTraceId() {
        ProblemDetailFactory factory = new ProblemDetailFactory(Tracer.NOOP);

        ProblemDetail problem = factory.forStatus(HttpStatus.NOT_FOUND, null, "/any");

        assertThat(problem.getProperties()).containsOnlyKeys("errorCode");
    }

    /** D-165: the status picks the common code; the D-162 text replaces the thrown detail except for 409 / 429. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            400 | MALFORMED_REQUEST      | Request could not be read
            401 | UNAUTHORIZED           | Authentication is required
            403 | FORBIDDEN              | Access is denied
            404 | NOT_FOUND              | Resource not found
            405 | METHOD_NOT_ALLOWED     | Method is not supported for this resource
            406 | MALFORMED_REQUEST      | Request could not be read
            409 | CONFLICT               | thrown detail
            413 | MALFORMED_REQUEST      | Request could not be read
            415 | UNSUPPORTED_MEDIA_TYPE | Content type is not supported
            429 | RATE_LIMITED           | thrown detail
            500 | INTERNAL_ERROR         | An unexpected error occurred
            503 | INTERNAL_ERROR         | An unexpected error occurred
            """)
    void statusSelectsTheCommonCodeAndDetail(int status, String errorCode, String detail) {
        ProblemDetailFactory factory = new ProblemDetailFactory(Tracer.NOOP);

        ProblemDetail problem = factory.forStatus(HttpStatusCode.valueOf(status), "thrown detail", "/any");

        assertThat(List.of(problem.getProperties().get("errorCode"), problem.getDetail()))
                .containsExactly(errorCode, detail);
    }

    private static Tracer tracerWithCurrentTrace(String traceId) {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn(traceId);
        return tracer;
    }
}
