package com.solarianofc.gameservice.shared.internal.error;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Tracer;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ProblemResponseWriterTests {

    /** Outside Spring MVC (security handlers): same body as the MVC path, {@code errorCode} as a top-level member. */
    @Test
    void writesTheStatusProblemAsProblemJson() throws IOException {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        ProblemResponseWriter writer = new ProblemResponseWriter(new ProblemDetailFactory(Tracer.NOOP), jsonMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(HttpStatus.UNAUTHORIZED, new MockHttpServletRequest("GET", "/api/v1/players/me"), response);

        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(List.of(
                        response.getStatus(),
                        String.valueOf(response.getContentType()),
                        body.path("errorCode").asString(""),
                        body.path("instance").asString("")))
                .containsExactly(401, "application/problem+json", "UNAUTHORIZED", "/api/v1/players/me");
    }
}
