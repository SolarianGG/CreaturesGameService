# GameService — Decisions Log

The single source of truth for decisions approved by the user. Anything in `docs/PROJECT.md` or `docs/HARNESS.md` that cannot be traced to an **active** `D-<n>` entry is a **draft** and must be confirmed via AskUserQuestion before it is implemented.

## Process

- **When to record:** immediately after the user approves a decision (AskUserQuestion answer or explicit chat approval). Documents that describe the decision (`PROJECT.md`, `HARNESS.md`, slice files) link to its `D-<n>`.
- **Append-only:** entries are never rewritten. A changed decision is a new entry with `Supersedes: D-<old>`; the old entry only gets its `Status` switched to `superseded by D-<new>`.
- **Alternatives:** the options the user did not choose. `not recorded` for decisions migrated from earlier documents, where the alternatives were not written down.

**Entry format:**

```
### D-<number> — <short title>
- Date: YYYY-MM-DD
- Area: product | account | match | rating | leaderboard | matchmaking | realtime | telemetry | architecture | harness
- Decision: what was decided
- Alternatives: options not chosen | not recorded
- Source: user (AskUserQuestion) | user (chat) | migrated from <document>
- Supersedes: - | D-<n>
- Status: active | superseded by D-<n>
```

---

## Entries

### D-1 — Game type
- Date: 2026-09-22
- Area: product
- Decision: 1v1 game; match result is `WIN` / `LOSS` / `DRAW`.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-2 — Source of match results
- Date: 2026-09-22
- Area: match
- Decision: a trusted **game server** reports results — a service client with role `GAME_SERVER`, client credentials.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-3 — Tokens
- Date: 2026-09-22
- Area: account
- Decision: short-lived **access JWT** (15 min, stateless) + **opaque refresh token** (30 days); the refresh token hash is stored in PostgreSQL, rotated on every use, with reuse detection.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-4 — Password hashing
- Date: 2026-09-22
- Area: account
- Decision: **Argon2id** (`Argon2PasswordEncoder`, BouncyCastle).
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-5 — Roles
- Date: 2026-09-22
- Area: account
- Decision: `PLAYER`, `ADMIN`, `GAME_SERVER`.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-6 — Architecture
- Date: 2026-09-22
- Area: architecture
- Decision: modular monolith, package-by-feature, **Spring Modulith** (module boundary verification in tests, events between modules).
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-7 — Rating
- Date: 2026-09-22
- Area: rating
- Decision: classic Elo.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-8 — Leaderboard
- Date: 2026-09-22
- Area: leaderboard
- Decision: a single **global** leaderboard by rating: Redis sorted set + SQL variant for comparison.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-9 — Matchmaking
- Date: 2026-09-22
- Area: matchmaking
- Decision: automatic pairing with an expanding Elo window + **accept** confirmation within N seconds.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-10 — Real-time
- Date: 2026-09-22
- Area: realtime
- Decision: WebSocket + **STOMP**.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-11 — Telemetry pipeline
- Date: 2026-09-22
- Area: telemetry
- Decision: raw events → RabbitMQ → batch insert into **PostgreSQL partitioned by day**, background aggregation.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-12 — Identifiers
- Date: 2026-09-22
- Area: architecture
- Decision: **UUIDv7**, generated in the application (Hibernate `@UuidGenerator`, v7 strategy).
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-13 — Database
- Date: 2026-09-22
- Area: architecture
- Decision: **PostgreSQL 18**.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-14 — JWT signing
- Date: 2026-09-22
- Area: account
- Decision: **EdDSA (Ed25519)**, public keys published via **JWKS** (`kid` in the header, key rotation).
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-15 — Leaderboard ties
- Date: 2026-09-22
- Area: leaderboard
- Decision: equal rating → **whoever reached it first** ranks higher.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-16 — Accept penalty
- Date: 2026-09-22
- Area: matchmaking
- Decision: **escalating cooldown** 0 → 1 → 5 → 15 min; decline and ignore count the same; the counter resets 1 h after the last refusal.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-17 — Abandoned matches
- Date: 2026-09-22
- Area: match
- Decision: **auto-cancel on timeout**: `CREATED` without start → 1 min, `IN_PROGRESS` without result → 24 h; `CANCELLED` with `cancel_reason = TIMEOUT`, ratings unchanged, players notified over STOMP; a late start/result → `409`. `cancel_reason` values: `TIMEOUT`, `SERVER`, `ADMIN`.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-18 — Queue join during cooldown
- Date: 2026-09-22
- Area: matchmaking
- Decision: `409` `ProblemDetail`, `errorCode = MATCHMAKING_COOLDOWN`, with the remaining time.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-19 — Telemetry ids
- Date: 2026-09-22
- Area: telemetry
- Decision: `telemetry_events.id` is `bigint` — an exception from UUIDv7: high-volume append-only table, ids never exposed.
- Alternatives: not recorded
- Source: migrated from `docs/PROJECT.md` §1
- Supersedes: -
- Status: active

