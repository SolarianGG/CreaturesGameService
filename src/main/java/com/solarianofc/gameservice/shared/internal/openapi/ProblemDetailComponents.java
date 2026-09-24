package com.solarianofc.gameservice.shared.internal.openapi;

import com.solarianofc.gameservice.shared.error.CommonErrorCode;
import com.solarianofc.gameservice.shared.error.RateLimitedException;
import com.solarianofc.gameservice.shared.internal.error.ProblemDetailFactory;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * The shared error part of the OpenAPI document (D-175): the {@code ProblemDetail} schema, one response per error
 * status (D-185) and one example per common code (D-181, D-186). Endpoints reference them through {@code $ref}.
 */
final class ProblemDetailComponents {

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String PROBLEM_DETAIL = "ProblemDetail";

    private static final String INVALID_FIELD = "InvalidField";

    private static final String SCHEMAS = "#/components/schemas/";

    private static final String EXAMPLES = "#/components/examples/";

    private static final String EXAMPLE_INSTANCE = "/api/v1/example";

    private static final String EXAMPLE_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    private static final String RETRY_AFTER_SECONDS = "retryAfterSeconds";

    /** Response name, status, codes (their examples, in this order) and headers. */
    private record ErrorResponse(
            String name, HttpStatus status, List<CommonErrorCode> codes, Map<String, Header> headers) {

        ErrorResponse(String name, HttpStatus status, CommonErrorCode... codes) {
            this(name, status, List.of(codes), Map.of());
        }
    }

    private static final List<ErrorResponse> RESPONSES = List.of(
            new ErrorResponse(
                    "BadRequest",
                    HttpStatus.BAD_REQUEST,
                    CommonErrorCode.VALIDATION_ERROR,
                    CommonErrorCode.MALFORMED_REQUEST),
            new ErrorResponse("Unauthorized", HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED),
            new ErrorResponse("Forbidden", HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN),
            new ErrorResponse("NotFound", HttpStatus.NOT_FOUND, CommonErrorCode.NOT_FOUND),
            new ErrorResponse(
                    "MethodNotAllowed",
                    HttpStatus.METHOD_NOT_ALLOWED,
                    List.of(CommonErrorCode.METHOD_NOT_ALLOWED),
                    Map.of(
                            HttpHeaders.ALLOW,
                            new Header()
                                    .description("Methods supported by the resource, comma-separated")
                                    .schema(new StringSchema()))),
            new ErrorResponse("Conflict", HttpStatus.CONFLICT, CommonErrorCode.CONFLICT),
            new ErrorResponse(
                    "UnsupportedMediaType", HttpStatus.UNSUPPORTED_MEDIA_TYPE, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE),
            new ErrorResponse(
                    "TooManyRequests",
                    HttpStatus.TOO_MANY_REQUESTS,
                    List.of(CommonErrorCode.RATE_LIMITED),
                    Map.of(
                            HttpHeaders.RETRY_AFTER,
                            new Header()
                                    .description("Seconds to wait before retrying; sent only when the wait is known")
                                    .schema(new IntegerSchema().format("int64")))),
            new ErrorResponse("InternalServerError", HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_ERROR));

    private ProblemDetailComponents() {}

    static Components addTo(Components components, ProblemDetailFactory factory) {
        components.addSchemas(PROBLEM_DETAIL, problemDetailSchema());
        components.addSchemas(INVALID_FIELD, invalidFieldSchema());
        for (ErrorResponse response : RESPONSES) {
            components.addResponses(response.name(), response(response));
            for (CommonErrorCode code : response.codes()) {
                components.addExamples(code.code(), example(factory, response.status(), code));
            }
        }
        return components;
    }

