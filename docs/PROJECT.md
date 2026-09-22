# GameService — Project Description

Backend of a game service for a **1v1 game**: accounts, Elo-based matchmaking, match history and statistics, a global leaderboard, real-time notifications and telemetry ingestion. The goal is a learning project that stays close to production: a modular monolith used to practice schema design, caching, queues, real-time, observability and load testing.

> Values in this document that the user has not explicitly approved are **drafts** and must be confirmed via AskUserQuestion before implementation (see `AGENTS.md`). Approved decisions are recorded in `docs/DECISIONS.md`; anything not traceable to an active `D-<n>` there is a draft.

---

## 1. Key decisions

Summary of the product decisions. The authoritative, append-only record (with dates, alternatives and superseded entries) is `docs/DECISIONS.md`; when a decision changes, it is changed there first.

| ID | Question | Decision |
|---|---|---|
| [D-1](DECISIONS.md) | Game type | 1v1, result: `WIN` / `LOSS` / `DRAW` |
| [D-2](DECISIONS.md) | Source of match results | Trusted **game server** (service client with role `GAME_SERVER`, client credentials) |
| [D-3](DECISIONS.md) | Tokens | Short-lived **access JWT** (15 min, stateless) + **opaque refresh** (30 days), hash stored in PostgreSQL, rotated on every use, reuse detection |
| [D-4](DECISIONS.md) | Password hashing | **Argon2id** (`Argon2PasswordEncoder`, BouncyCastle) |
| [D-5](DECISIONS.md) | Roles | `PLAYER`, `ADMIN`, `GAME_SERVER` |
| [D-6](DECISIONS.md) | Architecture | Modular monolith, package-by-feature, **Spring Modulith** (module boundary verification in tests, events between modules) |
| [D-7](DECISIONS.md) | Rating | Classic Elo |
| [D-8](DECISIONS.md) | Leaderboard | A single **global** leaderboard by rating: Redis sorted set + SQL variant for comparison |
| [D-9](DECISIONS.md) | Matchmaking | Automatic pairing with an expanding Elo window + **accept** confirmation within N seconds |
| [D-10](DECISIONS.md) | Real-time | WebSocket + **STOMP** |
| [D-11](DECISIONS.md) | Telemetry | Raw events → RabbitMQ → batch insert into **PostgreSQL partitioned by day**, background aggregation |
| [D-12](DECISIONS.md) | Identifiers | **UUIDv7**, generated in the application (Hibernate `@UuidGenerator`, v7 strategy) |
| [D-13](DECISIONS.md) | Database | **PostgreSQL 18** |
| [D-14](DECISIONS.md) | JWT signing | **EdDSA (Ed25519)**, public keys published via **JWKS** (`kid` in the header, key rotation) |
| [D-15](DECISIONS.md) | Leaderboard ties | Equal rating → **whoever reached it first** ranks higher |
| [D-16](DECISIONS.md) | Accept penalty | **Escalating cooldown**: 0 → 1 → 5 → 15 min; decline and ignore count the same; counter resets 1 h after the last refusal |
| [D-17](DECISIONS.md) | Abandoned matches | **Auto-cancel on timeout**: `CREATED` without start → 1 min, `IN_PROGRESS` without result → 24 h; `CANCELLED` with `cancel_reason = TIMEOUT`, ratings unchanged, players notified over STOMP; a late start/result → `409`. `cancel_reason` values: `TIMEOUT`, `SERVER`, `ADMIN` |
| [D-18](DECISIONS.md) | Queue join during cooldown | `409` `ProblemDetail`, `errorCode = MATCHMAKING_COOLDOWN`, with the remaining time |
| [D-19](DECISIONS.md) | Telemetry ids | `telemetry_events.id` is `bigint` (exception from UUIDv7: high-volume append-only table, ids never exposed) |

## 2. Tech stack

