package com.solarianofc.gameservice.shared.internal.openapi;

import com.solarianofc.gameservice.IntegrationTest;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.TestPropertySource;

/**
 * {@link IntegrationTest} with springdoc enabled as in the {@code local} profile (D-56). One annotation, so every class
 * using it shares one cached context (D-188).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@IntegrationTest
@TestPropertySource(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
@interface SpringdocIntegrationTest {}
