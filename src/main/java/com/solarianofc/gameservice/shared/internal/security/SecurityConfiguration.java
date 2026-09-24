package com.solarianofc.gameservice.shared.internal.security;

import com.solarianofc.gameservice.shared.internal.error.ProblemResponseWriter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Security chains (D-124). Defining any chain switches off both Boot default chains, so the application chain is
 * declared too: it denies everything until the {@code account} module replaces it.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    /** Actuator: {@code health} (incl. probe groups) and {@code prometheus} are anonymous, the rest is denied. */
    @Bean
    @Order(1)
    SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) {
        return http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(requests -> requests.requestMatchers(EndpointRequest.to("health", "prometheus"))
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .build();
    }

    /** Rejections are {@code ProblemDetail}s: 401 without authentication, 403 without rights (D-153, D-166). */
    // Matches every request, so it comes after every specific chain (D-190).
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) {
        return http.exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // The container's error dispatch renders the problem of an already failed request (D-157); a direct
                // request to the error path is a REQUEST dispatch and stays denied.
                .authorizeHttpRequests(requests -> requests.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .build();
    }

    /** No or failed authentication: 401 {@code UNAUTHORIZED} problem (D-153, D-166). */
    @Bean
    AuthenticationEntryPoint problemAuthenticationEntryPoint(ProblemResponseWriter writer) {
        return (request, response, exception) -> writer.write(HttpStatus.UNAUTHORIZED, request, response);
    }

    /** Authenticated but not allowed: 403 {@code FORBIDDEN} problem (D-153, D-166). */
    @Bean
    AccessDeniedHandler problemAccessDeniedHandler(ProblemResponseWriter writer) {
        return (request, response, exception) -> writer.write(HttpStatus.FORBIDDEN, request, response);
    }
}
