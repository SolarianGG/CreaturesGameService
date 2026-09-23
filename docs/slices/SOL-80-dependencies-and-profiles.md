# SOL-80 — Dependencies and profiles (local, test)
Linear: https://linear.app/solarianofc/issue/SOL-80/dependencies-and-profiles-local-test
Status: done | Phase: 0
Spec approved: 2026-09-23

## Goal
The application gets the full Spring Boot 4.1 dependency set from docs/PROJECT.md §2 and the `local` and `test`
profiles, and its context starts under `test` against a real PostgreSQL 18, so the following phase 0 slices
(SOL-84, SOL-83, SOL-86, ...) build on a working runtime (D-58).

## Scope
- `build.gradle`: starters under their Boot 4.1 names with their `*-test` companions, PostgreSQL driver (runtime),
  `spring-boot-testcontainers` and Testcontainers modules (D-82); versions from the Boot BOM.
- `gradle/libs.versions.toml`: `spring-modulith-bom` 2.1.1, imported via dependency management;
  `spring-modulith-starter-core`, `-starter-jpa`, `-starter-test` (D-85).
- `application.properties` -> `application.yml` (`spring.application.name` kept); `application-local.yml` with the
  D-86 values; `application-test.yml` (nothing test-specific yet: the connection comes from the container) (D-84).
- No default profile (D-88); no JPA/Hibernate settings (D-89).
- Test configuration class with a `PostgreSQLContainer` (`postgres:18-alpine`, D-87) exposed as a `@ServiceConnection`
  bean; `GameServiceApplicationTests` imports it and runs with `@ActiveProfiles("test")` (D-83).

## Out of scope
- Flyway baseline migration, Redis and RabbitMQ containers, a shared base integration test (SOL-84).
- Modulith `verify()` and ArchUnit (SOL-83); Actuator exposure, JSON logs (SOL-86); security configuration
  (phase 1); OpenAPI (SOL-137); Docker Compose (SOL-81).
- JPA/Hibernate settings (D-89).

## Acceptance criteria
- `./gradlew build` is green (all sensors incl. JaCoCo), locally and in CI.
- The application context starts under the `test` profile with a DataSource connected to PostgreSQL 18.
- No deprecated Boot 4 starter aliases (`spring-boot-starter-web`, `spring-boot-starter-oauth2-resource-server`).

## Decisions
D-6, D-13, D-58, D-59, D-66, D-74, D-82, D-83, D-84, D-85, D-86, D-87, D-88, D-89

## Test cases
Acceptance level for this slice (no API): the Spring context test under the `test` profile.

- [x] TC-1 Dependencies resolve and compile: `compileJava compileTestJava` green with the D-82 / D-85 set;
      `dependencies` shows Modulith 2.1.1 and only non-deprecated starter names.
- [x] TC-2 RED (negative, specific reason): with the starters added but no container, `contextLoads` fails with
      "Failed to configure a DataSource" — shown before the container configuration exists.
- [x] TC-3 GREEN: with the Testcontainers configuration `contextLoads` passes; the test also asserts that the
      active profiles are exactly `test` and that the DataSource reports PostgreSQL major version 18.
- [x] TC-4 `local` profile: a test loads the configuration with profile `local` (no context start, no Docker) and
      asserts the D-86 values (datasource URL/user/password, Redis host/port, RabbitMQ host/port/user);
      negative: without a profile these properties are absent (D-88).
- [x] TC-5 `./gradlew build` green (spotless, checkstyle, pmd, spotbugs, jacoco); after the user's push the CI run
      on the PR is green (Docker on `ubuntu-latest`).

## Journal (append-only)
- Preconditions: Boot 4.1.1 BOM read from the Gradle cache (starter names, Testcontainers 2.0.5,
  testcontainers-redis 2.2.4, Flyway 12.4.0, PostgreSQL driver 42.7.13); Modulith BOM 2.1.1 latest GA on Maven Central;
  local Docker 27.4.0.
- Decisions D-82..D-89 approved (AskUserQuestion, two batches).
- Harness: `session_state.py` proven live — the SessionStart hook injected STATE.md and the active slice at the
  start of this session (2026-09-23); blocker removed from STATE.md.
- Spec approved (gate 1). Commit plan (user choice): one docs commit on `main` with the SOL-87 leftovers and the
  SOL-80 spec + D-82..D-89, then the branch slice/SOL-80-dependencies-and-profiles from the clean `main`.
- Branch slice/SOL-80-dependencies-and-profiles created from main (5229b9c).
- TC-1 green: D-82 starters + D-85 Modulith BOM (catalog `spring-modulith` 2.1.1, `dependencyManagement` import)
  added; `compileJava compileTestJava` green; resolved: modulith-starter-core/jpa/test 2.1.1, testcontainers-postgresql
  2.0.5, testcontainers-redis 2.2.4, postgresql 42.7.13 (runtime); 0 matches for the deprecated `starter-web` /
  `starter-oauth2-resource-server` and 0 FAILED in runtime and testRuntime classpaths.
- TC-2 RED (expected): GameServiceApplicationTests under `@ActiveProfiles("test")` (contextLoads + new
  runsUnderTheTestProfileOnly, dataSourceIsPostgreSql18) -> 3/3 fail: "Failed to configure a DataSource: 'url'
  attribute is not specified and no embedded datasource could be configured. Reason: Failed to determine a suitable
  driver class".
