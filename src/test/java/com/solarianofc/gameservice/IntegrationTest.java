package com.solarianofc.gameservice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context under the {@code test} profile against the PostgreSQL, Redis and RabbitMQ containers of
 * {@link ContainersConfiguration} (D-95); classes with this annotation share one cached context (D-101).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import(ContainersConfiguration.class)
public @interface IntegrationTest {}