| Concern | Technology |
|---|---|
| Platform | Java 21, Spring Boot 4.1, Gradle |
| Web/REST | Spring MVC, Bean Validation, `@RestControllerAdvice` + `ProblemDetail` (RFC 9457) |
| Database | PostgreSQL 18, Spring Data JPA (Hibernate), Flyway |
| Security | Spring Security (OAuth2 Resource Server for JWT validation), EdDSA (Ed25519) JWT + JWKS, Argon2id |
| Leaderboard / cache | Spring Data Redis (sorted sets), Spring Cache (Redis) |
| Messaging | Spring AMQP (RabbitMQ) |
| Real-time | spring-boot-starter-websocket (STOMP) |
| Modularity | Spring Modulith (+ Event Publication Registry as a transactional outbox) |
| Observability | Actuator, Micrometer, Prometheus, Grafana, structured JSON logs |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL, Redis, RabbitMQ) |
| Environment | Docker Compose, GitHub Actions, k6 |

> Spring Boot 4 starters are more modular (and it uses Jackson 3) — verify exact artifact names against the Boot 4.1 documentation when adding them.

## 3. Architecture

### 3.1. Modules

Each module is a top-level package under `com.solarianofc.gameservice`. A module's public API lives at the package root, the implementation in `internal` (inaccessible to other modules; enforced by `ApplicationModules.verify()`).

```
com.solarianofc.gameservice
├── shared/          common concerns: security config, error handling, ProblemDetail, clock, id generation
├── account/         registration, login, refresh/logout, profile, roles, bans, service clients
├── match/           match lifecycle, result intake, match history, player statistics
├── rating/          Elo, current player rating
├── leaderboard/     Redis ZSET, SQL implementation for comparison, page cache
├── matchmaking/     search queue, pairing worker, accept phase
├── notification/    STOMP: "match found" delivery, lobby status
└── telemetry/       batch ingestion, batching, aggregation, partition management
```

Example of a module's internal layout:

```
match/
├── MatchApi.java              public facade for other modules
├── MatchFinished.java         public domain event
└── internal/
    ├── web/                   controllers, request/response DTOs
    ├── domain/                entities, value objects
    ├── persistence/           repositories
    └── service/
```

### 3.2. Module interaction

- **Synchronous** — only through public facades (`*Api`), never through another module's repositories/entities.
- **Asynchronous inside the monolith** — Spring Modulith domain events (`@ApplicationModuleListener`), persisted in the `event_publication` table within the same transaction (guaranteed delivery without distributed transactions).
- **Via RabbitMQ** — where a queue/worker/delay is needed: matchmaking, accept timeouts, telemetry.

Main event flow:

```
game server ──POST result──▶ match ──MatchFinished──▶ rating ──RatingChanged──▶ leaderboard (ZADD)
                                  │                                     └──────▶ profile/stats cache (evict)
                                  └──────────────▶ notification (to players: "match finished, rating ±N")

player ──POST /queue──▶ matchmaking ──RabbitMQ──▶ pairing worker ──MatchProposed──▶ notification (STOMP "match found")
                                                           both accepted ──▶ match.create ──▶ notification (connection details)
```

## 4. Functional requirements by module

### 4.1. account — accounts and authentication

- **Registration**: `username` (3–20 characters, `[A-Za-z0-9_]`, case-insensitively unique), `email` (unique), password (8–128 characters).
- **Login** by username or email → `accessToken` + `refreshToken` pair.
- **Access JWT**: EdDSA (Ed25519) signature, `kid` in the header; public keys published at `/.well-known/jwks.json` so the game server can validate tokens itself; key rotation supported (old keys stay in JWKS until all tokens signed with them expire). Claims: `sub` (userId), `roles`, `iat`, `exp`, `jti`. TTL 15 minutes. Ed25519 support in Spring Security / Nimbus is verified at the start of phase 1.
- **Refresh token**: random 256 bits (opaque), only its SHA-256 hash is stored. TTL 30 days. Rotation: on `/refresh` the old token is marked used and a new one is issued within the same `family`. **Reuse detection**: reusing an already rotated token → the whole family is revoked (all sessions in that chain).
- **Logout**: revoke the current refresh token; `logout-all` — all of them.
- **Bans** (ADMIN): a banned user cannot log in or refresh tokens; active refresh tokens are revoked.
- **Service clients** (`GAME_SERVER`): `client_id` + `client_secret` (Argon2 hash) → short-lived access token with role `GAME_SERVER`.
- **Protection**: rate limiting on login/registration, identical error message for "no such user" and "wrong password", constant response time (dummy hash when the user does not exist).

### 4.2. match — matches, history, statistics

