package com.solarianofc.gameservice.shared.internal.security;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) {
        return http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll())
                .build();
    }
}
