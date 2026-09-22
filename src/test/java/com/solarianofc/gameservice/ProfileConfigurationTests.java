package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

class ProfileConfigurationTests {

    private static final Map<String, String> LOCAL_CONNECTIONS = localConnections();

    @Test
    void localProfileConnectsToLocalhostServices() {
        StandardEnvironment environment = load("local");

        assertThat(propertiesOf(environment)).containsExactlyEntriesOf(LOCAL_CONNECTIONS);
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

    private static StandardEnvironment load(String... profiles) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(profiles);
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

    private static Map<String, String> propertiesOf(StandardEnvironment environment) {
        Map<String, String> properties = new LinkedHashMap<>();
        LOCAL_CONNECTIONS.keySet().forEach(name -> properties.put(name, environment.getProperty(name)));
        return properties;
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
