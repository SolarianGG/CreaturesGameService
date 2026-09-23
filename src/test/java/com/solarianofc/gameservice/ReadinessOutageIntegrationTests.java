package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * A real RabbitMQ outage takes the application out of readiness but not out of liveness (D-125). Stopping a container
 * breaks its context, so this class runs in its own context and container set (D-128), closed afterwards.
 */
@IntegrationTest
@DirtiesContext
class ReadinessOutageIntegrationTests {

    private static final String READINESS = "readiness";

    private static final String LIVENESS = "liveness";

    private static final Probe UP = new Probe(200, "UP");

    private static final Probe DOWN = new Probe(503, "DOWN");

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private RabbitMQContainer rabbitMqContainer;

    @Autowired
    private JsonMapper jsonMapper;

    private RestTestClient management;

    @BeforeEach
    void bindClient() {
        management = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + managementPort)
                .build();
    }

    @Test
    void rabbitMqOutageTakesReadinessDownWhileLivenessStaysUp() {
        await().atMost(Duration.ofSeconds(10)).until(() -> UP.equals(probe(READINESS)));

        rabbitMqContainer.stop();

        // Every poll runs the whole readiness group (DB, Redis, RabbitMQ), so poll sparingly.
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> DOWN.equals(probe(READINESS)));
        assertThat(Map.of(READINESS, probe(READINESS), LIVENESS, probe(LIVENESS)))
                .containsOnly(entry(READINESS, DOWN), entry(LIVENESS, UP));
    }

    private Probe probe(String group) {
        EntityExchangeResult<String> result = management
                .get()
                .uri("/actuator/health/{group}", group)
                .exchange()
                .expectBody(String.class)
                .returnResult();
        String status =
                jsonMapper.readTree(result.getResponseBody()).path("status").asString();
        return new Probe(result.getStatus().value(), status);
    }

    /** HTTP status and health status of a probe response. */
    private record Probe(int httpStatus, String status) {}

    /**
     * Gives this class its own context key, hence its own containers (as D-114). Do not remove: without it the class
     * would join the shared {@code @IntegrationTest} context, stop its RabbitMQ and force every later class to rebuild
     * the shared context and containers.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class OwnContext {}
}
