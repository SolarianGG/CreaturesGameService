package com.solarianofc.gameservice.shared.internal.error;

import com.solarianofc.gameservice.shared.error.ApiException;
import com.solarianofc.gameservice.shared.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps Spring MVC exceptions to the {@code ProblemDetail} contract (D-150..D-166). */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ProblemDetailFactory problems;

    GlobalExceptionHandler(ProblemDetailFactory problems) {
        this.problems = problems;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), message(error)))
                .toList();
        return validationProblem(fields, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> fields = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new InvalidField(fieldName(error, result), message(error))))
                .toList();
        return validationProblem(fields, headers, status, request);
    }

    /** Module errors (D-152): status, code, detail, properties and headers as thrown (e.g. D-160). */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        ProblemDetail problem =
                problems.create(ex.getStatus(), ex.getErrorCode(), ex.getDetail(), request.getRequestURI());
        ex.getProperties().forEach(problem::setProperty);
        return ResponseEntity.status(ex.getStatus()).headers(ex.getHeaders()).body(problem);
    }

    /**
     * Security exceptions thrown inside Spring MVC (method security) go back to Spring Security, whose entry point and
     * access denied handler decide between 401 and 403 (D-153, D-166); rethrowing the same exception makes Spring
     * pass it on instead of treating it as a handler failure.
     */
    @ExceptionHandler(AccessDeniedException.class)
    void passAccessDeniedOn(AccessDeniedException ex) {
        throw ex;
    }

    /** See {@link #passAccessDeniedOn}. */
    @ExceptionHandler(AuthenticationException.class)
    void passAuthenticationFailureOn(AuthenticationException ex) {
        throw ex;
    }

    /** Anything else is a bug or an outage (D-156): full detail only in the log, a generic problem for the client. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        UnexpectedErrorLog.record(request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail problem = problems.forStatus(HttpStatus.INTERNAL_SERVER_ERROR, null, request.getRequestURI());
        return ResponseEntity.internalServerError().body(problem);
    }

    /** Every other exception handled by the base class; its status stays, the body becomes our problem. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        if (statusCode.is5xxServerError()) {
            UnexpectedErrorLog.record(servletRequest.getMethod(), servletRequest.getRequestURI(), ex);
        }
        String thrownDetail = body instanceof ProblemDetail thrown ? thrown.getDetail() : null;
        ProblemDetail problem = problems.forStatus(statusCode, thrownDetail, servletRequest.getRequestURI());
        return ResponseEntity.status(statusCode).headers(headers).body(problem);
    }

    private ResponseEntity<Object> validationProblem(
            List<InvalidField> fields, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ProblemDetail problem = problems.forCode(status, CommonErrorCode.VALIDATION_ERROR, null, path);
        problem.setProperty("errors", fields);
        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    /** A bean parameter keeps its field path; a plain parameter is named as the client sent it. */
    private static String fieldName(MessageSourceResolvable error, ParameterValidationResult result) {
        return error instanceof FieldError fieldError
                ? fieldError.getField()
                : parameterName(result.getMethodParameter());
    }

    /**
     * The {@code @RequestParam} / {@code @PathVariable} name, else the Java name. Handler method parameters synthesize
     * annotations, so {@code name()} already resolves its {@code value} alias.
     */
    private static String parameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.name().isEmpty()) {
            return requestParam.name();
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.name().isEmpty()) {
            return pathVariable.name();
        }
        return Objects.requireNonNullElse(parameter.getParameterName(), "");
    }

    /** Bean Validation message; a binding failure's text names Java types and echoes the input, so it is fixed (D-169). */
    private static String message(MessageSourceResolvable error) {
        if (error instanceof FieldError fieldError && fieldError.isBindingFailure()) {
            return "Invalid value";
        }
        return Objects.requireNonNullElse(error.getDefaultMessage(), "");
    }

    /** One entry of {@code errors[]} (D-159). */
    record InvalidField(String field, String message) {}
}
