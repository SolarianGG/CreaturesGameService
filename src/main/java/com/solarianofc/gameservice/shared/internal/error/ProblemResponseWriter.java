package com.solarianofc.gameservice.shared.internal.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes a status problem where Spring MVC does not run, e.g. in the security filter chains (D-153). The converter is
 * built from a {@code JsonMapper.Builder} because only that constructor registers Spring's {@code ProblemDetail}
 * mixin, which puts {@code errorCode} and {@code traceId} at the top level.
 */
@Component
public class ProblemResponseWriter {

    private final ProblemDetailFactory problems;

    private final JacksonJsonHttpMessageConverter converter;

    ProblemResponseWriter(ProblemDetailFactory problems, JsonMapper jsonMapper) {
        this.problems = problems;
        this.converter = new JacksonJsonHttpMessageConverter(jsonMapper.rebuild());
    }

    public void write(HttpStatus status, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail problem = problems.forStatus(status, null, request.getRequestURI());
        try (ServletServerHttpResponse output = new ServletServerHttpResponse(response)) {
            output.setStatusCode(status);
            converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, output);
        }
    }
}
