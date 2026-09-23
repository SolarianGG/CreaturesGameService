# SOL-84 — Base Testcontainers integration test and Flyway baseline
Linear: https://linear.app/solarianofc/issue/SOL-84/base-testcontainers-integration-test-and-flyway-baseline
Status: done | Phase: 0
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

- [x] TC-1 `@IntegrationTest` + Redis: RED — `RedisConnectionIntegrationTests` (PING -> PONG, server major version 8)
      fails without the Redis container with a connection error to `localhost:6379`; GREEN — `RedisContainer` bean.
- [x] TC-2 RabbitMQ: RED — `RabbitMqConnectionIntegrationTests` (connection open, server major version 4) fails
      without the RabbitMQ container with a connection error to `localhost:5672`; GREEN — `RabbitMQContainer` bean.
- [x] TC-3 Flyway baseline: RED — `FlywayBaselineIntegrationTests` (applied migrations = exactly version 1 "baseline",
      success; `citext` in `pg_extension`) fails without a migration (no applied migrations); GREEN — `V1__baseline.sql`.
- [x] TC-4 JPA settings: RED — `ProfileConfigurationTests` asserts the base configuration has
      `spring.jpa.open-in-view=false` and `spring.jpa.hibernate.ddl-auto=validate` (fails: absent); GREEN — D-98 values
      in `application.yml`; all integration tests still start (validate with no entities).
- [x] TC-5 Flyway sensor proof (D-99), each violation added temporarily, run, error recorded, removed:
      (a) invalid SQL in `V2__...` -> migration fails with the PostgreSQL syntax error;
      (b) file name not matching `V{n}__{description}.sql` -> Flyway validate/naming error;
      (c) a second migration with version 1 -> "Found more than one migration with version 1".
- [x] TC-6 `GameServiceApplicationTests` on `@IntegrationTest` (unchanged assertions); `./gradlew build` green
      (spotless, checkstyle, pmd, spotbugs, jacoco); after the user's push the CI run on the PR is green.

## Journal (append-only)
- Preconditions: Linear SOL-84 read (blocks SOL-83, SOL-88, SOL-128; blocked by SOL-80 — done); build already has
  `spring-boot-starter-flyway`, `flyway-database-postgresql`, `testcontainers-rabbitmq`, `testcontainers-redis` (D-82, D-91).
- Decisions D-94..D-101 approved (AskUserQuestion, two batches).
- L-19 check: PMD `TestClassWithoutTestCases` visits only `ASTClassDeclaration` and skips interfaces (javap of
  pmd-java 7.27.0) -> the annotation name `IntegrationTest` should not trigger it; confirmed by `pmdTest` in TC-6.
- Spec approved (gate 1). Commit plan: one docs commit on `main` (SOL-80 close leftovers, SOL-84 spec, D-94..D-101),
  then the branch slice/SOL-84-testcontainers-and-flyway-baseline from the clean `main`.
- Docs commit 9249c64 on `main` (checked with `git log`); branch slice/SOL-84-testcontainers-and-flyway-baseline
  created from it. Nothing listens on 6379/5672 locally (netstat), so RED runs cannot pass by accident.
- TC-1 RED (expected): `@IntegrationTest` + RedisConnectionIntegrationTests -> 2/2 fail: `RedisConnectionException`
  "Unable to connect to Redis", caused by "Connection refused: localhost/127.0.0.1:6379".
- TC-1 green: `RedisContainer` (`redis:8-alpine`) bean -> 2/2 pass (PONG, redis_version 8.x); package checkpoint green.
- Observation (pre-existing since SOL-80): on context shutdown `eventPublicationRegistry` logs WARN
  `relation "event_publication" does not exist` — the Modulith JPA registry has no table (D-94 keeps it out of V1).
  Relevant for TC-4: `ddl-auto: validate` (D-98) validates the Modulith `event_publication` entity too.
- TC-2 RED (expected): RabbitMqConnectionIntegrationTests -> 2/2 fail: `AmqpConnectException:
  java.net.ConnectException: Connection refused` (Boot default localhost:5672; the port is not in the message).
- TC-2 green: `RabbitMQContainer` (`rabbitmq:4-management-alpine`) bean -> 2/2 pass (connection open, version 4.x);
  package checkpoint green: 11 tests (GameServiceApplicationTests 3, NullMarkedPackagesTest 1,
  ProfileConfigurationTests 3, RabbitMq 2, Redis 2).
- TC-3 RED (expected): FlywayBaselineIntegrationTests -> 2/2 fail: applied migrations `[]` instead of
  `[("1", "baseline", SUCCESS)]`; `pg_extension` = `["plpgsql"]` without `citext`.
- TC-3 green: `V1__baseline.sql` (D-94) -> 2/2 pass; package checkpoint green: 13 tests.
- TC-4 RED (expected): `baseConfigurationLeavesTheSchemaToFlyway` -> both properties `null`.
- TC-4 test attempt 1/3 FAIL: with the D-98 values ProfileConfigurationTests 4/4 pass, but every `@IntegrationTest`
  context fails: `SchemaManagementException: Schema validation: missing table [event_publication]` (Modulith JPA
  entity vs D-94 baseline without that table) -> D-94 and D-98 conflict, asking the user.
