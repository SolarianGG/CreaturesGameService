# SOL-81 — Docker Compose: postgres, redis, rabbitmq, app
Linear: https://linear.app/solarianofc/issue/SOL-81/docker-compose-postgres-redis-rabbitmq-app
Status: done | Phase: 0
Spec approved: 2026-09-24

## Goal
`docker compose up --build` on a clean machine starts PostgreSQL, Redis, RabbitMQ (management + STOMP) and the
application; the application becomes healthy and `/actuator/health` is `UP`. The management port stays inside the
Compose network (D-122), credentials come only from a git-ignored `.env`. A CI job keeps this true on every PR.

## Scope
- `compose.yaml` (repository root) with services `postgres`, `redis`, `rabbitmq`, `app`:
  - images `postgres:18-alpine`, `redis:8-alpine`, `rabbitmq:4-management-alpine` (D-193);
  - ports bound to `127.0.0.1`: `5432`, `6379`, `5672`, `15672`, `61613`; `app` publishes `8080` only, `8081` is not
    published (D-194);
  - credentials from `.env` without defaults, `${VAR:?...}` (D-196), names `POSTGRES_DB`, `POSTGRES_USER`,
    `POSTGRES_PASSWORD`, `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS` (D-204);
  - `app` without a profile; `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`, `SPRING_DATA_REDIS_HOST`,
    `SPRING_RABBITMQ_HOST` / `_USERNAME` / `_PASSWORD` built from `.env` and the service names (D-197);
  - named volume for PostgreSQL data only (D-198);
  - RabbitMQ plugins from `docker/rabbitmq/enabled_plugins` mounted read-only (D-201);
  - healthchecks on all four services, `app` depends on the three with `service_healthy`, no restart policy (D-203);
    timings interval 5s / timeout 3s / retries 10, `app` start period 60s (D-206).
- `Dockerfile` (multi-stage, D-192, D-199, D-205): `eclipse-temurin:21-jdk-alpine` runs `./gradlew bootJar --no-daemon`
  (through `sh`, the checkout has no executable bit, D-81) with a cache mount for `/root/.gradle`;
  `eclipse-temurin:21-jre-alpine` runs the boot jar as an unprivileged user; `HEALTHCHECK`-equivalent in Compose via
  BusyBox `wget` on `http://localhost:8081/actuator/health/readiness`. `.dockerignore` (build output, IDE files,
  `.git`, `.env`).
- `.env.example` committed (all values `gameservice`), `.env` added to `.gitignore` (D-196, D-204).
- `application-local.yml`: RabbitMQ user / password `gameservice` (D-200); `ProfileConfigurationTests` updated.
- CI: job `compose` in `.github/workflows/build.yml`, parallel to `build`: `cp .env.example .env`,
  `docker compose up --build --wait --wait-timeout 180`, health of `app` checked, logs on failure,
  `docker compose down -v`; `timeout-minutes: 20` (D-195, D-202, D-206).
- Docs: `docs/PROJECT.md` §8 Docker Compose (services, ports, `.env`, commands, links D-192..D-207).

## Out of scope
- Prometheus and Grafana services (SOL-133); Steam variables in `.env.example` (SOL-143, D-147).
- Spring Boot `spring-boot-docker-compose` auto-start from the IDE; production deployment; image publishing.
- Redis password; TLS; resource limits for containers.

## Acceptance criteria
- `docker compose up --build --wait` with `.env` copied from `.env.example` ends with all four services healthy;
  inside the network `GET http://app:8081/actuator/health` answers `{"status":"UP"}` and readiness shows `db`,
  `redis`, `rabbit` `UP`.
- Without `.env` `docker compose config` fails and names the missing variable.
- From the host: `8080` answers (401 `UNAUTHORIZED` problem+json — the application chain denies all, D-124); `8081`
  is not reachable; the infrastructure ports are bound to `127.0.0.1` only.
- RabbitMQ has `rabbitmq_stomp` enabled and listens on `61613`.
- The application in the container runs as a non-root user and writes ECS JSON logs.
- The `local` profile run from the host (`./gradlew bootRun --args=--spring.profiles.active=local`) against the
  Compose services is ready (D-200).
- CI job `compose` is green on the PR; a deliberate break fails the same command (D-47 proof in the journal).
- `./gradlew build` green locally and in CI.