### D-20 — No assumptions
- Date: 2026-09-22
- Area: harness
- Decision: anything the agent is not sure about is asked via AskUserQuestion; "sensible defaults" are offered as options, never picked silently; unapproved values are drafts.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §1 (see L-1)
- Supersedes: -
- Status: active

### D-21 — English only
- Date: 2026-09-22
- Area: harness
- Decision: everything in the repository, commit messages, pull requests and issues is in English; chat with the user is not affected.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §1 (see L-2)
- Supersedes: -
- Status: active

### D-22 — Computational sensor set
- Date: 2026-09-22
- Area: harness
- Decision: Spotless + palantir-java-format, Error Prone + NullAway, Checkstyle (minimal custom rule set), PMD (minimal custom rule set: bestpractices + errorprone), SpotBugs, Spring Modulith verify, ArchUnit (each rule agreed with the user), Flyway validate, JUnit 5 / Mockito / Testcontainers, JaCoCo. Exact rule lists, plugin versions, NullAway and SpotBugs settings are still drafts, to be agreed in phase 0.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §2.1
- Supersedes: -
- Status: active

### D-23 — Zero tolerance
- Date: 2026-09-22
- Area: harness
- Decision: any analyzer violation fails the build (warnings = errors); suppressions only locally, with a justification comment and the user's approval.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §2.1
- Supersedes: -
- Status: active

### D-24 — Coverage thresholds
- Date: 2026-09-22
- Area: harness
- Decision: JaCoCo ≥ 70% lines, ≥ 60% branches; below fails the build.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §2.1
- Supersedes: -
- Status: active

### D-25 — Tools deliberately not selected
- Date: 2026-09-22
- Area: harness
- Decision: no PIT (mutation testing), no separate unit/integration test tasks, no OWASP Dependency-Check, no generated module documentation.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §2.1
- Supersedes: -
- Status: active

### D-26 — Inferential sensor
- Date: 2026-09-22
- Area: harness
- Decision: `/security-review` is mandatory at the end of a slice if `account` or security code in `shared` was touched.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §2.2
- Supersedes: -
- Status: active

### D-27 — Agent permissions
- Date: 2026-09-22
- Area: harness
- Decision: allow Gradle tasks and read-only git (`status`, `diff`, `log`, `show`, `blame`, `branch` listing); deny `git push`, `git reset --hard`, any git command with `--force`; ask for everything else.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §3
- Supersedes: -
- Status: active

### D-28 — Commits
- Date: 2026-09-22
- Area: harness
- Decision: the agent proposes a commit message, the user commits.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §3
- Supersedes: -
- Status: active

### D-29 — Protected config files
- Date: 2026-09-22
- Area: harness
- Decision: editing build files, application configs (main **and** test resources), infrastructure/CI files, `config/**`, `.claude/**` and `AGENTS.md` requires confirmation (PreToolUse hook `protect_configs.py`); config files are never edited through shell commands.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §3 (test resources: see L-6)
- Supersedes: -
- Status: active

