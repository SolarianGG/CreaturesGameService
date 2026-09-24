package com.solarianofc.gameservice.shared.internal.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.solarianofc.gameservice.IntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

/** Outside the {@code local} profile springdoc is off and its paths fall to the application chain (D-56, D-174). */
@IntegrationTest
class OpenApiDisabledIntegrationTests {

    private static final List<String> DOCS_PATHS =
            List.of("/v3/api-docs", "/v3/api-docs.yaml", "/swagger-ui/index.html", "/swagger-ui.html");

    @LocalServerPort
    private int serverPort;

    @Autowired
    private ApplicationContext context;

    @Test
    void springdocServesNeitherTheDocumentNorSwaggerUi() {
        assertThat(List.of(
                        context.getBeanNamesForType(OpenApiResource.class).length,
                        context.getBeanNamesForType(SwaggerWelcomeCommon.class).length))
                .containsOnly(0);
    }

    @Test
    void docsPathsAnswerUnauthorizedProblems() {
        assertThat(DOCS_PATHS.stream().collect(Collectors.toMap(Function.identity(), this::answer)))
                .isEqualTo(DOCS_PATHS.stream()
                        .collect(Collectors.toMap(
                                Function.identity(), path -> "401 application/problem+json UNAUTHORIZED")));
    }

    /** Status, content type and {@code errorCode} of an anonymous GET. */
    private String answer(String path) {
        EntityExchangeResult<String> result = OpenApiDocuments.client(serverPort)
                .get()
                .uri(path)
                .exchange()
                .expectBody(String.class)
                .returnResult();
        Map<String, Object> problem = JsonPath.read(result.getResponseBody(), "$");
        return result.getStatus().value() + " " + result.getResponseHeaders().getContentType() + " "
                + problem.get("errorCode");
    }
}