## Decisions
D-47, D-81, D-118, D-122, D-124, D-125, D-147, D-192..D-207 (D-200 supersedes D-86)

## Test cases
Acceptance level: the real Docker CLI (`docker compose ...`, `docker compose exec`, `curl` from the host) — the same
commands a developer and the CI job run. Negative cases assert the specific reason (the missing variable name,
connection refused, the bound address), not only a non-zero exit code. Each command and its output go to the journal.

- [x] TC-1 Local profile credentials (D-200): RED — `ProfileConfigurationTests` expects RabbitMQ `gameservice` /
      `gameservice` -> fails with `guest`; GREEN — `application-local.yml` changed.
- [x] TC-2 Compose configuration and secrets (D-196, D-204): RED — `docker compose config` fails (no `compose.yaml`);
      GREEN — `compose.yaml` (infrastructure services), `.env.example`, `.gitignore` `.env`; with `.env` the config
      renders; without `.env` it fails naming `POSTGRES_PASSWORD` (or the first missing variable); `git check-ignore .env`.
- [x] TC-3 Infrastructure services (D-193, D-194, D-198, D-201, D-203): `docker compose up --wait postgres redis
      rabbitmq` -> healthy; `docker compose port` shows `127.0.0.1` for every published port; `rabbitmq-plugins list -E`
      contains `rabbitmq_stomp`, `61613` accepts TCP; the PostgreSQL volume exists; `local` profile from the host is
      ready (readiness `UP` on `localhost:8081`).
- [x] TC-4 Application image and service (D-192, D-197, D-199, D-205, D-206): RED — `docker compose up --build --wait`
      fails (no `app` / `Dockerfile`); GREEN — `Dockerfile`, `.dockerignore`, `app` service; all services healthy;
      `/actuator/health` `UP` and readiness `db` / `redis` / `rabbit` `UP` via `docker compose exec app wget`;
      `id -u` in `app` is not `0`; the first `app` log line parses as ECS JSON.
- [x] TC-5 Port exposure (D-194, D-122): from the host `curl localhost:8081` -> connection refused;
      `docker compose port app 8081` -> nothing published; `curl localhost:8080/` -> 401 `UNAUTHORIZED` problem+json.
- [~] TC-6 CI job (D-195, D-202, D-206): job `compose` added; D-47 proof — a deliberate break (wrong
      `SPRING_DATASOURCE_PASSWORD` for `app`) makes the job's command fail with `app` unhealthy -> reverted -> green;
      CI `compose` and `build` green on the PR.
- [ ] TC-7 Verify: docs (`PROJECT.md` §8, D-207), `/simplify`, `./gradlew build`, `/security-review` (secrets and port
      exposure), diff vs spec and active D-<n>.

## Journal (append-only)
- 2026-09-24 Spec drafted from SOL-81; draft values approved as D-192..D-207 (D-200 supersedes D-86: RabbitMQ
  accepts `guest` only over loopback).
- 2026-09-24 Spec approved (gate 1) incl. `/security-review` in TC-7; branch `slice/SOL-81-docker-compose` from `main`.
- 2026-09-24 TC-1 RED: `localProfileConnectsToLocalhostServices` -> rabbitmq username / password `guest` not expected.
  GREEN: `application-local.yml` RabbitMQ `gameservice` / `gameservice` (D-200). Checkpoint: 9 profile tests, PMD green.
- 2026-09-24 TC-2 RED: `docker compose config` -> exit 14 `no configuration file provided: not found`.
  GREEN: `compose.yaml` (postgres, redis, rabbitmq), `.env.example`, `docker/rabbitmq/enabled_plugins`, `.gitignore`
  `.env`. Without `.env`: exit 15 `required variable POSTGRES_PASSWORD is missing a value: set it in .env (copy
  .env.example)`. With `.env` (copied): exit 0, every published port `host_ip: 127.0.0.1`;
  `git check-ignore -v .env` -> `.gitignore:39:.env`. Working tree files are CRLF (`core.autocrlf=true`), the
  rendered values have no `\r`.
