package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@IntegrationTest
class GameServiceApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertThat(context.getBean(GameServiceApplication.class)).isNotNull();
    }

    @Test
    void runsUnderTheTestProfileOnly() {
        assertThat(environment.getActiveProfiles()).containsExactly("test");
    }

    @Test
    void dataSourceIsPostgreSql18() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(List.of(metaData.getDatabaseProductName(), metaData.getDatabaseMajorVersion()))
                    .containsExactly("PostgreSQL", 18);
        }
    }
}
