package com.solarianofc.gameservice.shared.internal.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.web.server.LocalServerPort;

/** The OpenAPI document with springdoc enabled as in the {@code local} profile (D-53, D-56). */
@SpringdocIntegrationTest
class OpenApiDocumentIntegrationTests {

    @LocalServerPort
    private int serverPort;

    @Test
    void apiDocsAnswerAnOpenApi3Document() {
        assertThat(document().<String>read("$.openapi")).startsWith("3.");
    }

    @ParameterizedTest
    // "/v3/api-docs" itself is fetched anonymously by every document() call.
    @ValueSource(strings = {"/v3/api-docs.yaml", "/swagger-ui/index.html"})
    void docsPathsAreServedAnonymously(String path) {
        assertOk(path);
    }

    @Test
    void infoNamesTheContractVersionAndTheErrorFormat() {
        assertThat(document().<Map<String, Object>>read("$.info"))
                .containsEntry("title", "GameService API")
                .containsEntry("version", "v1")
                .extractingByKey("description")
                .asString()
                .contains("application/problem+json");
    }

    @Test
    void theOnlyServerIsTheSameHost() {
        assertThat(document().<List<Map<String, Object>>>read("$.servers")).containsExactly(Map.of("url", "/"));
    }

    @Test
    void bearerAuthIsAJwtBearerToken() {
        assertThat(document().<Map<String, Object>>read("$.components.securitySchemes.bearerAuth"))
                .containsOnly(
                        Map.entry("type", "http"), Map.entry("scheme", "bearer"), Map.entry("bearerFormat", "JWT"));
    }

    @Test
    void clientCredentialsUseTheServiceTokenEndpointWithoutScopes() {
        assertThat(document().<Map<String, Object>>read("$.components.securitySchemes.clientCredentials"))
                .isEqualTo(Map.of(
                        "type",
                        "oauth2",
                        "flows",
                        Map.of(
                                "clientCredentials",
                                Map.of("tokenUrl", "/api/v1/auth/service-token", "scopes", Map.of()))));
    }

    @Test
    void theErrorDispatchIsNotPartOfTheContract() {
        assertThat(document().<Map<String, Object>>read("$.paths")).doesNotContainKey("/error");
    }

    @Test
    void noSecurityRequirementAppliesToTheWholeDocument() {
        assertThat(document().<Map<String, Object>>read("$")).doesNotContainKey("security");
    }

    private DocumentContext document() {
        return OpenApiDocuments.fetch(serverPort);
    }

    private void assertOk(String path) {
        OpenApiDocuments.client(serverPort)
                .get()
                .uri(path)
                .exchange()
                .expectStatus()
                .isOk();
    }
}