- 2026-09-24 TC-3: `docker compose up --wait --wait-timeout 180 postgres redis rabbitmq` -> exit 0, all three healthy
  in ~6 s; ports `127.0.0.1:5432`, `:6379`, `:5672`, `:15672`, `:61613` (nothing on `0.0.0.0`).
  `rabbitmq-plugins list -E` -> `rabbitmq_management`, `rabbitmq_stomp` 4.3.6 (CRLF `enabled_plugins` parses);
  listeners incl. `61613 stomp`; TCP connect to `127.0.0.1:61613` ok. Volume `gameservice_postgres-data`, `PGDATA`
  `/var/lib/postgresql/18/docker` inside it. `./gradlew bootRun --args=--spring.profiles.active=local` from the host
  -> `localhost:8081/actuator/health/readiness` 200 `{"status":"UP"}` (D-200 works end-to-end); process stopped.
- 2026-09-24 TC-4 RED: `docker compose up --build --wait --wait-timeout 180 app` -> exit 1 `no such service: app`.
  While writing `app`: asked the interface of `8080` (D-194 wording open) -> D-208 `127.0.0.1:8080`.
- 2026-09-24 compose up --build attempt 1/3 FAIL: `./gradlew bootJar` in the build stage failed after 11m 17s with
  a Gradle error naming `repo.maven.apache.org`; the full reason was lost (output cut to the last 25 lines).
  Maven Central answers from an `eclipse-temurin:21-jdk-alpine` container in 1.4 s -> rebuild with a full plain log.
- 2026-09-24 compose up --build attempt 2/3: `docker compose build --progress=plain app` -> `BUILD SUCCESSFUL in 4m 11s`
  (no code change; the first failure was transient, cause not recoverable -> L-37).
- 2026-09-24 TC-4 GREEN: `Dockerfile`, `.dockerignore`, `app` service. `docker compose up --wait --wait-timeout 180`
  -> exit 0, all four healthy; `app` ports `127.0.0.1:8080->8080/tcp, 8081/tcp` (8081 not published).
  In `app`: `/actuator/health` -> `{"groups":["liveness","readiness"],"status":"UP"}`, readiness `{"status":"UP"}`;
  `id` -> `uid=100(gameservice)`; every log line is JSON, first line ECS (`log.level` INFO, `service.name`
  GameService, `ecs.version` 8.11). Readiness details are not exposed, so the composition was shown by an outage:
  `docker compose stop rabbitmq` -> readiness `HTTP/1.1 503`; started again -> `UP`.
- 2026-09-24 TC-5: from the host `curl localhost:8081` and `127.0.0.1:8081` -> exit 7 `Couldn't connect to server`;
  `docker compose port app 8081` -> `:0` (not published); `docker compose port app 8080` -> `127.0.0.1:8080`;
  `curl -i localhost:8080/` -> `401`, `application/problem+json`, `errorCode` `UNAUTHORIZED`. Inside the network
  (`rabbitmq` container) `http://app:8081/actuator/health` -> `UP`, so the port is isolated, not broken.
- 2026-09-24 TC-6: job `compose` in `build.yml` (checkout, `cp .env.example .env`, `up --build --wait
  --wait-timeout 180`, `exec -T app wget ... | grep -q '"status":"UP"'`, logs on failure, `down -v` always).
  Job commands run locally from `down -v`: `up exit=0`, `health check exit=0`.
  D-47 proof: the Postgres volume keeps the password it was initialised with, so `POSTGRES_PASSWORD=wrong` in the shell
  breaks only `app`: `container gameservice-app-1 exited (1)`, `up exit=1`, health check exit 1 (`service "app" is
  not running`); `app` log `PSQLException: FATAL: password authentication failed for user`. Reverted (`down -v`,
  normal `.env`) -> `up exit=0`, `health check exit=0`. CI on the PR still open.
- 2026-09-24 TC-7 started: `docs/PROJECT.md` §8 Docker Compose (run, `.env`, images, ports, healthchecks, CI; links
  D-122, D-192..D-208); `/simplify` running (4 review agents: reuse, simplification, efficiency, altitude).