### D-30 — Limits
- Date: 2026-09-22
- Area: harness
- Decision: max 3 consecutive fix attempts per failing sensor, then stop and ask; one vertical slice per cycle, then stop for user review.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §4
- Supersedes: -
- Status: active

### D-31 — Lessons log
- Date: 2026-09-22
- Area: harness
- Decision: mistakes and successes are recorded in `docs/LESSONS.md`; important lessons are promoted to `AGENTS.md` only with user approval.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §6
- Supersedes: -
- Status: active

### D-32 — Formatting and config-protection hooks
- Date: 2026-09-22
- Area: harness
- Decision: `protect_configs.py` (PreToolUse, Write/Edit/NotebookEdit) and `spotless_apply.py` (PostToolUse, Write/Edit, `.java` only), written in Python.
- Alternatives: not recorded
- Source: migrated from `docs/HARNESS.md` §7
- Supersedes: -
- Status: active

### D-33 — Memory and state live in repository files
- Date: 2026-09-22
- Area: harness
- Decision: project memory and state are repository files: `docs/STATE.md` (current state), `docs/slices/NN-<name>.md` (slice working memory), `docs/DECISIONS.md` (approved decisions), `docs/LESSONS.md` (lessons).
- Alternatives: Linear (phases as projects, slices as issues); hybrid (backlog in Linear, slice files in the repo)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-34 — Decisions log
- Date: 2026-09-22
- Area: harness
- Decision: approved decisions are kept in `docs/DECISIONS.md` as append-only short blocks (this format). All decisions from `docs/PROJECT.md` §1 and the approved harness decisions are migrated; `PROJECT.md` §1 becomes a summary that links to `D-<n>`.
- Alternatives: keep decisions in `PROJECT.md` §1; one ADR file per decision (`docs/adr/`); table format; log only new decisions; migrate only `PROJECT.md` §1
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-35 — Agent auto-memory scope
- Date: 2026-09-22
- Area: harness
- Decision: the agent's auto-memory (outside the repository) keeps only personal preferences that are not project facts; duplicates of repository rules are removed. The repository is the source of truth.
- Alternatives: keep duplicates as a safety net; do not use auto-memory at all
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-36 — STATE.md format
- Date: 2026-09-22
- Area: harness
- Decision: compact format — position (phase, active slice, loop step), next action, blockers / waiting for user, backlog of the current phase. Details live in the slice file.
- Alternatives: extended format with last sensor results and completed-slice history
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-37 — Slice file format
- Date: 2026-09-22
- Area: harness
- Decision: `docs/slices/NN-<name>.md` = approved spec (goal, scope / out of scope, acceptance criteria, decision links; not changed without user approval) + test-case checklist + append-only journal (including fix attempts per sensor, so the 3-attempt limit survives `/clear`) + report and retro filled at STOP.
- Alternatives: spec + checklist only, without a journal
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-49