- D-102 approved: `V2__event_publication.sql` = the Modulith reference script
  `schemas/v2/schema-postgresql.sql` from spring-modulith-events-jdbc 2.1.1 (Maven Central, read in the scratchpad;
  its 9 columns match the `JpaEventPublication` fields, checked with javap). Deviation from the spec: the table was
  out of scope; FlywayBaselineIntegrationTests now expects V1 + V2 (`baselineMigrationsAreAppliedInOrder`).
- TC-4 green (attempt 2/3): package 14/14 green (ProfileConfigurationTests 4, Flyway 2, ...); the shutdown WARN about
  `event_publication` is gone. Lesson L-20 recorded.
- TC-5 (a) proven: temporary `V3__broken_sql.sql` (`CREATE TABL ...`) -> FlywayBaselineIntegrationTests 2/2 fail:
  `FlywayMigrateException`, `ERROR: syntax error at or near "TABL"`; file removed.
- TC-5 (b) NOT caught: temporary `V3_bad_name.sql` (single underscore) -> tests green, Flyway logs
  "Successfully validated 2 migrations" and silently ignores the file (Flyway default `validateMigrationNaming=false`);
  file removed -> sensor gap, asking the user (would need `spring.flyway.validate-migration-naming: true`).
- TC-5 (c) proven: temporary `V1__duplicate_version.sql` -> 2/2 fail: `FlywayException`
  "Found more than one migration with version 1"; file removed.
- D-103 approved: `spring.flyway.validate-migration-naming: true` in `application.yml`.
- TC-5 (b) proven after D-103: temporary `V3_bad_name.sql` -> 2/2 fail: `FlywayException: Invalid SQL filenames found:
  Invalid versioned migration name format: V3_bad_name.sql (could not recognise version number 3_bad_name)`; file
  removed. Sensor active for all three D-99 violations. ProfileConfigurationTests asserts the D-98/D-103 values
  (`baseConfigurationLeavesTheSchemaToFlyway`, shared `propertiesOf` helper); package checkpoint green: 14 tests.
- TC-6: GameServiceApplicationTests on `@IntegrationTest` (Spotless removed the now unused imports).
  `./gradlew clean build` BUILD SUCCESSFUL (spotlessCheck, checkstyleMain/Test, pmdMain/Test incl. the annotation
  name `IntegrationTest`, spotbugsMain, test, jacocoTestCoverageVerification); tests 14/14. CI part pending the push.
- Verify: /simplify reviewed inline (ProfileConfigurationTests assertion simplified, nothing else); diff traced to
  D-94..D-103 (application.yml, V1, V2, ContainersConfiguration, IntegrationTest, 3 new test classes, 2 changed test
  classes); no Cyrillic; /security-review not required (no account/security code).
- Close (local): report and retro filled, lesson L-21 recorded; STATE.md at step E (gate 2). Hand-over: the user
  commits the 13 paths on the slice branch, pushes, opens the PR and reports CI (TC-6).
- TC-6 green: commit 147dd21 (14 files, checked with `git show --stat`; equals `origin/slice/SOL-84-...` after
  `git fetch`) pushed, PR to `main` open; the CI run is green (reported by the user) — Redis and RabbitMQ
  Testcontainers work on `ubuntu-latest`. Slice status done; Linear replication next (user confirmed "close").
- Linear replication done: description = approved spec (+ D-102, D-103 notes), report comment, status In Review;
  PR #3 already attached by the integration. No new Backlog issues. Done follows on the PR merge.

## Report (filled at STOP)
- Done: `@IntegrationTest` meta-annotation (context under `test` + ContainersConfiguration); ContainersConfiguration
  with PostgreSQL 18, `redis:8-alpine` and `rabbitmq:4-management-alpine` (`@ServiceConnection`, one set per cached
  context). Flyway: `V1__baseline.sql` (citext), `V2__event_publication.sql` (Modulith 2.1 registry, reference
  script), `validate-migration-naming: true`; JPA `open-in-view: false`, `ddl-auto: validate`. Acceptance tests
  FlywayBaselineIntegrationTests, RedisConnectionIntegrationTests, RabbitMqConnectionIntegrationTests.
- Sensors: `./gradlew clean build` green (spotless, checkstyle, pmd, spotbugs, jacoco), tests 14/14; CI green on the PR.
  Flyway sensor proven on invalid SQL, bad file name, duplicate version (D-99). Fix attempts: integration tests 2/3
  (event_publication table, D-102).
- Deviations from the approved spec: D-102 (event_publication table was out of scope, now V2; Flyway test expects
  V1 + V2); D-103 (Flyway naming validation, not in the spec — needed for the D-99 proof).
- Open points: none new; Redis/RabbitMQ are only connection-checked (their features come with their slices).

## Retro (-> LESSONS L-<n>)
- L-20 (mistake): D-94 and D-98 conflicted through the Modulith JPA entity -> check classpath entities and existing
  WARNs before proposing schema options.
- L-21 (success): proving the sensor by deliberate violations (D-47) exposed that Flyway ignores misnamed files by
  default — without the proof the gap would have stayed invisible.
- Worked well: taking the event_publication DDL from the library's own artifact; RED runs with the specific cause.
- Harness proposals: none this slice.