- A match is created by the matchmaking module after both players accept (status `CREATED`).
- The game server reports the start (`IN_PROGRESS`) and the result (`FINISHED`) or cancellation (`CANCELLED`).
- Result intake is **idempotent**: resubmitting the same result → `200` without recalculation; attempting to change the result of a finished match → `409`.
- Status transitions follow a strict state machine: `CREATED → IN_PROGRESS → FINISHED | CANCELLED`, `CREATED → CANCELLED`.
- **Abandoned matches**: a background job cancels matches stuck in `CREATED` for more than **1 minute** (no start) or in `IN_PROGRESS` for more than **24 hours** (no result): status `CANCELLED`, `cancel_reason = TIMEOUT`, ratings unchanged, players are free to queue again and are notified. A late start/result for a timed-out match → `409`.
- **Player match history** — keyset pagination (cursor on `(finished_at, match_id)`), newest first.
- **Player statistics**: matches, wins/losses/draws, win rate, current/peak rating, current win streak; statistics against a specific opponent (head-to-head).
- All key queries are verified with `EXPLAIN (ANALYZE, BUFFERS)` on a seed of ≥ 1M matches; plans are recorded in `docs/perf/`.

### 4.3. rating — Elo

- Starting rating **1000**.
- Expected score: `E_a = 1 / (1 + 10^((R_b − R_a) / 400))`.
- New rating: `R'_a = R_a + K · (S_a − E_a)`, where `S = 1 / 0.5 / 0`.
- `K = 40` for the first 30 matches (calibration), then `K = 20`.
- Recalculation for both players in one transaction with optimistic locking (`@Version`) and retry; `rating_before/rating_after` are written to the match participants.
- Publishes `RatingChanged(userId, oldRating, newRating)`.

### 4.4. leaderboard

- **Redis**: key `lb:global`, `ZADD lb:global <rating> <userId>`. Operations: top N (`ZREVRANGE ... WITHSCORES`), player rank (`ZREVRANK`), player "neighbors" (±N around their rank).
- **Equal ratings**: whoever reached the rating first ranks higher. Redis score is composite: `rating · 10^10 + (MAX_TS − ratingReachedAtSeconds)` (fits into the 53-bit precision of a Redis double score). The SQL implementation orders by `rating DESC, rating_reached_at ASC`. `rating_reached_at` is updated whenever the rating changes.
- Enrichment (username etc.) — batched from PostgreSQL + profile cache.
- **Rebuild**: on startup if the key is empty, and via an admin endpoint — from `player_ratings` (through a temporary key + `RENAME`, atomically).
- **SQL implementation** of the same functionality (index on `rating DESC`, rank via `count(*)` / window functions) behind a switch `leaderboard.backend=redis|sql`.
- **Comparison**: k6 scenario + seed of 1M players, p50/p95/p99 and RPS for top-100, player rank and neighbors; results in `docs/perf/leaderboard.md`.
- **Cache** (Spring Cache, Redis): enriched top-100 page with a 5 s TTL; player profiles and statistics with a 60 s TTL, invalidated on `MatchFinished`/`RatingChanged`.
- **Rate limiting** (Redis, Lua script, sliding window): global per user/IP, separate limits for auth and telemetry. When exceeded — `429` + `Retry-After`.

### 4.5. matchmaking

- A player joins the queue (`POST /api/v1/matchmaking/queue`) — a player cannot be queued twice or while in an active match.
- Queue state — Redis (ZSET `mm:queue` by rating + a hash with the join time).
- **Pairing worker** (tick ~1 s, a single active instance via a Redis distributed lock): each player's acceptable window is `±(50 + 25 · wait_seconds / 5)`, capped at ±400. The pair with the smallest rating difference is chosen.
- A found pair → `MatchProposed` → both players receive "match found" over STOMP and must accept within **15 s**.
- The accept timeout is implemented via RabbitMQ (message TTL + dead-letter exchange). If someone did not accept: the player who accepted returns to the queue keeping their wait time; the one who did not is removed from the queue and penalized.
- **Refusal penalty (escalating cooldown)**: an explicit decline and an ignored accept (timeout) count the same. Refusals within the window: 1st → no cooldown, 2nd → 1 min, 3rd → 5 min, 4th and further → 15 min each. The refusal counter is stored in Redis with a TTL of **1 hour** from the last refusal; the cooldown is a separate Redis key with its own TTL. Joining the queue during a cooldown → `409`, `errorCode = MATCHMAKING_COOLDOWN`, with the remaining time (`cooldownUntil`, `retryAfterSeconds`).
- Both accepted → the match is created and players receive connection details for the game server.
- Metrics: queue size, wait time, rating difference within a pair, share of rejected accepts.

