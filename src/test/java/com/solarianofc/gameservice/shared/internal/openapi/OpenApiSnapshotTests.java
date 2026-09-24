package com.solarianofc.gameservice.shared.internal.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Snapshot sensor (D-55, D-173, D-179): the generated OpenAPI document must equal the committed
 * {@code docs/api/openapi.yaml}, so every contract change shows up in review. {@code ./gradlew updateOpenApiSnapshot}
 * runs this test with {@value #UPDATE_PROPERTY} and rewrites the file instead.
 */
@SpringdocIntegrationTest
class OpenApiSnapshotTests {

    static final String UPDATE_PROPERTY = "openapi.snapshot.update";

    private static final Path SNAPSHOT = Path.of("docs", "api", "openapi.yaml");

    @LocalServerPort
    private int serverPort;

    @Test
    void generatedDocumentEqualsTheCommittedSnapshot() throws IOException {
        String generated = withLf(OpenApiDocuments.client(serverPort)
                .get()
                .uri("/v3/api-docs.yaml")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, generated, StandardCharsets.UTF_8);
        }
        String committed = Files.exists(SNAPSHOT) ? withLf(Files.readString(SNAPSHOT, StandardCharsets.UTF_8)) : "";

        assertThat(generated)
                .as("The API contract changed; review the difference and run ./gradlew updateOpenApiSnapshot")
                .isEqualTo(committed);
    }

    private static String withLf(String text) {
        return text.replace("\r\n", "\n");
    }
}
