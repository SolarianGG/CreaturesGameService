package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class RabbitMqConnectionIntegrationTests {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Test
    void rabbitMqConnectionIsOpen() {
        try (Connection connection = connectionFactory.createConnection()) {
            assertThat(connection.isOpen()).isTrue();
        }
    }

    @Test
    void rabbitMqServerIsMajorVersion4() {
        try (Connection connection = connectionFactory.createConnection()) {
            assertThat(connection.getDelegate().getServerProperties().get("version"))
                    .asString()
                    .startsWith("4.");
        }
    }
}