### 4.6. notification — real-time

- Endpoint `/ws`, STOMP protocol. Authentication: JWT in the `CONNECT` header, validated in a `ChannelInterceptor`.
- User queues: `/user/queue/matchmaking` (found/cancelled/timeout), `/user/queue/match` (created, finished, cancelled incl. timeout auto-cancel, rating change).
- Client → server: `/app/matchmaking/accept`, `/app/matchmaking/decline`.
- Initially a simple broker; for horizontal scaling — **STOMP broker relay via RabbitMQ** (`rabbitmq_stomp` plugin).
- Heartbeats, message size limit, rate limiting of inbound messages.

### 4.7. telemetry

- `POST /api/v1/telemetry/events` — a batch of up to 500 events, up to 256 KB. Event: `type`, `clientTimestamp`, `sessionId`, `payload` (JSON).
- The endpoint only validates and publishes the batch to RabbitMQ → `202 Accepted` response (fast intake, no DB write within the request).
- A **worker** consumes the queue, accumulates a batch (by size N or timeout T) and writes to `telemetry_events` via JDBC batch insert / `COPY`. Poison messages → DLQ.
- The table is partitioned by day (`received_at`); partitions are created ahead of time by a background job, partitions older than 30 days are dropped (`DROP` the partition, not `DELETE`).
- **Aggregation**: a background job runs every minute and computes, over closed intervals, event counts and unique users per `(bucket, type)` → `telemetry_aggregates` (upsert, idempotent).
- Metrics: events ingested/s, batch size, queue lag, write duration.

## 5. Data model (PostgreSQL)

PostgreSQL 18. All migrations — Flyway (`src/main/resources/db/migration`, `V{n}__{description}.sql`). Time — `timestamptz` in UTC. Identifiers — `uuid`, **UUIDv7 generated in the application** (Hibernate `@UuidGenerator` v7 strategy; availability verified in phase 1), so ids are known before `INSERT` and time-ordered for B-tree locality. Exception: `telemetry_events.id` stays `bigint` (high-volume append-only table).

```
users
  id PK, username (citext, unique), email (citext, unique), password_hash,
  role, status (ACTIVE/BANNED), created_at, updated_at, version

refresh_tokens
  id PK, user_id FK → users, family_id, token_hash (unique),
  expires_at, used_at, revoked_at, created_at, user_agent, ip
  INDEX (user_id), INDEX (family_id)

service_clients
  id PK, client_id (unique), secret_hash, enabled, created_at

player_ratings
  user_id PK FK → users, rating, rating_reached_at, peak_rating, games_played,
  wins, losses, draws, current_streak, updated_at, version
  INDEX (rating DESC, rating_reached_at ASC)

matches
  id PK, status, created_at, started_at, finished_at, cancel_reason (TIMEOUT / SERVER / ADMIN)
  INDEX (status, created_at) WHERE status IN ('CREATED','IN_PROGRESS')   -- partial, used by the timeout job

match_participants
  match_id FK → matches, user_id FK → users, PK (match_id, user_id),
  result (WIN/LOSS/DRAW), rating_before, rating_after,
  finished_at (denormalized for pagination)
  INDEX (user_id, finished_at DESC, match_id DESC)

telemetry_events  PARTITION BY RANGE (received_at)
  id bigint, user_id, session_id, type, client_ts, received_at, payload jsonb

telemetry_aggregates
  bucket_start, type, events_count, unique_users, PK (bucket_start, type)

event_publication        -- Spring Modulith table (outbox)
```

## 6. REST API (v1)

