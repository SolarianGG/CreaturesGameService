package com.solarianofc.gameservice.shared.internal.openapi;

import com.solarianofc.gameservice.shared.internal.error.ProblemDetailFactory;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The shared part of the OpenAPI document: info, server, security schemes and error components (D-175..D-177,
 * D-180). No root-level
 * security requirement; every operation declares its own (D-183).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("springdoc.api-docs.enabled")
class OpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";

    private static final String CLIENT_CREDENTIALS = "clientCredentials";

    private static final String DESCRIPTION = """
            Backend of a game service for a 1v1 game: accounts, matchmaking, match history and statistics, \
            leaderboard, real-time notifications and telemetry.

            Every error is an RFC 9457 `application/problem+json` document with an `errorCode`; the common codes \
            are listed in the `ProblemDetail` schema, the codes of an operation in its responses.""";

    @Bean
    OpenAPI gameServiceOpenApi(ProblemDetailFactory problemDetailFactory) {
        return new OpenAPI()
                .info(new Info().title("GameService API").version("v1").description(DESCRIPTION))
                // Fixed, so the generated document never contains the host and port of the running server.
                .servers(List.of(new Server().url("/")))
                .components(ProblemDetailComponents.addTo(securitySchemes(), problemDetailFactory));
    }

    private static Components securitySchemes() {
        return new Components()
                .addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                .addSecuritySchemes(
                        CLIENT_CREDENTIALS,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .clientCredentials(new OAuthFlow()
                                                .tokenUrl("/api/v1/auth/service-token")
                                                .scopes(new Scopes()))));
    }
}
