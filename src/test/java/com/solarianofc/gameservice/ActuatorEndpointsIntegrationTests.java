package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Actuator on the separate management port (D-122, D-123) against the shared context (D-127, D-129). */
@IntegrationTest
class ActuatorEndpointsIntegrationTests {

    @LocalManagementPort
    private int managementPort;

    @LocalServerPort
    private int serverPort;

    @Autowired
    private HealthEndpoint healthEndpoint;

    private RestTestClient management;

    private RestTestClient main;

    @BeforeEach
    void bindClients() {
        management = client(managementPort);
        main = client(serverPort);
    }

    @Test
    void readinessGroupConsistsOfTheReadinessStateAndTheInfrastructure() {
        CompositeHealthDescriptor readiness = (CompositeHealthDescriptor) healthEndpoint.healthForPath("readiness");

        Map<String, Status> statuses = readiness.getComponents().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, component -> component.getValue().getStatus()));

        assertThat(statuses)
                .containsOnly(
                        entry("readinessState", Status.UP),
                        entry("db", Status.UP),
                        entry("redis", Status.UP),
                        entry("rabbit", Status.UP));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    void healthAndProbesAreUpOnTheManagementPortWithoutAuthentication(String path) {
        assertUp(management, path);
    }

    @Test
    void prometheusIsScrapableOnTheManagementPortWithoutAuthentication() {
        String body = management
                .get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).containsPattern("(?m)^jvm_memory_used_bytes\\{");
    }

    @Test
    void otherManagementPathsAreDenied() {
        assertDeniedByChain(management, "/actuator/env");
    }

    @Test
    void healthIsNotServedOnTheMainPort() {
        assertDeniedByChain(main, "/actuator/health");
    }

    @Test
    void mainPortRequestsAreDeniedByTheApplicationChain() {
        assertDeniedByChain(main, "/");
    }

    private static void assertUp(RestTestClient client, String path) {
        client.get()
                .uri(path)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP");
    }

    /** {@code denyAll} (D-124): 403 without an authentication challenge, unlike the Boot default chain (401 Basic). */
    private static void assertDeniedByChain(RestTestClient client, String path) {
        client.get()
                .uri(path)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectHeader()
                .doesNotExist(HttpHeaders.WWW_AUTHENTICATE);
    }

    private static RestTestClient client(int port) {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