All responses are JSON, errors are `application/problem+json`.

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | Registration |
| POST | `/api/v1/auth/login` | public | Login → tokens |
| POST | `/api/v1/auth/refresh` | public (refresh) | Token rotation |
| POST | `/api/v1/auth/logout` | PLAYER | Revoke current refresh token |
| POST | `/api/v1/auth/logout-all` | PLAYER | Revoke all sessions |
| POST | `/api/v1/auth/service-token` | client credentials | Token for the game server |
| GET | `/.well-known/jwks.json` | public | Public keys for JWT validation |
| GET | `/api/v1/players/me` | PLAYER | Own profile |
| GET | `/api/v1/players/{id}` | PLAYER | Public profile |
| GET | `/api/v1/players/{id}/stats` | PLAYER | Statistics |
| GET | `/api/v1/players/{id}/matches?cursor=&limit=` | PLAYER | History (keyset) |
| GET | `/api/v1/players/{id}/head-to-head/{opponentId}` | PLAYER | Statistics against an opponent |
| GET | `/api/v1/matches/{id}` | PLAYER | Match details |
| POST | `/api/v1/matches/{id}/start` | GAME_SERVER | Match started |
| POST | `/api/v1/matches/{id}/result` | GAME_SERVER | Result (idempotent) |
| POST | `/api/v1/matches/{id}/cancel` | GAME_SERVER, ADMIN | Cancellation |
| GET | `/api/v1/leaderboard?offset=&limit=` | PLAYER | Top |
| GET | `/api/v1/leaderboard/me?around=` | PLAYER | Own rank and neighbors |
| POST | `/api/v1/matchmaking/queue` | PLAYER | Join the queue |
| DELETE | `/api/v1/matchmaking/queue` | PLAYER | Leave the queue |
| GET | `/api/v1/matchmaking/status` | PLAYER | Search status |
| POST | `/api/v1/telemetry/events` | PLAYER | Event batch |
| POST | `/api/v1/admin/players/{id}/ban` | ADMIN | Ban |
| POST | `/api/v1/admin/leaderboard/rebuild` | ADMIN | Rebuild the leaderboard |

### Errors

`@RestControllerAdvice` maps all errors to `ProblemDetail`:

```json
{
  "type": "https://gameservice.local/problems/validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request contains invalid fields",
  "instance": "/api/v1/auth/register",
  "errorCode": "VALIDATION_ERROR",
  "traceId": "4bf92f3577b34da6",
  "errors": [{ "field": "username", "message": "size must be between 3 and 20" }]
}
```

Standard codes: `400` validation, `401` missing/invalid token, `403` insufficient rights/banned, `404`, `409` state conflict (duplicate, invalid status transition), `429` rate limit, `500` without leaking details.

## 7. Security

- Stateless sessions, CSRF disabled (Bearer tokens only), CORS — explicit allowlist.
- Secrets (Ed25519 private keys, DB passwords, client secrets) — only via environment variables, never in the repository.
- Argon2id with parameters following OWASP recommendations; on login the hash is recomputed if the parameters are outdated (`upgradeEncoding`).
- Rate limiting on auth endpoints and telemetry; request body size limit.
- Spring Security default security headers; Actuator exposes only `health` and `prometheus` externally (the latter on a separate port/network).
- Logs contain no passwords, tokens or full telemetry payloads.

## 8. Operations and observability

- **Docker Compose**: `app`, `postgres`, `redis`, `rabbitmq` (management + stomp plugins), `prometheus`, `grafana` (datasource and dashboard provisioning from the repository).
- **Logs**: structured JSON (Spring Boot built-in structured logging) with `traceId`/`spanId`, `userId`, `matchId` via MDC.
- **Metrics**: standard (HTTP, JVM, HikariCP, Redis, RabbitMQ) + business metrics:
  `gs.matchmaking.queue.size`, `gs.matchmaking.wait.time`, `gs.matchmaking.accept.timeouts`, `gs.matches.finished`, `gs.rating.delta`, `gs.leaderboard.query.time{backend}`, `gs.telemetry.events.ingested`, `gs.telemetry.batch.size`, `gs.ratelimit.rejected`.
- **Grafana**: dashboards "API" (RPS, latency, errors), "Matchmaking", "Telemetry", "JVM/infrastructure".
- **Health**: liveness/readiness probes; readiness accounts for the DB, Redis and RabbitMQ.

## 9. Testing

Sensors, thresholds and the development loop are defined in `docs/HARNESS.md`.

