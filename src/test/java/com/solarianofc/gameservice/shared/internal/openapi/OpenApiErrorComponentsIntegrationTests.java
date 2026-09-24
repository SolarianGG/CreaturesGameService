package com.solarianofc.gameservice.shared.internal.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.solarianofc.gameservice.shared.error.CommonErrorCode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Shared {@code ProblemDetail} schema, responses and examples of the OpenAPI document (D-175, D-181..D-186). */
@SpringdocIntegrationTest
class OpenApiErrorComponentsIntegrationTests {

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String PROBLEM_SCHEMA = "#/components/schemas/ProblemDetail";

    private static final String INSTANCE = "/api/v1/example";

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    private static final String TYPE = "type";

    private static final String STRING = "string";

    private static final String ARRAY = "array";

    private static final String ERROR_CODE = "errorCode";

    @LocalServerPort
    private int serverPort;

    @Test
    void problemDetailPropertiesHaveTypesAndFormats() {
        Map<String, Map<String, Object>> properties = document().read("$.components.schemas.ProblemDetail.properties");

        assertThat(project(properties, OpenApiErrorComponentsIntegrationTests::typeOf))
                .isEqualTo(Map.of(
                        TYPE,
                        "string/uri",
                        "title",
                        STRING,
                        "status",
                        "integer/int32",
                        "detail",
                        STRING,
                        "instance",
                        "string/uri-reference",
                        ERROR_CODE,
                        STRING,
                        "traceId",
                        STRING,
                        "errors",
                        "array of #/components/schemas/InvalidField",
                        "retryAfterSeconds",
                        "integer/int64"));
    }

    @Test
    void problemDetailRequiresTheFieldsOfEveryProblem() {
        assertThat(document().<List<String>>read("$.components.schemas.ProblemDetail.required"))
                .containsExactlyInAnyOrder(TYPE, "title", "status", "detail", "instance", ERROR_CODE);
    }

    @Test
    void errorCodeAndTraceIdArePatterns() {
        Map<String, Map<String, Object>> properties = document().read("$.components.schemas.ProblemDetail.properties");

        assertThat(project(properties, property -> property.get("pattern")))
                .isEqualTo(Map.of(ERROR_CODE, "^[A-Z][A-Z0-9_]*$", "traceId", "^[0-9a-f]{32}$"));
    }

    @Test
    void invalidFieldHasARequiredFieldAndMessage() {
        DocumentContext document = document();
        Map<String, Map<String, Object>> properties = document.read("$.components.schemas.InvalidField.properties");

        assertThat(Map.of(
                        "properties",
                        project(properties, OpenApiErrorComponentsIntegrationTests::typeOf),
                        "required",
                        document.<List<String>>read("$.components.schemas.InvalidField.required")))
                .isEqualTo(Map.of(
                        "properties",
                        Map.of("field", STRING, "message", STRING),
                        "required",
                        List.of("field", "message")));
    }

    @Test
    void everySchemaPropertyIsDescribed() {
        DocumentContext document = document();
        Map<String, Map<String, Object>> problem = document.read("$.components.schemas.ProblemDetail.properties");
        Map<String, Map<String, Object>> invalidField = document.read("$.components.schemas.InvalidField.properties");

        assertThat(Stream.concat(problem.entrySet().stream(), invalidField.entrySet().stream())
                        .filter(property -> Objects.toString(property.getValue().get("description"), "")
                                .isBlank())
                        .map(Map.Entry::getKey))
                .isEmpty();
    }

    @Test
    void everyErrorResponseIsAProblemJsonDocument() {
        Map<String, Map<String, Object>> responses = document().read("$.components.responses");

        // The set of response names is pinned by everyErrorResponseReferencesTheExamplesOfItsCodes.
        assertThat(project(responses, response -> contentOf(response).keySet() + " " + schemaRefOf(response))
                        .values())
                .containsOnly("[" + PROBLEM_JSON + "] " + PROBLEM_SCHEMA);
    }

    @Test
    void everyErrorResponseReferencesTheExamplesOfItsCodes() {
        Map<String, Map<String, Object>> responses = document().read("$.components.responses");

        assertThat(project(responses, OpenApiErrorComponentsIntegrationTests::exampleRefsOf))
                .isEqualTo(Map.of(
                        // Keys are written sorted (D-189).
                        "BadRequest", List.of("MALFORMED_REQUEST", "VALIDATION_ERROR"),
                        "Unauthorized", List.of("UNAUTHORIZED"),
                        "Forbidden", List.of("FORBIDDEN"),
                        "NotFound", List.of("NOT_FOUND"),
                        "MethodNotAllowed", List.of("METHOD_NOT_ALLOWED"),
                        "Conflict", List.of("CONFLICT"),
                        "UnsupportedMediaType", List.of("UNSUPPORTED_MEDIA_TYPE"),
                        "TooManyRequests", List.of("RATE_LIMITED"),
                        "InternalServerError", List.of("INTERNAL_ERROR")));
    }