### D-38 — SessionStart state injection
- Date: 2026-09-22
- Area: harness
- Decision: a SessionStart hook injects `docs/STATE.md` and the active slice file referenced in it on `startup`, `resume`, `clear` and `compact`. `DECISIONS.md` and `LESSONS.md` are not injected — read on demand.
- Alternatives: inject STATE.md only; also inject the list of active decisions
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-39 — Stop hook for stale state
- Date: 2026-09-22
- Area: harness
- Decision: a Stop hook blocks stopping once (exit 2) when a changed file (per `git status`) under `src/`, `build.gradle` or `docs/` is newer (mtime) than `docs/STATE.md` or the active slice file; it lets the agent stop if it already blocked in the same turn (`stop_hook_active`).
- Alternatives: non-blocking warning to the user; tracking edits per session with an extra PostToolUse hook
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-40 — Checkpoints instead of PreCompact
- Date: 2026-09-22
- Area: harness
- Decision: no PreCompact hook (it cannot block compaction or make the agent act). State is saved continuously: the slice file is updated after every test case, decision and sensor fix attempt; after compaction the SessionStart hook (`compact`) re-injects the state.
- Alternatives: additionally snapshot `git status` / `git diff --stat` to a file in a PreCompact hook
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-41 — Development loop structure
- Date: 2026-09-22
- Area: harness
- Decision: A. phase start (backlog of slices) → B. slice spec (gate: user approval) → C. build, per test case (RED → GREEN → REFACTOR, checkpoint) → D. verify (self-review, outer loop) → E. close (report, retro, state) → STOP (gate: user review and commit). Details in `docs/HARNESS.md` §5.
- Alternatives: not recorded
- Source: user (chat, then AskUserQuestion on the open points D-42..D-46)
- Supersedes: -
- Status: active

### D-42 — Phase backlog before the first slice
- Date: 2026-09-22
- Area: harness
- Decision: at the start of a phase the agent splits it into slices (1–2 lines of DoD each); the backlog is approved by the user before the first slice and kept in `docs/STATE.md`.
- Alternatives: define slices one at a time
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-50

### D-43 — Outside-in TDD
- Date: 2026-09-22
- Area: harness
- Decision: double loop — first a failing acceptance test for the test case at the API level, then unit tests of domain/service code inside it until the acceptance test is green.
- Alternatives: inside-out (domain → service → repository → API); choose per slice in the spec
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-44 — Inner loop test scope
- Date: 2026-09-22
- Area: harness
- Decision: after every change: `compileJava compileTestJava` + the test class(es) of the current test case; when a test case is closed (checkpoint): all tests of the affected module (`--tests '<module package>.*'`). No new Gradle tasks or tags (D-25 unchanged).
- Alternatives: all module tests after every change; JUnit `@Tag` unit/integration split
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-45 — Self-review before the report
- Date: 2026-09-22
- Area: harness
- Decision: `/simplify` before the final `gradlew build`; after the build, the diff is checked against the slice spec and active decisions — every change must trace to one of them (L-7), otherwise it is asked or reverted.
- Alternatives: manual Cyrillic scan; `/code-review`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-46 — Branch per slice
- Date: 2026-09-22
- Area: harness
- Decision: every slice is developed on `slice/NN-<name>` branched from `main`; the agent creates it after the spec is approved (`git switch -c`, confirmed by the user through the permission prompt); the user merges.
- Alternatives: the user creates the branch; base `master`; `feature/<name>` naming
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-51

### D-47 — Sensors are proven by failing
- Date: 2026-09-22
- Area: harness
- Decision: a new sensor or rule is considered active only after it has been shown to fail on a deliberate violation (then the violation is removed); the proof is recorded in the slice journal.
- Alternatives: not recorded
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-48 — Local state first, Linear replicated on completion
- Date: 2026-09-22
- Area: harness
- Decision: during work the local repository files are the source of truth (D-33); Linear (project GameService, team Solarianofc) is updated only when a work cycle completes (D-52). Items waiting for replication are listed in `docs/STATE.md` under `Pending Linear replication`.
- Alternatives: not recorded
- Source: user (chat)
- Supersedes: -
- Status: active

### D-49 — Slice file format (one slice = one Linear issue)
- Date: 2026-09-22
- Area: harness
- Decision: one slice corresponds to exactly one Linear issue. The slice file is `docs/slices/SOL-<n>-<name>.md` = approved spec (goal, scope / out of scope, acceptance criteria, decision links; not changed without user approval) + test-case checklist + append-only journal (including fix attempts per sensor, so the 3-attempt limit survives `/clear`) + report and retro filled at STOP.
- Alternatives: `NN-<name>` numbering with the Linear ID inside the file; one slice grouping several issues
- Source: user (AskUserQuestion)
- Supersedes: D-37
- Status: active

