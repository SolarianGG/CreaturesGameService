package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * ECS JSON log lines carry the trace and span IDs of the current observation (D-118, D-120). The test profile logs
 * plain text (D-119), so this class enables ECS for its own context only (D-128).
 */
@IntegrationTest
@DirtiesContext
@TestPropertySource(properties = "logging.structured.format.console=ecs")
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(StructuredLoggingIntegrationTests.class);

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void logLineInsideAnObservationIsEcsJsonWithTraceIds(CapturedOutput output) {
        String message = "structured-logging-probe " + UUID.randomUUID();

        TraceContext current = Observation.createNotStarted("structured.logging.probe", observationRegistry)
                .observe(() -> {
                    LOG.info(message);
                    return tracer.currentTraceContext().context();
                });

        JsonNode line = jsonMapper.readTree(output.getOut()
                .lines()
                .filter(candidate -> candidate.contains(message))
                .findFirst()
                .orElseThrow());

        assertThat(List.of(
                        line.path("message").asString(),
                        line.path("ecs").path("version").asString().isBlank(),
                        line.path("traceId").asString(),
                        line.path("spanId").asString()))
                .containsExactly(message, false, current.traceId(), current.spanId());
    }
}
