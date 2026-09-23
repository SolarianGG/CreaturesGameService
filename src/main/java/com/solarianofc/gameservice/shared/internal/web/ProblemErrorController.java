package com.solarianofc.gameservice.shared.internal.web;

import com.solarianofc.gameservice.shared.internal.error.ProblemDetailFactory;
import com.solarianofc.gameservice.shared.internal.error.UnexpectedErrorLog;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Replaces Boot's {@code BasicErrorController} (D-157, D-167): errors outside Spring MVC (filters, {@code sendError})
 * reach the container's error dispatch and are answered with the same {@code ProblemDetail} as everything else.
 */
@RestController
class ProblemErrorController implements ErrorController {

    private final ProblemDetailFactory problems;

    private final ErrorAttributes errorAttributes;

    ProblemErrorController(ProblemDetailFactory problems, ErrorAttributes errorAttributes) {
        this.problems = problems;
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("${spring.web.error.path:${error.path:/error}}")
    ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        HttpStatusCode status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer code
                ? HttpStatusCode.valueOf(code)
                : HttpStatus.INTERNAL_SERVER_ERROR;
        String path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String uri
                ? uri
                : request.getRequestURI();
        if (status.is5xxServerError()) {
            UnexpectedErrorLog.record(
                    request.getMethod(), path, errorAttributes.getError(new ServletWebRequest(request)));
        }
        return ResponseEntity.status(status).body(problems.forStatus(status, null, path));
    }
}
