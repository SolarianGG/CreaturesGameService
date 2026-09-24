package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

class ProfileConfigurationTests {

    private static final String CONSOLE_LOG_FORMAT = "logging.structured.format.console";

    private static final Map<String, String> LOCAL_CONNECTIONS = localConnections();

    private static final Map<String, String> SCHEMA_OWNERSHIP = schemaOwnership();

    private static final String API_DOCS_ENABLED = "springdoc.api-docs.enabled";

    private static final String SWAGGER_UI_ENABLED = "springdoc.swagger-ui.enabled";

    private static final String OFF = "false";

    private static final String ON = "true";

    /** D-56, D-175: the document and Swagger UI only in {@code local}; no responses derived from the advice. */
    private static final Map<String, String> SPRINGDOC_OFF =
            Map.of(API_DOCS_ENABLED, OFF, SWAGGER_UI_ENABLED, OFF, "springdoc.override-with-generic-response", OFF);

    private static final Map<String, String> SPRINGDOC_ON = Map.of(API_DOCS_ENABLED, ON, SWAGGER_UI_ENABLED, ON);

    @Test
    void localProfileConnectsToLocalhostServices() {
        StandardEnvironment environment = load("local");

        assertThat(propertiesOf(environment, LOCAL_CONNECTIONS)).containsExactlyEntriesOf(LOCAL_CONNECTIONS);
    }

    @Test
    void withoutProfileNoConnectionIsConfigured() {
        StandardEnvironment environment = load();

        assertThat(LOCAL_CONNECTIONS.keySet()).noneMatch(environment::containsProperty);
    }

    @Test
    void withoutProfileTheBaseConfigurationIsLoaded() {
        StandardEnvironment environment = load();

        assertThat(environment.getProperty("spring.application.name")).isEqualTo("GameService");
    }

    @Test
    void withoutProfileTheConsoleLogsEcsJson() {
        assertThat(load().getProperty(CONSOLE_LOG_FORMAT)).isEqualTo("ecs");
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "test"})
    void localAndTestProfilesLogPlainText(String profile) {
        // An empty format makes Boot fall back to the plain text pattern (D-119).
        assertThat(load(profile).getProperty(CONSOLE_LOG_FORMAT)).isEmpty();
    }

    @Test
    void baseConfigurationLeavesTheSchemaToFlyway() {
        StandardEnvironment environment = load();

        assertThat(propertiesOf(environment, SCHEMA_OWNERSHIP)).containsExactlyEntriesOf(SCHEMA_OWNERSHIP);
    }

    @Test
    void baseConfigurationSwitchesSpringdocOffAndItsGenericResponses() {
        assertThat(propertiesOf(load(), SPRINGDOC_OFF)).isEqualTo(SPRINGDOC_OFF);
    }

    @Test
    void localProfileServesTheApiDocsAndSwaggerUi() {
        assertThat(propertiesOf(load("local"), SPRINGDOC_ON)).isEqualTo(SPRINGDOC_ON);
    }

    private static StandardEnvironment load(String... profiles) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(profiles);
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

    private static Map<String, String> propertiesOf(StandardEnvironment environment, Map<String, String> expected) {
        Map<String, String> properties = new LinkedHashMap<>();
        expected.keySet().forEach(name -> properties.put(name, environment.getProperty(name)));
        return properties;
    }

    private static Map<String, String> schemaOwnership() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("spring.flyway.validate-migration-naming", "true");
        settings.put("spring.jpa.open-in-view", "false");
        settings.put("spring.jpa.hibernate.ddl-auto", "validate");
        return settings;
    }

    private static Map<String, String> localConnections() {
        Map<String, String> connections = new LinkedHashMap<>();
        connections.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/gameservice");
        connections.put("spring.datasource.username", "gameservice");
        connections.put("spring.datasource.password", "gameservice");
        connections.put("spring.data.redis.host", "localhost");
        connections.put("spring.data.redis.port", "6379");
        connections.put("spring.rabbitmq.host", "localhost");
        connections.put("spring.rabbitmq.port", "5672");
        connections.put("spring.rabbitmq.username", "guest");
        connections.put("spring.rabbitmq.password", "guest");
        return connections;
    }
}