### D-50 — Phase backlog imported from Linear
- Date: 2026-09-22
- Area: harness
- Decision: at the start of a phase (step A) the agent reads the issues of the phase milestone in Linear (with their `blocks` relations), proposes the order and adjustments; the user approves; the backlog is written to `docs/STATE.md`. From then on the local files are the source of truth.
- Alternatives: derive the backlog locally from the roadmap and reconcile Linear during replication
- Source: user (AskUserQuestion)
- Supersedes: D-42
- Status: active

### D-51 — Branch per slice
- Date: 2026-09-22
- Area: harness
- Decision: every slice is developed on `slice/SOL-<n>-<name>` branched from `main`; the agent creates it after the spec is approved (`git switch -c`, confirmed by the user through the permission prompt); the user merges.
- Alternatives: the user creates the branch; base `master`; `feature/<name>` or `NN` naming
- Source: user (AskUserQuestion)
- Supersedes: D-46
- Status: active

### D-52 — Linear replication
- Date: 2026-09-22
- Area: harness
- Decision: at gate 2 (STOP) the agent replicates the slice to its Linear issue: status → `In Review`; a comment with the report (what was done, sensor results, deviations, links to D-<n>, slice file, branch); the issue description replaced by the approved spec (draft values resolved, with D-<n>); new Linear issues (status `Backlog`, correct phase milestone) for deferred out-of-scope items. When the user reports the commit/merge, the issue goes to `Done`. Linear is not touched while the slice is in progress. The report contains a preview of every Linear write; the user confirms each call in the permission prompt (settings unchanged).
- Alternatives: status only after merge; `In Progress` at spec approval; allow Linear writes without confirmation
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-53 — OpenAPI, code-first with springdoc
- Date: 2026-09-22
- Area: architecture
- Decision: the REST API is documented with OpenAPI generated from code by springdoc-openapi (v3 line, which targets Spring Boot 4); compatibility with Spring Boot 4.1 and Jackson 3 is verified as the first test case of the OpenAPI slice — if it fails, the agent returns with a question.
- Alternatives: spec-first with openapi-generator; spec-first without generation plus response validation
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-54 — API documentation completeness
- Date: 2026-09-22
- Area: architecture
- Decision: every endpoint is documented with: summary + description (purpose, behavior, idempotency, required role); request/response schemas where every field has a description, required flag, format and constraints (from Bean Validation); every possible error status as `ProblemDetail` with the list of `errorCode` values for that endpoint; request/response examples (including errors); security schemes (bearer JWT, client credentials). This is part of the DoD of every slice that adds or changes an endpoint.
- Alternatives: not recorded
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-55 — OpenAPI snapshot sensor
- Date: 2026-09-22
- Area: harness
- Decision: a test generates the OpenAPI document and compares it with the committed `docs/api/openapi.yaml`; any difference fails the build, so every API change is visible in review.
- Alternatives: completeness test; Spectral / Redocly lint; response validation against the spec in integration tests
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-56 — Swagger UI and api-docs exposure
- Date: 2026-09-22
- Area: architecture
- Decision: Swagger UI and `/v3/api-docs` are enabled only in the `local` profile; the contract for consumers is the committed snapshot `docs/api/openapi.yaml`.
- Alternatives: enabled everywhere publicly; enabled everywhere for `ADMIN`; no UI, snapshot only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-57 — OpenAPI placement in the roadmap
- Date: 2026-09-22
- Area: harness
- Decision: a new phase 0 slice (after SOL-85, global error handling) sets up springdoc, security schemes, shared `ProblemDetail` responses and the snapshot sensor. The requirement is recorded locally now; the new Linear issue is created as part of the replication that closes the current cycle, and existing API issues receive the requirement through "description = approved spec" when they are completed.
- Alternatives: inside SOL-85; inside the first API slice of phase 1; update Linear immediately
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active
