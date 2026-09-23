# SOL-86 — Structured JSON logs and Actuator (health, liveness/readiness)
Linear: https://linear.app/solarianofc/issue/SOL-86/structured-json-logs-and-actuator-health-livenessreadiness
Status: done | Phase: 0
Spec approved: 2026-09-23

## Goal
The application writes ECS JSON logs with `traceId`/`spanId`, exposes only `health` and `prometheus` on a separate
management port, and its readiness reflects the DB, Redis and RabbitMQ, so Compose (SOL-81), Prometheus and phase 1+
slices build on observable, probe-ready runtime.

## Scope
- JSON logs: `logging.structured.format.console: ecs` in `application.yml`; the `local` and `test` profiles log plain
  text (D-118, D-119).
- Tracing: `spring-boot-micrometer-tracing-opentelemetry` + `micrometer-tracing-bridge-otel`, no exporter,
  `management.tracing.sampling.probability: 1.0` (D-120, D-121).
- Actuator: `management.server.port: 8081`, web exposure exactly `health`, `prometheus`;
  `micrometer-registry-prometheus` (D-122). Probes only on the management port (D-123).
- Readiness group: `readinessState`, `db`, `redis`, `rabbit` (D-125).
- Security (first code of `shared`, D-104, D-126): `shared.internal.security.SecurityConfiguration` with
  `managementSecurityFilterChain` (health incl. groups + prometheus `permitAll`, rest `denyAll`) and
  `applicationSecurityFilterChain` (everything `denyAll` until phase 1) (D-124).
- `@IntegrationTest` switches to `webEnvironment = RANDOM_PORT` (D-127).
- Tests: shared-context actuator tests; own contexts for the RabbitMQ outage and the JSON log test (D-125, D-128).

## Out of scope
- `userId` / `matchId` in MDC (with `account` / `match`), OTLP span or metrics export, business metrics.
- Prometheus scrape config, Grafana, network isolation of port 8081, Compose healthchecks (SOL-81).
- Health details / components for anonymous callers (Boot default stays); real outage tests for DB and Redis (D-125).
- Real authentication of the application chain (phase 1, `account`).

## Acceptance criteria
- Management port: `/actuator/health` answers 200 `UP` anonymously; `/actuator/health/liveness` and
  `/actuator/health/readiness` answer 200 `UP`; `/actuator/prometheus` answers 200 in the Prometheus text format with
  JVM metrics; any other `/actuator/**` path is rejected by the management chain.
- Main port: actuator paths are not served there; any request is rejected by the application chain.
- The readiness group contains `db`, `redis`, `rabbit`, all `UP`; after the RabbitMQ container is stopped readiness
  answers 503 `DOWN` while liveness stays 200 `UP`.
- With the ECS format enabled, a log line written inside an observation is valid JSON in ECS shape and carries the
  current trace and span IDs; the default profile resolves to `ecs`, `local` and `test` to plain text.
- `./gradlew build` green locally and in CI; `/security-review` done (security code in `shared`).

## Decisions
D-6, D-47, D-64, D-101, D-104, D-114, D-118..D-128 (D-127 supersedes D-95)

## Test cases
Acceptance level for this slice (no business API): HTTP against the management and main ports of the
`@IntegrationTest` context (real server, D-127), plus own-context tests. Negative cases assert the specific status /
body, not only "not 200". Exact rejection statuses are pinned at RED and recorded in the journal.

- [x] TC-1 Health on the management port: RED — `ActuatorEndpointsIntegrationTests` requests `/actuator/health` on
      the management port (`@LocalManagementPort`) -> fails without a separate management server; GREEN — D-127
      annotation, `management.server.port`, exposure; asserts 200 `{"status":"UP"}` anonymously and that
      `/actuator/health` on the main port is not served by actuator.
- [x] TC-2 Prometheus: RED — `/actuator/prometheus` on the management port is not 200 (not exposed / no registry);
      GREEN — `micrometer-registry-prometheus`, exposure, security permit; asserts 200, Prometheus content type and a
      `jvm_` metric line.