- D-90 approved: TestcontainersConfiguration (postgres:18-alpine, `@ServiceConnection`), imported by the test.
- TC-3 test attempt 1/3 RED: container starts (PostgreSQL 18.6), context fails in `flywayInitializer`:
  `FlywayException: Unsupported Database: PostgreSQL 18.6` — Flyway 12 needs the separate `flyway-database-postgresql`
  module (in the Boot BOM), which D-82 did not list -> asking the user.
- D-91 approved: `runtimeOnly 'org.flywaydb:flyway-database-postgresql'`.
- TC-3 green (attempt 2/3): GameServiceApplicationTests 3/3 pass — context under `test`, active profiles exactly
  [test], DataSource PostgreSQL 18 (container postgres:18-alpine -> 18.6).
- D-92 approved: ProfileConfigurationTests via `ConfigDataEnvironmentPostProcessor.applyTo`.
- TC-4 RED (expected): `spring.datasource.url` expected "jdbc:postgresql://localhost:5432/gameservice" but was null.
  GREEN: application.properties -> application.yml (git rm), application-local.yml (D-86), application-test.yml
  (comment only, D-84); ProfileConfigurationTests + GameServiceApplicationTests green.
- TC-5 build attempt 1: `pmdTest` FAILED, 4 violations — 3x UnitTestContainsTooManyAsserts (fixed: one assertion per
  test — metadata list, property map `containsExactlyEntriesOf`, `noneMatch`, base-config check split into its own
  test) and TestClassWithoutTestCases on TestcontainersConfiguration (name starts with "Test", PMD's default
  test-class pattern) -> asking the user (conflicts with the D-90 name).
- D-93 approved (supersedes D-90): class renamed to ContainersConfiguration (no suppression, no ruleset change).
- TC-5 build attempt 2: `spotlessJavaCheck` FAILED on ContainersConfiguration — the shell rename (`sed -i`) wrote LF
  line endings and bypassed the Spotless hook; `spotlessApply` -> build green. Lessons L-18, L-19 recorded.
- TC-5 local green: `./gradlew clean build` BUILD SUCCESSFUL (spotlessCheck, checkstyleMain/Test, pmdMain/Test,
  spotbugsMain, test, jacocoTestCoverageVerification); tests 7/7 (GameServiceApplicationTests 3,
  ProfileConfigurationTests 3, NullMarkedPackagesTest 1). CI part pending the user's push.
- Verify: /simplify reviewed inline (no changes); diff traced to D-82..D-89, D-91..D-93 (build.gradle, catalog,
  three YAML files, ContainersConfiguration, two test classes); no Cyrillic; /security-review not required (security
  starters added, no security code). Hand-over: the user commits on the slice branch, pushes, opens the PR to `main`
  and reports the CI result (TC-5).
- TC-5 green: commit 88e6c3d (all 13 files, checked with `git show --stat`) pushed on the slice branch, PR to `main`;
  the CI run is green (reported by the user) — Testcontainers PostgreSQL works on `ubuntu-latest`.
- Merged: PR #2 -> `main` (a656d8c). Linear replication (user confirmed): description = approved spec (+ D-91..D-93),
  report comment; the issue was already Done — the Linear/GitHub integration closed it on merge and attached PR #2.

## Report (filled at STOP)
- Done: Boot 4.1 starters under their new names (`webmvc`, `security-oauth2-resource-server`) with `*-test`
  companions; PostgreSQL driver and `flyway-database-postgresql` (runtime); Testcontainers 2.0.5 (+ redis 2.2.4);
  Spring Modulith 2.1.1 (BOM in the catalog; core, jpa, test). `application.yml` + `application-local.yml`
  (localhost, D-86) + `application-test.yml`; no default profile. `ContainersConfiguration` (postgres:18-alpine,
  `@ServiceConnection`); GameServiceApplicationTests (context, profile, PostgreSQL 18) and ProfileConfigurationTests
  (local values, no connections without a profile, base config loaded).
- Sensors: `./gradlew clean build` green (spotless, checkstyle, pmd, spotbugs, jacoco), tests 7/7; CI green on the PR.
  Fix attempts: context test 2/3 (Flyway module), build 3 (PMD asserts + test-class name; Spotless line endings).
- Deviations from the approved spec: D-91 (extra Flyway PostgreSQL module); D-93 supersedes D-90 (class name);
  one extra test (`withoutProfileTheBaseConfigurationIsLoaded`) from splitting assertions for PMD.
- Open points: security starters are active with Boot defaults (generated user) until phase 1; no Flyway migrations
  yet (SOL-84); Redis/RabbitMQ are not contacted by tests yet (SOL-84).

## Retro (-> LESSONS L-<n>)
- L-18 (mistake): a shell rename bypassed the Spotless hook (LF line endings) -> source edits via Edit/Write only.
- L-19 (mistake): a proposed class name matched PMD's test-class pattern -> check names against analyzer rules first.
- Worked well: reading the Boot BOM from the Gradle cache gave the exact Boot 4.1 artifact names before any question;
  the RED run of TC-2 surfaced the specific rejection reason, and TC-3's failure pointed straight to the missing module.
- Harness proposals: none this slice (L-18 stays a candidate ANTI-PATTERN if it repeats).
