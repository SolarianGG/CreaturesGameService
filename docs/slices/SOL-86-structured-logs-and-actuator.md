# SOL-86 — Structured JSON logs and Actuator (health, liveness/readiness)
Linear: https://linear.app/solarianofc/issue/SOL-86/structured-json-logs-and-actuator-health-livenessreadiness
Status: in progress | Phase: 0
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

- [ ] TC-1 Health on the management port: RED — `ActuatorEndpointsIntegrationTests` requests `/actuator/health` on
      the management port (`@LocalManagementPort`) -> fails without a separate management server; GREEN — D-127
      annotation, `management.server.port`, exposure; asserts 200 `{"status":"UP"}` anonymously and that
      `/actuator/health` on the main port is not served by actuator.
- [ ] TC-2 Prometheus: RED — `/actuator/prometheus` on the management port is not 200 (not exposed / no registry);
      GREEN — `micrometer-registry-prometheus`, exposure, security permit; asserts 200, Prometheus content type and a
      `jvm_` metric line.
- [ ] TC-3 Security chains: RED — `/actuator/env` on the management port and a request to `/` on the main port are
      not rejected by our chains (Boot defaults); GREEN — `SecurityConfiguration`; asserts the pinned rejection
      status for both; health and prometheus stay anonymous (TC-1, TC-2 regress green).
- [ ] TC-4 Probes and readiness composition: RED — readiness group does not contain `db`/`redis`/`rabbit`
      (in-process `HealthEndpoint` for path `readiness`); GREEN — group config; asserts the three components `UP`,
      liveness and readiness 200 `UP` over HTTP.
- [ ] TC-5 RabbitMQ outage: `ReadinessOutageIntegrationTests` (own context + containers, D-125, D-128); RED —
      without `rabbit` in the group readiness stays 200 after the container stop (checked by temporarily removing it);
      GREEN — readiness 503 `DOWN`, liveness 200 `UP`.
- [ ] TC-6 JSON logs with trace IDs: `StructuredLoggingIntegrationTests` (own context, ECS enabled for it only,
      D-119, D-128); RED — without the OTel tracer the ECS line has no trace/span IDs; GREEN — tracing dependencies +
      sampling; the captured line parses as JSON, has the ECS shape and IDs equal to the current span's.
- [ ] TC-7 Profiles: RED — `ProfileConfigurationTests` expects `ecs` in default and plain text in `local`/`test`;
      GREEN — `application.yml` / profile files; the way to switch a profile back to plain text is verified in the
      Boot 4.1.1 jar first (asked if it needs anything beyond a property).
- [ ] TC-8 Verify: `/simplify`, `./gradlew build` green (incl. jacoco), `/security-review`, diff vs spec and
      D-<n>; CI on the PR green after the user's push.

## Journal (append-only)
- Preconditions: SOL-83 PR #4 merged, `main` == `origin/main`, clean tree (fresh `git fetch`/`status`); Linear SOL-83
  Done, SOL-86 Backlog. Verified in Boot 4.1.1 jars: probes enabled by default, readiness group has no
  `db`/`redis`/`rabbit` by default, web exposure default `health`, default management security permits only health
  (rest formLogin/httpBasic), indicator names `db`/`redis`/`rabbit`.
- Decisions D-118..D-128 recorded (AskUserQuestion, four batches; D-123, D-124 delegated to the agent).
- Spec approved (gate 1). Commit plan: one docs commit on `main` by the user, then the slice branch from the clean
  `main`.

## Report (filled at STOP)

## Retro (-> LESSONS L-<n>)
