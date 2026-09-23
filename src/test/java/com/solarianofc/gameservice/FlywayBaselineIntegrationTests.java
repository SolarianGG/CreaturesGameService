package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class FlywayBaselineIntegrationTests {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void baselineMigrationsAreAppliedInOrder() {
        assertThat(Arrays.asList(flyway.info().applied()))
                .extracting(
                        info -> info.getVersion().getVersion(), MigrationInfo::getDescription, MigrationInfo::getState)
                .containsExactly(
                        tuple("1", "baseline", MigrationState.SUCCESS),
                        tuple("2", "event publication", MigrationState.SUCCESS));
    }

    @Test
    void citextExtensionIsInstalled() {
        assertThat(jdbcTemplate.queryForList("SELECT extname FROM pg_extension", String.class))
                .contains("citext");
    }
}
