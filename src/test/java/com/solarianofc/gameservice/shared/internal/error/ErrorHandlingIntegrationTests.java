package com.solarianofc.gameservice.shared.internal.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.solarianofc.gameservice.IntegrationTest;
import com.solarianofc.gameservice.shared.error.ApiException;
import com.solarianofc.gameservice.shared.error.CommonErrorCode;
import com.solarianofc.gameservice.shared.error.ErrorCode;
import com.solarianofc.gameservice.shared.error.RateLimitedException;
import jakarta.servlet.Filter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Error responses of the main port as {@code application/problem+json} (D-150..D-162). A test controller raises each
 * kind of error behind a test chain that permits only its path; everything else goes through the production chains.
 * Own context (D-154, D-128), closed afterwards.
 */
@IntegrationTest
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class ErrorHandlingIntegrationTests {

    private static final String TEST_PATH = "/test/errors";

    private static final String BODY = "/body";

    private static final String PARAMS = "/params";

    private static final String RATE_LIMITED = "/rate-limited";

    private static final String RATE_LIMIT_DETAIL = "Too many sample requests";

    private static final String ADMIN_ONLY = "/admin-only";

    private static final String ACCESS_DENIED_IN_CONTROLLER = "/access-denied";

    private static final String FILTER_FAILURE = "/filter-failure";

    private static final String PLAYER = "player";

    private static final String PLAYER_PASSWORD = "player-password";

    /** Stands for internals (SQL, hosts, credentials) that must never reach the client. */
    private static final String SECRET = "connection to db-internal:5432 refused for user gameservice";

    private static final String PROBLEM_TYPE_BASE = "https://gameservice.local/problems/";

    private static final String TRACE_ID_PATTERN = "[0-9a-f]{32}";

    @LocalServerPort
    private int serverPort;

    @Autowired
    private JsonMapper jsonMapper;

    private RestTestClient client;

    @BeforeEach
    void bindClient() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + serverPort)
                .build();
    }

    @Test
    void invalidRequestBodyIsAValidationProblemWithTheInvalidFields() {
        String path = TEST_PATH + BODY;

        Problem problem = post(path, """
                {"username": "ab", "items": [{"type": ""}]}
                """);

        assertThat(problem)
                .isEqualTo(validationProblem(
                        path,
                        Map.of("username", "size must be between 3 and 20", "items[0].type", "must not be blank")));
    }

    @Test
    void bindingFailureKeepsTheFieldButNotTheConversionText() {
        String path = TEST_PATH + "/model";

        // D-169: Spring's text would name Java types and echo "abc".
        assertThat(get(path + "?page=abc")).isEqualTo(validationProblem(path, Map.of("page", "Invalid value")));
    }

    @Test
    void invalidQueryParameterIsAValidationProblemWithTheParameterName() {
        String path = TEST_PATH + PARAMS;

        // The client's language does not change the messages (D-164).
        Problem problem = Problem.of(
                client.get()
                        .uri(path + "?limit=0")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru")
                        .exchange()
                        .expectBody(String.class)
                        .returnResult(),
                jsonMapper);

        assertThat(problem).isEqualTo(validationProblem(path, Map.of("limit", "must be greater than or equal to 1")));
    }

    @Test
    void unreadableJsonBodyIsAMalformedRequest() {
        String path = TEST_PATH + BODY;

        assertMalformed(post(path, "{\"username\": "), path);
    }

    @Test
    void queryParameterOfTheWrongTypeIsAMalformedRequest() {
        String path = TEST_PATH + PARAMS;

        assertMalformed(get(path + "?limit=abc"), path);
    }

    @Test
    void missingRequiredQueryParameterIsAMalformedRequest() {
        String path = TEST_PATH + PARAMS;

        assertMalformed(get(path), path);
    }

    private static void assertMalformed(Problem problem, String path) {
        assertThat(problem)
                .isEqualTo(expected(400, "Bad Request", "MALFORMED_REQUEST", "Request could not be read", path));
    }

    @Test
    void unknownPathIsNotFound() {
        String path = TEST_PATH + "/missing";

        assertThat(get(path)).isEqualTo(expected(404, "Not Found", "NOT_FOUND", "Resource not found", path));
    }

    @Test
    void unsupportedMethodIsMethodNotAllowedWithTheAllowedMethods() {
        String path = TEST_PATH + PARAMS;

        EntityExchangeResult<String> result =
                client.delete().uri(path).exchange().expectBody(String.class).returnResult();

        assertThat(List.of(
                        Problem.of(result, jsonMapper),
                        result.getResponseHeaders().getAllow()))
                .containsExactly(
                        expected(
                                405,
                                "Method Not Allowed",
                                "METHOD_NOT_ALLOWED",
                                "Method is not supported for this resource",
                                path),
                        Set.of(HttpMethod.GET));
    }

    @Test
    void unsupportedContentTypeIsUnsupportedMediaType() {
        String path = TEST_PATH + BODY;

        EntityExchangeResult<String> result = client.post()
                .uri(path)
                .contentType(MediaType.TEXT_PLAIN)
                .body("username=abc")
                .exchange()
                .expectBody(String.class)
                .returnResult();

        assertThat(Problem.of(result, jsonMapper))
                .isEqualTo(expected(
                        415,
                        "Unsupported Media Type",
                        "UNSUPPORTED_MEDIA_TYPE",
                        "Content type is not supported",
                        path));
    }

    private static Problem validationProblem(String path, Map<String, String> errors) {
        return expected(400, "Bad Request", "VALIDATION_ERROR", "Request contains invalid fields", path, errors);
    }

    /** Expected problem without {@code errors[]}; {@code type} follows from the code (D-150). */
    private static Problem expected(int status, String title, String errorCode, String detail, String path) {
        return expected(status, title, errorCode, detail, path, Map.of());
    }

    private static Problem expected(
            int status, String title, String errorCode, String detail, String path, Map<String, String> errors) {
        return new Problem(
                status,
                MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                PROBLEM_TYPE_BASE + errorCode.toLowerCase(Locale.ROOT).replace('_', '-'),
                title,
                detail,
                path,
                errorCode,
                true,
                errors);
    }

    @Test
    void apiExceptionKeepsItsStatusCodeAndDetail() {
        String path = TEST_PATH + "/conflict";

        assertThat(get(path)).isEqualTo(expected(409, "Conflict", "CONFLICT", "Sample name is already taken", path));
    }

    @Test
    void moduleErrorCodeAndExtraPropertiesReachTheClient() {
        String path = TEST_PATH + "/business-rule";

        EntityExchangeResult<String> result = getResult(path);

        assertThat(List.of(Problem.of(result, jsonMapper), json(result).path("remainingAttempts")))
                .containsExactly(
                        expected(409, "Conflict", "SAMPLE_BUSINESS_RULE", "Sample rule violated", path),
                        jsonMapper.getNodeFactory().numberNode(0));
    }

    @Test
    void rateLimitWithAKnownWaitSetsRetryAfterRoundedUpToSeconds() {
        String path = TEST_PATH + RATE_LIMITED;

        EntityExchangeResult<String> result = getResult(path + "?retryAfterMillis=1500");

        assertThat(List.of(
                        Problem.of(result, jsonMapper),
                        String.valueOf(result.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER)),
                        json(result).path("retryAfterSeconds").asString("")))
                .containsExactly(expected(429, "Too Many Requests", "RATE_LIMITED", RATE_LIMIT_DETAIL, path), "2", "2");
    }

    @Test
    void rateLimitWithoutAKnownWaitHasNoRetryAfter() {
        String path = TEST_PATH + RATE_LIMITED;

        EntityExchangeResult<String> result = getResult(path);

        assertThat(List.of(
                        Problem.of(result, jsonMapper),
                        result.getResponseHeaders().containsHeader(HttpHeaders.RETRY_AFTER),
                        json(result).has("retryAfterSeconds")))
                .containsExactly(
                        expected(429, "Too Many Requests", "RATE_LIMITED", RATE_LIMIT_DETAIL, path), false, false);
    }

    /** D-156: generic body, full detail only in the ERROR log, linked by the trace ID. */
    @Test
    void unexpectedExceptionIsAnInternalErrorWithoutInternals(CapturedOutput output) {
        String path = TEST_PATH + "/unexpected";

        EntityExchangeResult<String> result = getResult(path);

        String traceId = json(result).path("traceId").asString("");
        String body = String.valueOf(result.getResponseBody());
        String log = output.getOut();
        boolean errorLineWithTraceId =
                log.lines().anyMatch(line -> line.contains("ERROR") && !traceId.isEmpty() && line.contains(traceId));
        boolean stackTraceLogged = log.contains(IllegalStateException.class.getName() + ": " + SECRET)
                && log.contains("\tat " + FailingController.class.getName());
        assertThat(List.of(
                        Problem.of(result, jsonMapper), body.contains(SECRET), errorLineWithTraceId, stackTraceLogged))
                .containsExactly(
                        expected(500, "Internal Server Error", "INTERNAL_ERROR", "An unexpected error occurred", path),
                        false,
                        true,
                        true);
    }

    /** D-166: no authentication -> 401 from the production chain's entry point. */
    @Test
    void anonymousRequestToADeniedPathIsUnauthorized() {
        String path = "/api/v1/anything";

        assertThat(get(path))
                .isEqualTo(expected(401, "Unauthorized", "UNAUTHORIZED", "Authentication is required", path));
    }

    @Test
    void authenticatedCallerWithoutTheRoleIsForbidden() {
        String path = TEST_PATH + ADMIN_ONLY;

        assertThat(getAsPlayer(path)).isEqualTo(expected(403, "Forbidden", "FORBIDDEN", "Access is denied", path));
    }

    /** The catch-all handler (D-156) must hand security exceptions back to Spring Security, not turn them into 500. */
    @Test
    void accessDeniedInsideTheControllerIsForbidden() {
        String path = TEST_PATH + ACCESS_DENIED_IN_CONTROLLER;

        assertThat(getAsPlayer(path)).isEqualTo(expected(403, "Forbidden", "FORBIDDEN", "Access is denied", path));
    }

    /** D-157: a failure before Spring MVC goes through the container's {@code /error} dispatch. */
    @Test
    void exceptionInAServletFilterIsAnInternalErrorProblem() {
        String path = TEST_PATH + FILTER_FAILURE;

        assertThat(get(path))
                .isEqualTo(
                        expected(500, "Internal Server Error", "INTERNAL_ERROR", "An unexpected error occurred", path));
    }

    /** Only the ERROR dispatch is permitted; the error path itself is not an open endpoint. */
    @Test
    void directRequestToTheErrorPathStaysDenied() {
        String path = "/error";

        assertThat(get(path))
                .isEqualTo(expected(401, "Unauthorized", "UNAUTHORIZED", "Authentication is required", path));
    }

    private Problem getAsPlayer(String uri) {
        return Problem.of(
                client.get()
                        .uri(uri)
                        .headers(headers -> headers.setBasicAuth(PLAYER, PLAYER_PASSWORD))
                        .exchange()
                        .expectBody(String.class)
                        .returnResult(),
                jsonMapper);
    }

    private Problem get(String uri) {
        return Problem.of(getResult(uri), jsonMapper);
    }

    private EntityExchangeResult<String> getResult(String uri) {
        return client.get().uri(uri).exchange().expectBody(String.class).returnResult();
    }

    private JsonNode json(EntityExchangeResult<String> result) {
        return jsonMapper.readTree(String.valueOf(result.getResponseBody()));
    }

    private Problem post(String path, String json) {
        EntityExchangeResult<String> result = client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange()
                .expectBody(String.class)
                .returnResult();
        return Problem.of(result, jsonMapper);
    }

    /**
     * Projection of a response onto the problem fields; one comparison per test. {@code traceIdPresent} is true for a
     * 32 hex digit {@code traceId}; {@code errors} maps {@code errors[].field} to {@code errors[].message}.
     */
    record Problem(
            int status,
            String contentType,
            String type,
            String title,
            String detail,
            String instance,
            String errorCode,
            boolean traceIdPresent,
            Map<String, String> errors) {

        static Problem of(EntityExchangeResult<String> result, JsonMapper jsonMapper) {
            String body = result.getResponseBody();
            JsonNode json = jsonMapper.readTree(body == null ? "{}" : body);
            MediaType contentType = result.getResponseHeaders().getContentType();
            return new Problem(
                    result.getStatus().value(),
                    contentType == null ? "" : contentType.toString(),
                    json.path("type").asString(""),
                    json.path("title").asString(""),
                    json.path("detail").asString(""),
                    json.path("instance").asString(""),
                    json.path("errorCode").asString(""),
                    json.path("traceId").asString("").matches(TRACE_ID_PATTERN),
                    json.path("errors")
                            .valueStream()
                            .collect(Collectors.toMap(
                                    error -> error.path("field").asString(""),
                                    error -> error.path("message").asString(""))));
        }
    }

    record SampleRequest(
            @NotBlank @Size(min = 3, max = 20) String username,
            @Valid List<SampleItem> items) {}

    record SampleItem(@NotBlank String type) {}

    record SampleQuery(@Nullable Integer page) {}

    @RestController
    @RequestMapping(TEST_PATH)
    static class FailingController {

        @PostMapping(BODY)
        @SuppressWarnings("unused")
        HttpStatus body(@Valid @RequestBody SampleRequest request) {
            return HttpStatus.OK;
        }

        @GetMapping("/model")
        @SuppressWarnings("unused")
        HttpStatus model(@ModelAttribute SampleQuery query) {
            return HttpStatus.OK;
        }

        @GetMapping(PARAMS)
        @SuppressWarnings("unused")
        // The Java name differs from the request parameter: errors[].field must be the client-facing name.
        HttpStatus params(@RequestParam("limit") @Min(1) int pageSize) {
            return HttpStatus.OK;
        }

        @GetMapping("/conflict")
        HttpStatus conflict() {
            throw new SampleConflictException();
        }

        @GetMapping("/business-rule")
        HttpStatus businessRule() {
            throw new SampleRuleException();
        }

        @GetMapping(ADMIN_ONLY)
        HttpStatus adminOnly() {
            return HttpStatus.OK;
        }

        /** Stands for method security (e.g. {@code @PreAuthorize}) rejecting inside Spring MVC. */
        @GetMapping(ACCESS_DENIED_IN_CONTROLLER)
        HttpStatus accessDeniedInController() {
            throw new AccessDeniedException("Sample access denied");
        }

        @GetMapping("/unexpected")
        HttpStatus unexpected() {
            throw new IllegalStateException(SECRET);
        }

        @GetMapping(RATE_LIMITED)
        HttpStatus rateLimited(@RequestParam(required = false) @Nullable Long retryAfterMillis) {
            throw retryAfterMillis == null
                    ? new RateLimitedException(RATE_LIMIT_DETAIL)
                    : new RateLimitedException(RATE_LIMIT_DETAIL, Duration.ofMillis(retryAfterMillis));
        }
    }

    /** A module's own error codes (D-158). */
    enum SampleErrorCode implements ErrorCode {
        SAMPLE_BUSINESS_RULE;

        @Override
        public String code() {
            return name();
        }
    }

    static class SampleConflictException extends ApiException {

        private static final long serialVersionUID = 1L;

        SampleConflictException() {
            super(HttpStatus.CONFLICT, CommonErrorCode.CONFLICT, "Sample name is already taken");
        }
    }

    static class SampleRuleException extends ApiException {

        private static final long serialVersionUID = 1L;

        SampleRuleException() {
            super(HttpStatus.CONFLICT, SampleErrorCode.SAMPLE_BUSINESS_RULE, "Sample rule violated");
        }

        @Override
        public Map<String, Object> getProperties() {
            return Map.of("remainingAttempts", 0);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingEndpointsConfiguration {

        @Bean
        FailingController failingController() {
            return new FailingController();
        }

        /**
         * Precedes the production chains and covers only the test controller's path: HTTP Basic for the in-memory
         * player, {@code ADMIN_ONLY} needs a role the player lacks, everything else is open.
         */
        @Bean
        @Order(0)
        SecurityFilterChain testErrorsSecurityFilterChain(
                HttpSecurity http,
                AuthenticationEntryPoint authenticationEntryPoint,
                AccessDeniedHandler accessDeniedHandler) {
            return http.securityMatcher(TEST_PATH + "/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .httpBasic(Customizer.withDefaults())
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(requests -> requests.requestMatchers(TEST_PATH + ADMIN_ONLY)
                            .hasRole("ADMIN")
                            .anyRequest()
                            .permitAll())
                    .build();
        }

        /** Fails before any controller, like a broken filter in production. */
        @Bean
        FilterRegistrationBean<Filter> failingFilter() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>((request, response, chain) -> {
                throw new IllegalStateException(SECRET);
            });
            registration.addUrlPatterns(TEST_PATH + FILTER_FAILURE);
            return registration;
        }

        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(User.withUsername(PLAYER)
                    .password("{noop}" + PLAYER_PASSWORD)
                    .roles("PLAYER")
                    .build());
        }
    }
}
