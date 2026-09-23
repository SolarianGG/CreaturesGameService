package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@IntegrationTest
class RedisConnectionIntegrationTests {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Test
    void redisAnswersPing() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
    }

    @Test
    void redisServerIsMajorVersion8() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            assertThat(connection.serverCommands().info("server").getProperty("redis_version"))
                    .startsWith("8.");
        }
    }
}
