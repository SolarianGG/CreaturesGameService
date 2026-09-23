package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

/** Module boundaries of the application (D-105) and a permanent proof that the sensor detects violations (D-106). */
class ModularityTests {

    /** Fixture root outside the application package; its classes live in the test sources (D-112). */
    private static final String FIXTURE_MODULES = "com.solarianofc.archfixtures.modules";

    @Test
    void applicationModulesVerify() {
        ApplicationModules.of(GameServiceApplication.class).verify();
    }

    @Test
    void accessToAnotherModulesInternalTypeFailsVerification() {
        ApplicationModules fixture = ApplicationModules.of(FIXTURE_MODULES, new ImportOption.OnlyIncludeTests());

        assertThatThrownBy(fixture::verify)
                .isInstanceOf(Violations.class)
                .hasMessageContaining("depends on non-exposed type " + FIXTURE_MODULES + ".beta.internal.BetaInternal");
    }
}
