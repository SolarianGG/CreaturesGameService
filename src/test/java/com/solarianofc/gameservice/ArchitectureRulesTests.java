package com.solarianofc.gameservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solarianofc.archfixtures.rules.FieldInjection;
import com.solarianofc.archfixtures.rules.JavaUtilLogging;
import com.solarianofc.archfixtures.rules.MisplacedController;
import com.solarianofc.archfixtures.rules.StandardStreams;
import com.solarianofc.archfixtures.rules.internal.web.PlacedController;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;

/**
 * Architecture rules on the main classes (D-116). Every rule also has a negative test on a fixture from
 * {@code com.solarianofc.archfixtures.rules} (D-112), so a rule that silently stops matching fails the build.
 */
@AnalyzeClasses(packagesOf = GameServiceApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTests {

    /** Checks fields; the main classes have none yet, so an empty {@code should} is allowed (D-116). */
    @ArchTest
    static final ArchRule NO_FIELD_INJECTION =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);

    @Test
    void fieldInjectionViolatesTheRule() {
        assertViolation(
                NO_FIELD_INJECTION,
                FieldInjection.class,
                "Field <" + FieldInjection.class.getName() + ".dependency> is annotated with");
    }

    @ArchTest
    static final ArchRule NO_STANDARD_STREAMS = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @Test
    void standardStreamAccessViolatesTheRule() {
        assertViolation(
                NO_STANDARD_STREAMS,
                StandardStreams.class,
                "Method <" + StandardStreams.class.getName() + ".getOut()> gets field <java.lang.System.out>");
    }

    /**
     * Any dependency on java.util.logging (D-116); the library rule only flags setting a field of a JUL type and
     * misses a direct {@code Logger.getLogger(...)} call.
     */
    @ArchTest
    static final ArchRule NO_JAVA_UTIL_LOGGING = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAPackage("java.util.logging..")
            .because("logs go through SLF4J into the structured log output");

    @Test
    void javaUtilLoggingViolatesTheRule() {
        assertViolation(
                NO_JAVA_UTIL_LOGGING,
                JavaUtilLogging.class,
                "Method <" + JavaUtilLogging.class.getName()
                        + ".getLogger()> calls method <java.util.logging.Logger.getLogger");
    }

    /**
     * Web controllers belong to a module's {@code internal.web} package (docs/PROJECT.md §3.1, D-116); covers
     * {@code @RestController} through its {@code @Controller} meta-annotation. The main classes have no controllers
     * yet, so an empty {@code should} is allowed.
     */
    @ArchTest
    static final ArchRule CONTROLLERS_IN_INTERNAL_WEB = classes()
            .that()
            .areMetaAnnotatedWith(Controller.class)
            .should()
            .resideInAPackage("..internal.web..")
            .allowEmptyShould(true);

    @Test
    void controllerOutsideInternalWebViolatesTheRule() {
        assertViolation(
                CONTROLLERS_IN_INTERNAL_WEB,
                MisplacedController.class,
                "Class <" + MisplacedController.class.getName() + "> does not reside in a package '..internal.web..'");
    }

    @Test
    void controllerInInternalWebSatisfiesTheRule() {
        // Empty should not allowed here: the fixture must actually be matched by the rule, not skipped.
        assertThatCode(() -> check(CONTROLLERS_IN_INTERNAL_WEB.allowEmptyShould(false), PlacedController.class))
                .doesNotThrowAnyException();
    }

    private static void assertViolation(ArchRule rule, Class<?> fixture, String violation) {
        assertThatThrownBy(() -> check(rule, fixture))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(violation);
    }

    private static void check(ArchRule rule, Class<?> fixture) {
        rule.check(new ClassFileImporter().importClasses(fixture));
    }
}