- 2026-09-24 /simplify (4 agents) -> asked -> D-209 (`.gitattributes` LF for Dockerfile / compose.yaml / .env.example /
  docker/**), D-210 (test `.env.example` == `local` profile + `test` task input), D-211 (not applied: 9 `:?`
  messages, CI Docker cache, healthcheck anchor, fixed `bootJar` name). Applied without a decision: Dockerfile
  copies `src/main` only; dropped the hypothetical `cp` comment and the repeated 8081 comment in the Dockerfile.
  D-210 test green at once; discrimination (L-36): `RABBITMQ_DEFAULT_PASS=drift` in `.env.example`, run without
  `--rerun` -> reran (input works) and failed `"spring.rabbitmq.password"="drift"` not found -> reverted -> green.
- 2026-09-24 pmdTest attempt 1/3: `AvoidDuplicateLiterals` "local" x4 -> constant `LOCAL`; compile + 10 profile
  tests + PMD green together (L-35).
- 2026-09-24 `./gradlew build` attempt 1: BUILD SUCCESSFUL in 2m 12s (117 tests, spotless, checkstyle, PMD, SpotBugs,
  JaCoCo lines 97% / branches 80%). Image rebuilt after the Dockerfile change (`COPY src/main src/main`); CI job
  commands from `down -v`: `up exit=0`, `health check exit=0`. Next: `/security-review`.
- 2026-09-24 `/security-review` (sub-agent on the working-tree diff): no findings — every published port on
  `127.0.0.1`, 8081 unpublished, credentials only from `.env` without defaults, Dockerfile copies named paths only
  (no `.env` / sources / Gradle cache in the runtime image), non-root exec-form entrypoint, CI `pull_request` (no
  `pull_request_target`), `contents: read`, no secrets. Next: diff vs spec, close.
- 2026-09-24 Diff vs spec and active D-<n> (fresh `git status` / `git diff HEAD`, `main` == `origin/main` e4691df):
  every change traces to the spec or D-192..D-211. Beyond the frozen spec wording: `8080` on `127.0.0.1` (D-208),
  `.gitattributes` LF rules (D-209), `.env.example` test + `test` task input in `build.gradle` (D-210), Dockerfile
  copies `src/main` only (/simplify, D-211); readiness composition shown by a RabbitMQ outage instead of listed
  components (details are not exposed). `git check-attr eol` -> `lf` for the four container files. TC-6 stays [~]
  until CI runs on the PR.
- 2026-09-24 Close: harness proposals approved -> D-212 (HARNESS §2.1 row for the CI `compose` sensor), D-213
  (`.env.example`, `.dockerignore`, `docker/**` protected in HARNESS §3 and `protect_configs.py`). Hook proof (L-4):
  a real Edit of `.dockerignore` showed the "is a config file" confirmation (user confirmed). Lessons L-37, L-38.
  Linear replication on the user's request.
- 2026-09-24 Linear: SOL-81 -> In Review, description = approved spec with amendments marked, report comment added;
  no deferred issues.

## Report
- Done: `compose.yaml` with `postgres`, `redis`, `rabbitmq` (management + STOMP) and `app`; multi-stage `Dockerfile`
  (Temurin 21 Alpine, Gradle cache mount, non-root) and `.dockerignore`; credentials only from the git-ignored `.env`
  (`.env.example` committed, no defaults); every port on `127.0.0.1`, management port `8081` not published (network
  isolation of D-122); healthchecks on all services, `app` waits for healthy infrastructure; CI job `compose`;
  `local` profile RabbitMQ user `gameservice` (D-200) and a test that keeps it equal to `.env.example`;
  `docs/PROJECT.md` §8.
- Sensors: `./gradlew build` green (117 tests, spotless, checkstyle, PMD, SpotBugs, JaCoCo lines 97% / branches 80%);
  CI job commands green locally; D-47 proof of the `compose` job (wrong app DB password -> `app` exits,
  `PSQLException: FATAL: password authentication failed`); D-210 test proven by a deliberate drift. Fix attempts:
  compose up --build 1/3 (transient, L-37), pmdTest 1/3. `/security-review`: no findings.
- Decisions added: D-192..D-211 (D-200 supersedes D-86; D-208 while building; D-209..D-211 after /simplify).
- Deviations from PROJECT.md / spec wording: see the diff-vs-spec journal line; PROJECT.md §8 updated.
- Deferred: none (CI Docker layer cache: "nothing for now", D-211).

## Retro
- Went wrong: the first image build failed and its cause was lost, because the output was cut to a tail (L-37).
- Worked: checking an assumption against the real broker before coding — RabbitMQ's loopback-only `guest` would have
  broken the `local` profile against Compose (D-200); proving a sensor without editing files by using state the
  system already keeps (the Postgres volume keeps its first password) and proving readiness composition with an
  outage when the details are hidden (L-38).
- Loop changes: proposed at close — HARNESS §2.1 row for the CI `compose` sensor; protection of the new
  infrastructure files in §3.