- **Unit** (JUnit 5, Mockito): Elo, matchmaking window, match state machine, refresh rotation, validation.
- **Module tests** (Spring Modulith `@ApplicationModuleTest`, `Scenario`): publishing and handling events within a module.
- **Architecture tests**: `ApplicationModules.of(GameServiceApplication.class).verify()`, ArchUnit rules.
- **Integration** (Testcontainers: PostgreSQL, Redis, RabbitMQ; `@ServiceConnection`): end-to-end REST, Flyway migrations, STOMP client for real-time, workers.
- **Load** (k6, `load/`): login, leaderboard (redis vs sql), telemetry (batches), matchmaking (virtual players join the queue and accept the match).
- **CI** (GitHub Actions): `./gradlew build` (including integration tests), Gradle cache, test report publishing; a separate manual workflow for k6.

## 10. Non-functional targets

Targets for a local environment (to be refined after the first measurements):

- Leaderboard reads (Redis): p95 < 20 ms at 1000 RPS.
- Telemetry intake: ≥ 5,000 events/s, response p95 < 50 ms.
- Match history: p95 < 50 ms on a 1M-match seed, plan without Seq Scan.
- Matchmaking: with 1,000 players in the queue, median wait time < 10 s.

## 11. Roadmap

Each phase ends with a green `./gradlew build`, updated documentation and (where applicable) integration tests.

### Phase 0 — Skeleton
- Dependencies, profiles (`local`, `test`), Docker Compose (postgres, redis, rabbitmq).
- `shared`: global `@RestControllerAdvice` + `ProblemDetail`, structured logs, Actuator.
- Spring Modulith + architecture test; base Testcontainers test.
- Sensors from `docs/HARNESS.md` (Spotless, Error Prone/NullAway, Checkstyle, PMD, SpotBugs, JaCoCo).
- GitHub Actions: build and tests.

**DoD**: the application starts in Compose, `/actuator/health` = UP, CI is green.

### Phase 1 — Accounts and authentication
- Migrations `users`, `refresh_tokens`, `service_clients`.
- Registration, login, refresh with rotation and reuse detection, logout, roles, service-token.
- Rate limiting on auth.

**DoD**: integration tests for all auth scenarios, including refresh token reuse.

### Phase 2 — Matches, rating, statistics
- Migrations `matches`, `match_participants`, `player_ratings`.
- Game server API, state machine, idempotent result intake, Elo, events.
- History (keyset), statistics, head-to-head.
- Seed script for 1M matches, `EXPLAIN ANALYZE` of key queries → `docs/perf/`.

**DoD**: query plans use indexes; tests for Elo recalculation and concurrent result intake.

### Phase 3 — Leaderboard and cache
- Redis ZSET, SQL implementation, switch, rebuild.
- Spring Cache with TTL and event-driven invalidation.
- Redis vs SQL comparison with k6 → `docs/perf/leaderboard.md`.

**DoD**: benchmark report, consistency test between the ZSET and `player_ratings`.

### Phase 4 — Matchmaking
- Queue in Redis, pairing worker with a distributed lock, RabbitMQ (requests, timeouts via TTL + DLX).
- Accept phase, return to queue, match creation.

**DoD**: integration test for the full "queue → pair → accept → match" cycle, timeout test.

### Phase 5 — Real-time
- STOMP endpoint, JWT in CONNECT, user queues, accept/decline over WebSocket.
- Switch to the RabbitMQ STOMP relay.

**DoD**: integration test with a STOMP client receiving "match found".

### Phase 6 — Telemetry
- Batch intake, publishing to RabbitMQ, batch worker, DLQ.
- Partitioning, partition management, aggregation.

**DoD**: k6 intake test, aggregates match the raw data.

### Phase 7 — Observability and load
- Business metrics, Prometheus + Grafana with dashboards from the repository.
- Full set of k6 scenarios, load report, profiling of bottlenecks.

**DoD**: dashboards show all key metrics under k6 load, report in `docs/perf/`.

## 12. Open questions

None at the moment. All previously open questions were resolved on 2026-09-22 (see §1 and `docs/DECISIONS.md`). Values elsewhere in this document that are not traceable to an active `D-<n>` are still drafts.
