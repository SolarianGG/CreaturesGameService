# SOL-84 — Base Testcontainers integration test and Flyway baseline
Linear: https://linear.app/solarianofc/issue/SOL-84/base-testcontainers-integration-test-and-flyway-baseline
Status: in progress | Phase: 0
Spec approved: 2026-09-23

## Goal
Integration tests get one reusable entry point (`@IntegrationTest`) that starts the application context under the
`test` profile against real PostgreSQL 18, Redis and RabbitMQ containers, and the schema is owned by Flyway from the
first migration on, so the following slices (SOL-83, SOL-88, SOL-128, ...) build on a proven test and migration base.

## Scope
- `ContainersConfiguration` (D-93) gets two more `@ServiceConnection` beans: `RedisContainer` with `redis:8-alpine`
  (D-96) and `RabbitMQContainer` with `rabbitmq:4-management-alpine` (D-97); lifecycle via the Spring context cache (D-101).
- Meta-annotation `src/test/java/com/solarianofc/gameservice/IntegrationTest.java`: `@SpringBootTest`,
  `@ActiveProfiles("test")`, `@Import(ContainersConfiguration.class)` (D-95).
- Flyway baseline `src/main/resources/db/migration/V1__baseline.sql`: `CREATE EXTENSION IF NOT EXISTS citext` (D-94).
- `application.yml`: `spring.jpa.open-in-view: false`, `spring.jpa.hibernate.ddl-auto: validate` (D-98).
- Acceptance tests `FlywayBaselineIntegrationTests`, `RedisConnectionIntegrationTests`,
  `RabbitMqConnectionIntegrationTests`; `GameServiceApplicationTests` moves to `@IntegrationTest` (D-100).
- Flyway sensor proof with three deliberate, temporary violations (D-99, D-47).

## Out of scope
- Spring Modulith `event_publication` table (not in the baseline, D-94) — to the slice that first publishes events.
- Modulith `verify()` / ArchUnit (SOL-83); Actuator health for DB/Redis/RabbitMQ (SOL-86); Docker Compose (SOL-81).
- Tables of the data model (phase 1+); Flyway settings beyond Boot defaults (location `classpath:db/migration`).
- RabbitMQ STOMP plugin (real-time phase).

## Acceptance criteria
- An `@IntegrationTest` context starts against the three containers; Flyway applies `V1__baseline` (success in
  `flyway_schema_history`) and the `citext` extension exists.
- Redis answers `PING` through the application's `RedisConnectionFactory` (server major version 8); a RabbitMQ
  connection from the application's `ConnectionFactory` is open (server major version 4).
- Each of the three D-99 violations makes the context fail with its specific Flyway/PostgreSQL error.
- `./gradlew build` is green (all sensors incl. JaCoCo), locally and in CI.

## Decisions
D-13, D-47, D-83, D-87, D-89, D-93, D-94, D-95, D-96, D-97, D-98, D-99, D-100, D-101

## Test cases
Acceptance level for this slice (no API): Spring context tests annotated with `@IntegrationTest`.

- [ ] TC-1 `@IntegrationTest` + Redis: RED — `RedisConnectionIntegrationTests` (PING -> PONG, server major version 8)
      fails without the Redis container with a connection error to `localhost:6379`; GREEN — `RedisContainer` bean.
- [ ] TC-2 RabbitMQ: RED — `RabbitMqConnectionIntegrationTests` (connection open, server major version 4) fails
      without the RabbitMQ container with a connection error to `localhost:5672`; GREEN — `RabbitMQContainer` bean.
- [ ] TC-3 Flyway baseline: RED — `FlywayBaselineIntegrationTests` (applied migrations = exactly version 1 "baseline",
      success; `citext` in `pg_extension`) fails without a migration (no applied migrations); GREEN — `V1__baseline.sql`.
- [ ] TC-4 JPA settings: RED — `ProfileConfigurationTests` asserts the base configuration has
      `spring.jpa.open-in-view=false` and `spring.jpa.hibernate.ddl-auto=validate` (fails: absent); GREEN — D-98 values
      in `application.yml`; all integration tests still start (validate with no entities).
- [ ] TC-5 Flyway sensor proof (D-99), each violation added temporarily, run, error recorded, removed:
      (a) invalid SQL in `V2__...` -> migration fails with the PostgreSQL syntax error;
      (b) file name not matching `V{n}__{description}.sql` -> Flyway validate/naming error;
      (c) a second migration with version 1 -> "Found more than one migration with version 1".
- [ ] TC-6 `GameServiceApplicationTests` on `@IntegrationTest` (unchanged assertions); `./gradlew build` green
      (spotless, checkstyle, pmd, spotbugs, jacoco); after the user's push the CI run on the PR is green.

## Journal (append-only)
- Preconditions: Linear SOL-84 read (blocks SOL-83, SOL-88, SOL-128; blocked by SOL-80 — done); build already has
  `spring-boot-starter-flyway`, `flyway-database-postgresql`, `testcontainers-rabbitmq`, `testcontainers-redis` (D-82, D-91).
- Decisions D-94..D-101 approved (AskUserQuestion, two batches).
- L-19 check: PMD `TestClassWithoutTestCases` visits only `ASTClassDeclaration` and skips interfaces (javap of
  pmd-java 7.27.0) -> the annotation name `IntegrationTest` should not trigger it; confirmed by `pmdTest` in TC-6.
- Spec approved (gate 1). Commit plan: one docs commit on `main` (SOL-80 close leftovers, SOL-84 spec, D-94..D-101),
  then the branch slice/SOL-84-testcontainers-and-flyway-baseline from the clean `main`.

## Report (filled at STOP)

## Retro (-> LESSONS L-<n>)
