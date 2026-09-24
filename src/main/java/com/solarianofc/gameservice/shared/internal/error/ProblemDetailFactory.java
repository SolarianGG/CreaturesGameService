package com.solarianofc.gameservice.shared.internal.error;

import com.solarianofc.gameservice.shared.error.CommonErrorCode;
import com.solarianofc.gameservice.shared.error.ErrorCode;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.net.URI;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Builds every error body of the main port: {@code type} from the error code (D-150), {@code title} = reason phrase
 * (D-162), {@code errorCode}, and {@code traceId} of the current span, omitted without one (D-155).
 */
@Component
public class ProblemDetailFactory {

    private static final String TYPE_BASE = "https://gameservice.local/problems/";

    private final Tracer tracer;

    ProblemDetailFactory(Tracer tracer) {
        this.tracer = tracer;
    }

    ProblemDetail create(HttpStatusCode status, ErrorCode errorCode, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(
                URI.create(TYPE_BASE + errorCode.code().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(reasonPhrase(status));
        problem.setInstance(URI.create(instance));
        problem.setProperty("errorCode", errorCode.code());
        // A no-op span (tracing off) has an empty trace ID; it counts as no span.
        Span span = tracer.currentSpan();
        String traceId = span == null ? "" : span.context().traceId();
        if (!traceId.isEmpty()) {
            problem.setProperty("traceId", traceId);
        }
        return problem;
    }

    /**
     * Problem for an exception that only carries a status (D-165): the status picks the common code; the D-162 text
     * replaces the thrown detail, which is kept only for {@code CONFLICT} and {@code RATE_LIMITED}.
     */
    public ProblemDetail forStatus(HttpStatusCode status, @Nullable String thrownDetail, String instance) {
        return forCode(status, codeFor(status), thrownDetail, instance);
    }

    /**
     * Problem with a common code and its D-162 detail; codes without one take the thrown detail. Also builds the
     * OpenAPI error examples, so they match the real responses (D-186).
     */
    public ProblemDetail forCode(
            HttpStatusCode status, CommonErrorCode code, @Nullable String thrownDetail, String instance) {
        String detail = defaultDetail(code);
        if (detail == null) {
            detail = thrownDetail == null ? reasonPhrase(status) : thrownDetail;
        }
        return create(status, code, detail, instance);
    }

    private static @Nullable String defaultDetail(CommonErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR -> "Request contains invalid fields";
            case MALFORMED_REQUEST -> "Request could not be read";
            case UNAUTHORIZED -> "Authentication is required";
            case FORBIDDEN -> "Access is denied";
            case NOT_FOUND -> "Resource not found";
            case METHOD_NOT_ALLOWED -> "Method is not supported for this resource";
            case UNSUPPORTED_MEDIA_TYPE -> "Content type is not supported";
            case INTERNAL_ERROR -> "An unexpected error occurred";
            case CONFLICT, RATE_LIMITED -> null;
        };
    }

    private static CommonErrorCode codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> CommonErrorCode.UNAUTHORIZED;
            case 403 -> CommonErrorCode.FORBIDDEN;
            case 404 -> CommonErrorCode.NOT_FOUND;
            case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED;
            case 409 -> CommonErrorCode.CONFLICT;
            case 415 -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 429 -> CommonErrorCode.RATE_LIMITED;
            default -> status.is5xxServerError() ? CommonErrorCode.INTERNAL_ERROR : CommonErrorCode.MALFORMED_REQUEST;
        };
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known == null ? String.valueOf(status.value()) : known.getReasonPhrase();
    }
}