    @Test
    void allowAndRetryAfterAreDocumentedHeaders() {
        Map<String, Map<String, Object>> responses = document().read("$.components.responses");

        assertThat(project(responses, response -> headersOf(response).keySet().toString()))
                .containsEntry("MethodNotAllowed", "[Allow]")
                .containsEntry("TooManyRequests", "[Retry-After]")
                .containsEntry("BadRequest", "[]");
    }

    @Test
    void everyCommonErrorCodeHasANamedExample() {
        assertThat(document().<Map<String, Object>>read("$.components.examples").keySet())
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(CommonErrorCode.values())
                        .map(CommonErrorCode::code)
                        .toList());
    }

    @ParameterizedTest
    @MethodSource("examples")
    void exampleIsTheProblemTheServerSends(String code, Map<String, Object> expected) {
        assertThat(document().<Map<String, Object>>read("$.components.examples." + code + ".value"))
                .isEqualTo(expected);
    }

    static Stream<Arguments> examples() {
        return Stream.of(
                example(
                        "VALIDATION_ERROR",
                        400,
                        "Bad Request",
                        "Request contains invalid fields",
                        Map.of(
                                "errors",
                                List.of(Map.of("field", "username", "message", "size must be between 3 and 32")))),
                example("MALFORMED_REQUEST", 400, "Bad Request", "Request could not be read", Map.of()),
                example("UNAUTHORIZED", 401, "Unauthorized", "Authentication is required", Map.of()),
                example("FORBIDDEN", 403, "Forbidden", "Access is denied", Map.of()),
                example("NOT_FOUND", 404, "Not Found", "Resource not found", Map.of()),
                example(
                        "METHOD_NOT_ALLOWED",
                        405,
                        "Method Not Allowed",
                        "Method is not supported for this resource",
                        Map.of()),
                example("CONFLICT", 409, "Conflict", "Resource is in a conflicting state", Map.of()),
                example(
                        "UNSUPPORTED_MEDIA_TYPE",
                        415,
                        "Unsupported Media Type",
                        "Content type is not supported",
                        Map.of()),
                example("RATE_LIMITED", 429, "Too Many Requests", "Too many requests", Map.of("retryAfterSeconds", 30)),
                example("INTERNAL_ERROR", 500, "Internal Server Error", "An unexpected error occurred", Map.of()));
    }

    private static Arguments example(
            String code, int status, String title, String detail, Map<String, Object> extraProperties) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put(
                TYPE,
                "https://gameservice.local/problems/"
                        + code.toLowerCase(Locale.ROOT).replace('_', '-'));
        value.put("title", title);
        value.put("status", status);
        value.put("detail", detail);
        value.put("instance", INSTANCE);
        value.put(ERROR_CODE, code);
        value.put("traceId", TRACE_ID);
        value.putAll(extraProperties);
        return Arguments.of(code, value);
    }

    private DocumentContext document() {
        return OpenApiDocuments.fetch(serverPort);
    }

    private static Object typeOf(Map<String, Object> property) {
        Object type = property.get(TYPE);
        if (ARRAY.equals(type)) {
            return ARRAY + " of " + ((Map<?, ?>) property.get("items")).get("$ref");
        }
        Object format = property.get("format");
        return format == null ? type : type + "/" + format;
    }

    private static Map<String, Object> contentOf(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) response.get("content");
        return content;
    }

    private static Map<String, Object> mediaTypeOf(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> mediaType =
                (Map<String, Object>) contentOf(response).get(PROBLEM_JSON);
        return mediaType;
    }

    private static Object schemaRefOf(Map<String, Object> response) {
        return ((Map<?, ?>) mediaTypeOf(response).get("schema")).get("$ref");
    }

    private static List<String> exampleRefsOf(Map<String, Object> response) {
        return ((Map<?, ?>) mediaTypeOf(response).get("examples"))
                .values().stream()
                        .map(example -> String.valueOf(((Map<?, ?>) example).get("$ref")))
                        .map(ref -> ref.substring(ref.lastIndexOf('/') + 1))
                        .toList();
    }

    private static Map<String, Object> headersOf(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) response.getOrDefault("headers", Map.of());
        return headers;
    }

    private static <V> Map<String, Object> project(Map<String, V> entries, Function<V, Object> projection) {
        Map<String, Object> projected = new TreeMap<>();
        entries.forEach((name, value) -> {
            Object result = projection.apply(value);
            if (result != null) {
                projected.put(name, result);
            }
        });
        return projected;
    }
}