    private static Schema<?> problemDetailSchema() {
        String commonCodes = codeList(List.of(CommonErrorCode.values()));
        return new ObjectSchema()
                .description("RFC 9457 problem details of every error response")
                .addProperty(
                        "type",
                        new StringSchema()
                                .format("uri")
                                .description("URI of the problem type, derived from `errorCode`"))
                .addProperty("title", new StringSchema().description("HTTP reason phrase of the status"))
                .addProperty("status", new IntegerSchema().description("HTTP status code"))
                .addProperty("detail", new StringSchema().description("Human-readable explanation of this error"))
                .addProperty(
                        "instance",
                        new StringSchema().format("uri-reference").description("Path of the request that failed"))
                .addProperty(
                        "errorCode",
                        new StringSchema()
                                .pattern("^[A-Z][A-Z0-9_]*$")
                                .description("Machine-readable error code. Common codes: " + commonCodes
                                        + "; modules add their own, listed in the responses of each operation"))
                .addProperty(
                        "traceId",
                        new StringSchema()
                                .pattern("^[0-9a-f]{32}$")
                                .description("Trace ID of the request; absent when no trace is active"))
                .addProperty(
                        "errors",
                        new ArraySchema()
                                .items(new Schema<>().$ref(SCHEMAS + INVALID_FIELD))
                                .description("Invalid fields and parameters; only with `VALIDATION_ERROR`"))
                .addProperty(
                        RETRY_AFTER_SECONDS,
                        new IntegerSchema()
                                .format("int64")
                                .description("Seconds to wait before retrying; only with `RATE_LIMITED` when the wait "
                                        + "is known"))
                .required(List.of("type", "title", "status", "detail", "instance", "errorCode"));
    }

    private static Schema<?> invalidFieldSchema() {
        return new ObjectSchema()
                .description("One invalid field or parameter")
                .addProperty(
                        "field",
                        new StringSchema()
                                .description("Path of the field or name of the parameter, e.g. `items[0].type`"))
                .addProperty("message", new StringSchema().description("Why the value was rejected"))
                .required(List.of("field", "message"));
    }

    private static ApiResponse response(ErrorResponse response) {
        MediaType mediaType = new MediaType().schema(new Schema<>().$ref(SCHEMAS + PROBLEM_DETAIL));
        for (CommonErrorCode code : response.codes()) {
            mediaType.addExamples(code.code(), new Example().$ref(EXAMPLES + code.code()));
        }
        ApiResponse apiResponse = new ApiResponse()
                .description(response.status().getReasonPhrase() + "; `errorCode`: " + codeList(response.codes()))
                .content(new Content().addMediaType(PROBLEM_JSON, mediaType));
        response.headers().forEach(apiResponse::addHeaderObject);
        return apiResponse;
    }

    /**
     * The problem the server sends for {@code code}: the factory's fields and properties plus the extensions of the
     * real sources (D-186); only {@code traceId} is set here, as no span exists while the document is built. The
     * document's keys are sorted when written (D-189).
     */
    private static Example example(ProblemDetailFactory factory, HttpStatus status, CommonErrorCode code) {
        String thrownDetail = null;
        Map<String, Object> extensions = Map.of();
        switch (code) {
            case VALIDATION_ERROR ->
                extensions = Map.of(
                        "errors", List.of(Map.of("field", "username", "message", "size must be between 3 and 32")));
            case CONFLICT -> thrownDetail = "Resource is in a conflicting state";
            case RATE_LIMITED -> {
                RateLimitedException exception = new RateLimitedException("Too many requests", Duration.ofSeconds(30));
                thrownDetail = exception.getMessage();
                extensions = exception.getProperties();
            }
            default -> {
                // The factory's detail text, no extensions.
            }
        }
        ProblemDetail problem = factory.forCode(status, code, thrownDetail, EXAMPLE_INSTANCE);
        Map<String, Object> value = new HashMap<>();
        value.put("type", String.valueOf(problem.getType()));
        value.put("title", problem.getTitle());
        value.put("status", problem.getStatus());
        value.put("detail", problem.getDetail());
        value.put("instance", String.valueOf(problem.getInstance()));
        Map<String, Object> properties = problem.getProperties();
        if (properties != null) {
            value.putAll(properties);
        }
        value.put("traceId", EXAMPLE_TRACE_ID);
        value.putAll(extensions);
        return new Example().value(value);
    }

    private static String codeList(Collection<CommonErrorCode> codes) {
        return codes.stream().map(code -> "`" + code.code() + "`").collect(Collectors.joining(", "));
    }
}