- [x] TC-3 Security chains: RED — `/actuator/env` on the management port and a request to `/` on the main port are
      not rejected by our chains (Boot defaults); GREEN — `SecurityConfiguration`; asserts the pinned rejection
      status for both; health and prometheus stay anonymous (TC-1, TC-2 regress green).
- [x] TC-4 Probes and readiness composition: RED — readiness group does not contain `db`/`redis`/`rabbit`
      (in-process `HealthEndpoint` for path `readiness`); GREEN — group config; asserts the three components `UP`,
      liveness and readiness 200 `UP` over HTTP.
- [x] TC-5 RabbitMQ outage: `ReadinessOutageIntegrationTests` (own context + containers, D-125, D-128); RED —
      without `rabbit` in the group readiness stays 200 after the container stop (checked by temporarily removing it);
      GREEN — readiness 503 `DOWN`, liveness 200 `UP`.
- [x] TC-6 JSON logs with trace IDs: `StructuredLoggingIntegrationTests` (own context, ECS enabled for it only,
      D-119, D-128); RED — without the OTel tracer the ECS line has no trace/span IDs; GREEN — tracing dependencies +
      sampling; the captured line parses as JSON, has the ECS shape and IDs equal to the current span's.
- [x] TC-7 Profiles: RED — `ProfileConfigurationTests` expects `ecs` in default and plain text in `local`/`test`;
      GREEN — `application.yml` / profile files; the way to switch a profile back to plain text is verified in the
      Boot 4.1.1 jar first (asked if it needs anything beyond a property).
- [~] TC-8 Verify: `/simplify`, `./gradlew build` green (incl. jacoco), `/security-review`, diff vs spec and
      D-<n>; CI on the PR green after the user's push.

## Journal (append-only)
- Preconditions: SOL-83 PR #4 merged, `main` == `origin/main`, clean tree (fresh `git fetch`/`status`); Linear SOL-83
  Done, SOL-86 Backlog. Verified in Boot 4.1.1 jars: probes enabled by default, readiness group has no
  `db`/`redis`/`rabbit` by default, web exposure default `health`, default management security permits only health
  (rest formLogin/httpBasic), indicator names `db`/`redis`/`rabbit`.
- Decisions D-118..D-128 recorded (AskUserQuestion, four batches; D-123, D-124 delegated to the agent).
- Spec approved (gate 1). Commit plan: one docs commit on `main` by the user, then the slice branch from the clean
  `main`.
- Docs commit 695a4cd on `main` (pushed; checked with `git fetch` + `git log`); branch
  slice/SOL-86-structured-logs-and-actuator created from it. D-129 (RestTestClient) approved before TC-1.
- TC-1 RED: `ActuatorEndpointsIntegrationTests` -> both tests `BeanCreationException` / `PlaceholderResolutionException`
  for the local ports (MOCK environment starts no server) — the expected reason.
- TC-1 green: `@IntegrationTest` with `RANDOM_PORT` (D-127), `management.server.port: 8081` (D-122); health on the
  management port 200 `UP` anonymously. Main port `/actuator/health` pinned as 401 (Boot default chain, httpBasic);
  TC-3 replaces it with the application-chain status. Tests use random main and management ports (5 Tomcat ports
  in the run: the shared and the outbox context do not collide). Package checkpoint green: 31 tests, 0 failed.
- TC-2 RED: `/actuator/prometheus` on the management port -> 401 (Boot default chain). After
  `micrometer-registry-prometheus` (runtimeOnly) + exposure `health, prometheus` still 401 — security confirmed as the
  remaining reason. TC-2 and TC-3 closed together: TC-3 tests written before the security code.
- TC-3 RED: `/actuator/env` (management), `/actuator/health` and `/` (main) -> 401 instead of the expected 403 without
  `WWW-Authenticate` (`denyAll`, D-124). Test for main-port health re-pinned from 401 (TC-1) to 403.
- `SecurityConfiguration` in `shared.internal.security` (D-126): TC-3 green; prometheus then 403 — the endpoint did not
  exist, so the request fell to the application chain ("Exposing 1 endpoint"). Root cause (Boot 4.1.1 jar):
  `MetricsContextCustomizerFactory` sets `management.defaults.metrics.export.enabled=false` for every
  `@SpringBootTest` without `@AutoConfigureMetrics`. D-130 approved: `@AutoConfigureMetrics` on `@IntegrationTest`.
  Not a sensor fix attempt (spec gap, asked). `/actuator/env` is denied by the application chain (not exposed, so
  outside `EndpointRequest.toAnyEndpoint()`); test renamed to `otherManagementPathsAreDenied`.
