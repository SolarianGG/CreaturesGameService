package com.solarianofc.gameservice.shared.internal.openapi;

import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Anonymous access to the OpenAPI document and Swagger UI, only where springdoc is enabled (the {@code local} profile);
 * elsewhere these paths fall to the application chain and answer 401 (D-56, D-174).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("springdoc.api-docs.enabled")
class OpenApiSecurityConfiguration {

    /**
     * After the management chain (1), before the catch-all application chain (D-190). The paths come from the springdoc
     * settings, so a changed path keeps its access rule (D-191).
     */
    @Bean
    @Order(2)
    SecurityFilterChain openApiSecurityFilterChain(
            HttpSecurity http,
            SpringDocConfigProperties springDoc,
            ObjectProvider<SwaggerUiConfigProperties> swaggerUi) {
        String apiDocs = springDoc.getApiDocs().getPath();
        List<String> paths = new ArrayList<>(List.of(apiDocs, apiDocs + "/**", apiDocs + ".yaml"));
        swaggerUi.ifAvailable(ui -> paths.addAll(List.of(ui.getPath(), "/swagger-ui/**")));
        return http.securityMatcher(paths.toArray(String[]::new))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }
}