- TC-2 + TC-3 green: 5/5 in `ActuatorEndpointsIntegrationTests`, "Exposing 2 endpoints". Package checkpoint green
  (incl. Modulith verify / ArchUnit with the new `shared` module): 34 tests, 0 failed.
- TC-4 RED: `HealthEndpoint.healthForPath("readiness")` components = only `readinessState` ("could not find the
  following keys: db, redis, rabbit"). HTTP health/liveness/readiness 200 `UP` were green on the first run
  (characterization: probes are on by default, D-123). GREEN: `management.endpoint.health.group.readiness.include:
  readinessState, db, redis, rabbit`; the test pins exactly these four components, all `UP`. Package checkpoint
  green: 37 tests, 0 failed.
- TC-5 RED (temporary `@TestPropertySource` with readiness = `readinessState, db, redis`, no config edit): after
  `rabbitMqContainer.stop()` readiness stayed 200 ("expected 503 but was 200"). GREEN after removing the override:
  readiness 503 `DOWN` (awaited, ~2 s), liveness 200 `UP`. Own context via an empty nested `@TestConfiguration`
  (as D-114) + `@DirtiesContext`; the run shows one RabbitMQ container per context (shared, outbox, outage).
  Package checkpoint green: 38 tests, 0 failed.
- TC-6 check before coding (L-24, TC-2 finding): Boot 4.1.1 `TracingContextCustomizerFactory` only sets
  `management.tracing.export.enabled=false` in tests — tracing itself stays on; Boot default sampling is 0.1.
- TC-6 RED: ECS line valid (`ecs.version` 8.11) but without `traceId`/`spanId` (no tracer). GREEN:
  `spring-boot-micrometer-tracing-opentelemetry` + `micrometer-tracing-bridge-otel` (implementation), sampling 1.0;
  ECS writes the MDC keys top-level as `traceId` / `spanId`. The test then pins equality with
  `tracer.currentTraceContext().context()` inside the observation. Package checkpoint green: 39 tests, 0 failed.
- TC-7 check before coding (Boot 4.1.1 jar): `DefaultLogbackConfiguration` uses the structured encoder only when
  `${CONSOLE_LOG_STRUCTURED_FORMAT:-}` `hasLength`, so an empty `logging.structured.format.console` falls back to the
  plain text pattern — a property is enough, nothing to ask. Concern: `LoggingSystemProperties`' default setter writes
  the JVM system property only if absent, which would make the ECS test order-dependent — disproved empirically:
  the ECS test ran after the shared `test` context and passed; per-context logs show plain text for the shared and
  outage contexts and ECS only in `StructuredLoggingIntegrationTests`.
- TC-7 RED: `ProfileConfigurationTests` — default `null` instead of `ecs`, `local`/`test` `null` instead of empty.
  GREEN: `ecs` in `application.yml`, `""` in `application-local.yml` and `application-test.yml`. Package checkpoint
  green: 42 tests, 0 failed.
- /simplify (4 review agents: reuse, simplification, efficiency, altitude). Applied: one RestTestClient per class
  (`@BeforeEach`) instead of one per request / poll; outage poll interval 500 ms (each poll runs DB + Redis + RabbitMQ
  checks); flatter readiness-composition assertion; Javadoc on `OwnContext` states that removing it silently merges
  the class into the shared context. Skipped: shared RestTestClient helper (2 callers only), parameterizing the deny
  tests (not simpler), health cache TTL / sampling (config and approved D-121), an outcome test "test profile logs
  plain text" (Logback is JVM-global — order-dependent after the ECS context; mechanism verified in TC-7).
- spotbugs attempt 1/3 FAIL: `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` on both `SecurityConfiguration` beans ->
  `throws Exception` removed (Spring Security 7.1.1 `AbstractSecurityBuilder.build()` declares no exception). Fixed.
- pmdTest attempt 1/3 FAIL: `UnitTestContainsTooManyAsserts` (readiness composition, ECS test) and
  `UnitTestShouldIncludeAssert` (probe tests: RestTestClient `expect*` chains are not recognised as assertions) ->
  one AssertJ assertion per test over a projection (status map, `Probe(httpStatus, status)` record, field list),
  helper `assertUp(...)`; outage waits via `await().until(...)`. Also a compile error on the way: `JsonNode` is
  `Iterable`, so `assertThat(JsonNode)` picked the iterable assert -> plain `List` projection.
- pmdTest attempt 2/3 FAIL: `AvoidDuplicateLiterals` ("readiness" x5) -> constants `READINESS` / `LIVENESS`. The
  replacement was done with a Python script on a Java file (anti-pattern L-22); `spotlessApply` run right after.
- `./gradlew build` green (spotlessCheck, checkstyle, pmd, spotbugs, all tests, jacoco).
- /security-review (sub-agent, read-only): no findings >= 8/10, nothing MEDIUM. Checked: both chains end in
  `denyAll` (no fail-open path), anonymous health shows status only (show-details / show-components default
  `never`), prometheus anonymous by design (D-122, D-124; isolation in SOL-81), no exporter, no new sensitive logging.
- Diff vs spec and D-<n>: every change traces to D-118..D-130. Deviation from the acceptance wording: non-exposed
  `/actuator/**` on the management port is rejected by the application chain (403), not the management chain —
  same result, `EndpointRequest.toAnyEndpoint()` only matches exposed endpoints. Decisions added during the build:
  D-129 (RestTestClient), D-130 (`@AutoConfigureMetrics`). Retro: L-26..L-28; harness changes D-131, D-132 approved
  for a separate change.
- Linear replicated (confirmed in the permission prompts): SOL-86 description = approved spec, status In Review,
  report comment. No new Backlog issues.

## Report (filled at STOP)
- Done: ECS JSON console logs by default, plain text in `local`/`test` (D-118, D-119); `traceId`/`spanId` from the
  OpenTelemetry bridge without export, sampling 1.0 (D-120, D-121); Actuator on port 8081 exposing only `health` and
  `prometheus` (D-122, D-123); readiness = `readinessState`, `db`, `redis`, `rabbit` (D-125); first `shared` code:
  `SecurityConfiguration` with management and application chains (D-124, D-126); `@IntegrationTest` on random ports
  with metrics export (D-127, D-130); tests `ActuatorEndpointsIntegrationTests`, `ReadinessOutageIntegrationTests`
  (real RabbitMQ stop), `StructuredLoggingIntegrationTests`, `ProfileConfigurationTests` additions.
- Sensors: `./gradlew build` green locally (42 tests, spotless, checkstyle, pmd, spotbugs, jacoco); spotbugs 1/3 and
  pmdTest 2/3 attempts, fixed without suppressions; `/security-review` clean. CI: pending the user's push (TC-8).
- Deviations from the approved spec: acceptance wording for non-exposed management paths (see journal); two
  decisions added during the build (D-129, D-130). Deviations from docs/PROJECT.md: none (§7, §8 hold; `userId` /
  `matchId` in MDC stays with the modules that own them).
- Deferred: nothing new for Backlog — the remaining out-of-scope items already belong to SOL-81 (port isolation,
  Prometheus scrape, healthchecks) and phase 1 (real authentication, `userId` MDC).

## Retro (-> LESSONS L-<n>)
- Went wrong: Boot's test context customizer disabled metrics export and hid the prometheus endpoint (L-26 -> D-130);
  PMD test rules were only met in verify (L-27 -> D-131); a Java file was edited through a Python script again
  (L-28 -> D-132).
- Worked: checking framework defaults in the exact jars before asking (L-24 applied: probes, default management
  security, indicator names, empty structured format, tracing in tests) — no decision had to be re-asked; RED via a
  temporary `@TestPropertySource` instead of a config edit (TC-5); the order-dependency concern in TC-7 was tested
  empirically instead of assumed.
- Harness proposals: D-131 (pmd at checkpoint), D-132 (shell-write-to-Java hook) — approved, separate change.
