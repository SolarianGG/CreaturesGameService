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

### D-58 — Phase 0 backlog
- Date: 2026-09-22
- Area: harness
- Decision: phase 0 slice order: SOL-82 (narrowed to Spotless + Error Prone/NullAway) → new issue "Checkstyle + PMD + SpotBugs + JaCoCo" (split from SOL-82) → SOL-87 CI → SOL-80 → SOL-84 → SOL-83 → SOL-86 → SOL-85 → SOL-137 → SOL-81. Missing Linear relations (SOL-86 blocks SOL-85, SOL-84 blocks SOL-83, SOL-80 blocks SOL-83, SOL-86 blocks SOL-81, SOL-80 blocks SOL-86; new issue blocks SOL-87) are added at the next replication.
- Alternatives: CI last; dependencies (SOL-80) first; SOL-82 as one slice; SOL-82 split per tool; new issue after CI; add relations now or not at all
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-59 — SOL-82: remove Flyway starters from the skeleton
- Date: 2026-09-22
- Area: harness
- Decision: `spring-boot-starter-flyway` and `spring-boot-starter-flyway-test` are removed from `build.gradle` in SOL-82 so the baseline build is green (the skeleton's `contextLoads` fails without a DataSource); Flyway returns in SOL-80 / SOL-84 together with the DataSource.
- Alternatives: exclude DataSource/Flyway auto-configuration in the test; move SOL-80 first
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-60 — Tool versions in a version catalog
- Date: 2026-09-22
- Area: harness
- Decision: versions are declared in `gradle/libs.versions.toml`. Compile-time sensors: Spotless Gradle plugin 8.10.2, palantir-java-format 2.98.0, `net.ltgt.errorprone` plugin 5.1.1, Error Prone 2.50.0, NullAway 0.14.1, JSpecify 1.0.1 (latest releases on 2026-09-22).
- Alternatives: the same versions inline in `build.gradle`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-61 — NullAway configuration and scope
- Date: 2026-09-22
- Area: harness
- Decision: NullAway in JSpecify mode at ERROR severity; null-marking through `@NullMarked` in `package-info.java` of every package (no `AnnotatedPackages`). NullAway runs on main sources only; Error Prone runs on main and test sources.
- Alternatives: `AnnotatedPackages = com.solarianofc.gameservice`; both AnnotatedPackages and JSpecify mode; both tools on main and test; both on main only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-62 — Error Prone strictness
- Date: 2026-09-22
- Area: harness
- Decision: the default Error Prone check set (ERROR and WARNING checks) with javac `-Werror`, so any warning fails compilation; disabling an individual check only with user approval.
- Alternatives: default + additional off-by-default checks; only ERROR checks fail the build (would contradict D-23)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-63 — Spotless scope
- Date: 2026-09-22
- Area: harness
- Decision: Spotless formats Java only (`src/**/*.java`) with palantir-java-format, including import ordering and removal of unused imports.
- Alternatives: Java + Gradle + misc files; Java + misc files
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-64 — @NullMarked presence test
- Date: 2026-09-22
- Area: harness
- Decision: a JUnit test in SOL-82 fails if any package under `src/main/java` lacks a `package-info.java` annotated with `@NullMarked` (NullAway silently skips unmarked packages); it may later move to ArchUnit (SOL-83).
- Alternatives: Checkstyle `JavadocPackage` in the build-time sensors slice; ArchUnit in SOL-83
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-65 — Sensor failure proofs with temporary files
- Date: 2026-09-22
- Area: harness
- Decision: D-47 proofs use temporary source files with a deliberate violation (one per check), the failure output with the check name is recorded in the slice journal, then the file is deleted.
- Alternatives: permanent sensor self-tests (Gradle TestKit / compile-testing fixtures)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-66 — Base starters replace the Flyway starters
- Date: 2026-09-22
- Area: harness
- Decision: in SOL-82 the removed Flyway starters (D-59) are replaced by `spring-boot-starter` (implementation) and `spring-boot-starter-test` (testImplementation) — the starters they brought transitively; all other dependencies come with SOL-80.
- Alternatives: only `spring-boot-starter-test`; revisit D-59
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-67 — Build-time sensor versions
- Date: 2026-09-22
- Area: harness
- Decision: in `gradle/libs.versions.toml`: Checkstyle 14.1.0, PMD 7.27.0, SpotBugs 4.10.4 with the `com.github.spotbugs` Gradle plugin 6.5.11, find-sec-bugs 1.14.0, JaCoCo 0.8.15 (latest releases on 2026-09-22).
- Alternatives: Gradle's built-in default tool versions for Checkstyle / PMD / JaCoCo
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-68 — Checkstyle rule set
- Date: 2026-09-22
- Area: harness
- Decision: naming (PackageName, TypeName, MethodName, ConstantName, MemberName, ParameterName, LocalVariableName), imports (AvoidStarImport, IllegalImport, RedundantImport, UnusedImports), sizes with Checkstyle defaults (FileLength 2000, MethodLength 150, ParameterNumber 7), OneTopLevelClass. Formatting is owned by Spotless.
- Alternatives: stricter sizes (FileLength 500, MethodLength 60, ParameterNumber 5); naming + imports only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-69 — PMD rule set
- Date: 2026-09-22
- Area: harness
- Decision: `category/java/bestpractices.xml` and `category/java/errorprone.xml` in full; a rule is excluded only when it actually gets in the way, with user approval (D-23).
- Alternatives: the categories minus known noisy rules up front
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-70 — SpotBugs configuration
- Date: 2026-09-22
- Area: harness
- Decision: effort `max`, report threshold `low`, find-sec-bugs plugin enabled; exclusions only for actual findings, with user approval.
- Alternatives: effort max + threshold medium; defaults without find-sec-bugs
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-71 — Build-time sensor scope
- Date: 2026-09-22
- Area: harness
- Decision: Checkstyle and PMD run on main and test sources; SpotBugs runs on main sources only.
- Alternatives: all three on main and test; all three on main only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-72 — JaCoCo verification
- Date: 2026-09-22
- Area: harness
- Decision: `jacocoTestCoverageVerification` runs in `check` with the D-24 thresholds (≥ 70% lines, ≥ 60% branches) on the whole project (BUNDLE). `GameServiceApplication` (entry point without logic, `main()` not called by tests) is excluded from coverage — an approved suppression under D-23, documented with a comment in the build file.
- Alternatives: a test that calls `main()`; measure first and decide; per-class or per-package thresholds
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-73 — English sensor output
- Date: 2026-09-22
- Area: harness
- Decision: Checkstyle output is forced to English with `localeLanguage=en` in `config/checkstyle/checkstyle.xml` (it followed the JVM locale and printed Russian). Other tools are checked case by case when they are added.
- Alternatives: `-Duser.language=en` for the whole Gradle daemon in `gradle.properties`; leave localized output
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-74 — contextLoads asserts the application bean
- Date: 2026-09-22
- Area: harness
- Decision: PMD `UnitTestShouldIncludeAssert` on the empty skeleton test `GameServiceApplicationTests.contextLoads()` is fixed by asserting that the `GameServiceApplication` bean is present in the context — no suppression, no rule exclusion.
- Alternatives: `@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")` on the method; exclude the rule from the ruleset
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-75 — CI triggers
- Date: 2026-09-22
- Area: harness
- Decision: the build workflow runs on `push` to `main`, on `pull_request` targeting `main`, and on `workflow_dispatch`.
- Alternatives: push to any branch + pull_request; push to main only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-76 — CI runner and JDK
- Date: 2026-09-22
- Area: harness
- Decision: `ubuntu-latest` (Docker available for the Testcontainers tests of SOL-84) with Temurin JDK 21 via `actions/setup-java`.
- Alternatives: ubuntu-latest + Corretto 21; windows-latest + Temurin 21
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-77 — Gradle caching in CI
- Date: 2026-09-22
- Area: harness
- Decision: `gradle/actions/setup-gradle` (official action: dependency and build caching, job summary, wrapper validation).
- Alternatives: `actions/setup-java` with `cache: gradle`; no caching
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-78 — CI reports
- Date: 2026-09-22
- Area: harness
- Decision: on failure the workflow uploads `build/reports` and `build/test-results` with `actions/upload-artifact`; only first-party actions are used in the pipeline.
- Alternatives: always upload the artifacts; a third-party test reporter with PR annotations
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-79 — GitHub Action versions
- Date: 2026-09-22
- Area: harness
- Decision: `actions/checkout@v7`, `actions/setup-java@v6`, `actions/upload-artifact@v7`, `gradle/actions/setup-gradle@v6` (latest major tags on 2026-09-22).
- Alternatives: not recorded
- Source: user (AskUserQuestion, part of the SOL-87 spec approval)
- Supersedes: -
- Status: active

### D-80 — gradlew is executable in git
- Date: 2026-09-23
- Area: harness
- Decision: `gradlew` is stored with mode 100755 (`git update-index --chmod=+x gradlew`); it was 100644 because the Windows checkout has `core.fileMode=false`, which would make `./gradlew` fail with "Permission denied" on the Linux CI runner.
- Alternatives: run `sh gradlew` in the workflow; add a `chmod +x` step to every run
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-81

### D-81 — chmod in the workflow instead of the git file mode
- Date: 2026-09-23
- Area: harness
- Decision: the CI build step runs `chmod +x gradlew` before `./gradlew build`. Two attempts to store mode 100755 in git (D-80) were lost: the user's Git client re-indexes the file with `core.fileMode=false`, so the commits kept mode 100644 and the run failed with "Permission denied" (exit 126).
- Alternatives: retry committing the mode from the CLI; run `sh gradlew build` in the workflow
- Source: user (AskUserQuestion)
- Supersedes: D-80
- Status: active

### D-82 — SOL-80: starter set with Boot 4 names and test companions
- Date: 2026-09-23
- Area: architecture
- Decision: SOL-80 adds every starter from the issue under its Boot 4.1 name (verified against the `spring-boot-dependencies` 4.1.1 BOM): `spring-boot-starter-webmvc` (not the deprecated `-web`), `-validation`, `-data-jpa`, `-flyway`, `-security`, `-security-oauth2-resource-server` (not the deprecated `-oauth2-resource-server`), `-data-redis`, `-cache`, `-amqp`, `-websocket`, `-actuator`, `org.postgresql:postgresql` (runtime), each starter's `*-test` companion in `testImplementation`, plus `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-rabbitmq`, `testcontainers-junit-jupiter`, `testcontainers-redis` (versions from the Boot BOM).
- Alternatives: the same main starters with only `spring-boot-starter-test` in tests; only webmvc/validation/actuator now, the rest per slice
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-83 — SOL-80: PostgreSQL Testcontainer for the test context
- Date: 2026-09-23
- Area: architecture
- Decision: the application context under the `test` profile gets its DataSource from a PostgreSQL Testcontainer via `@ServiceConnection`, introduced in SOL-80. SOL-84 adds the Flyway baseline and the Redis and RabbitMQ containers.
- Alternatives: exclude DataSource/JPA/Flyway auto-configuration in the test profile until SOL-84; all three containers already in SOL-80
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-84 — Configuration format and profiles
- Date: 2026-09-23
- Area: architecture
- Decision: `application.properties` is replaced by `application.yml`; profiles `local` (`application-local.yml`, connections to Postgres, Redis and RabbitMQ on localhost for the future Compose setup) and `test` (`application-test.yml`, only what tests need); tests activate the profile with `@ActiveProfiles("test")`.
- Alternatives: the same content as `.properties`; empty profile files filled by later slices
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-85 — Spring Modulith dependencies
- Date: 2026-09-23
- Area: architecture
- Decision: `spring-modulith-bom` 2.1.1 (latest GA on 2026-09-23, line for Boot 4.1) is declared in `gradle/libs.versions.toml` and imported through dependency management; dependencies `spring-modulith-starter-core`, `spring-modulith-starter-jpa` (Event Publication Registry, D-6) and `spring-modulith-starter-test`.
- Alternatives: core + test only (registry later); Modulith deferred to SOL-83
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-86 — Local connection values
- Date: 2026-09-23
- Area: architecture
- Decision: `application-local.yml` uses plain values (reused by Compose in SOL-81): PostgreSQL `localhost:5432`, database `gameservice`, user `gameservice`, password `gameservice`; Redis `localhost:6379` without a password; RabbitMQ `localhost:5672`, `guest`/`guest`.
- Alternatives: environment placeholders with these defaults; environment placeholders without defaults
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-87 — PostgreSQL test image
- Date: 2026-09-23
- Area: architecture
- Decision: Testcontainers use the image `postgres:18-alpine` (major version fixed per D-13, minor floats).
- Alternatives: `postgres:18`; a pinned minor tag
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-88 — No default profile
- Date: 2026-09-23
- Area: architecture
- Decision: `spring.profiles.default` is not set; local runs activate `local` explicitly (`--spring.profiles.active=local`).
- Alternatives: `spring.profiles.default: local`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-89 — JPA settings deferred
- Date: 2026-09-23
- Area: architecture
- Decision: SOL-80 adds no JPA/Hibernate settings (`open-in-view`, `ddl-auto`, ...); they are decided in the slices that introduce entities or the Flyway baseline (SOL-84).
- Alternatives: `spring.jpa.open-in-view: false` and `ddl-auto: validate` now
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-90 — Test container configuration class
- Date: 2026-09-23
- Area: architecture
- Decision: the containers for tests live in `src/test/java/com/solarianofc/gameservice/TestcontainersConfiguration.java` (`@TestConfiguration`, `@ServiceConnection` beans); SOL-84 adds the Redis and RabbitMQ containers to the same class.
- Alternatives: `ContainersConfiguration`; one class per container (`PostgresTestConfiguration`, ...)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-93

### D-91 — Flyway PostgreSQL database module
- Date: 2026-09-23
- Area: architecture
- Decision: `org.flywaydb:flyway-database-postgresql` (version from the Boot 4.1.1 BOM) is added as `runtimeOnly` next to the PostgreSQL driver; without it Flyway 12.4.0 fails with "Unsupported Database: PostgreSQL 18.6". Extends the D-82 set.
- Alternatives: the same module as `implementation`; `spring.flyway.enabled=false` in tests until SOL-84
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-92 — Profile configuration test
- Date: 2026-09-23
- Area: architecture
- Decision: profile values are verified by `src/test/java/com/solarianofc/gameservice/ProfileConfigurationTests.java`, which loads the configuration through Boot's `ConfigDataEnvironmentPostProcessor.applyTo` (no context start, no Docker) with profile `local` and with no profile.
- Alternatives: `LocalProfileConfigurationTests`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-93 — Test container configuration renamed to ContainersConfiguration
- Date: 2026-09-23
- Area: architecture
- Decision: the test container configuration is `src/test/java/com/solarianofc/gameservice/ContainersConfiguration.java`; the D-90 name `TestcontainersConfiguration` matched PMD's default test-class pattern (`Test*`) and failed `pmdTest` with `TestClassWithoutTestCases`. SOL-84 adds the Redis and RabbitMQ containers to the same class.
- Alternatives: narrow `testClassPattern` of the rule in `config/pmd/ruleset.xml`; `@SuppressWarnings("PMD.TestClassWithoutTestCases")`
- Source: user (AskUserQuestion)
- Supersedes: D-90
- Status: active

### D-94 — Flyway baseline migration
- Date: 2026-09-23
- Area: architecture
- Decision: the first migration is `src/main/resources/db/migration/V1__baseline.sql` containing `CREATE EXTENSION IF NOT EXISTS citext` (needed by `users.username` / `users.email` in phase 1, docs/PROJECT.md §5).
- Alternatives: citext + the Spring Modulith `event_publication` table; a comment-only no-op migration; no migration (empty `db/migration`)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-95 — Integration test meta-annotation
- Date: 2026-09-23
- Area: architecture
- Decision: the reusable integration test base is a meta-annotation `@IntegrationTest` in the root test package, combining `@SpringBootTest`, `@ActiveProfiles("test")` and `@Import(ContainersConfiguration.class)`.
- Alternatives: abstract base class `AbstractIntegrationTest`; both (annotation + base class)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-127

### D-96 — Redis test image
- Date: 2026-09-23
- Area: architecture
- Decision: the Redis Testcontainer (`com.redis.testcontainers.RedisContainer`, `@ServiceConnection`) uses the image `redis:8-alpine` (major version fixed, minor floats).
- Alternatives: `redis:7-alpine`; the library default `redis/redis-stack-server`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-97 — RabbitMQ test image
- Date: 2026-09-23
- Area: architecture
- Decision: the RabbitMQ Testcontainer (`org.testcontainers.rabbitmq.RabbitMQContainer`, `@ServiceConnection`) uses the image `rabbitmq:4-management-alpine` (major version fixed, minor floats).
- Alternatives: `rabbitmq:4-alpine`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-98 — JPA settings with the Flyway baseline
- Date: 2026-09-23
- Area: architecture
- Decision: `application.yml` sets `spring.jpa.open-in-view: false` and `spring.jpa.hibernate.ddl-auto: validate` — the schema is owned by Flyway only, Hibernate only validates it. Resolves the deferral of D-89.
- Alternatives: only `open-in-view: false`; defer again to the first entity slice (SOL-88)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-99 — Flyway validate sensor proof
- Date: 2026-09-23
- Area: harness
- Decision: per D-47 the Flyway sensor is proven with three temporary deliberate violations, each shown to fail the context start with its specific Flyway/PostgreSQL error and then removed: a migration with invalid SQL, a migration file name that does not match `V{n}__{description}.sql`, and a second migration with an already used version. The proof is recorded in the SOL-84 journal.
- Alternatives: any subset of the three
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-100 — SOL-84 acceptance tests: one class per resource
- Date: 2026-09-23
- Area: architecture
- Decision: the SOL-84 acceptance tests are `FlywayBaselineIntegrationTests`, `RedisConnectionIntegrationTests` and `RabbitMqConnectionIntegrationTests`, all annotated with `@IntegrationTest` (D-95); `GameServiceApplicationTests` also moves to `@IntegrationTest`.
- Alternatives: extend `GameServiceApplicationTests`; one new `InfrastructureIntegrationTests`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-101 — Container lifecycle via the Spring context cache
- Date: 2026-09-23
- Area: architecture
- Decision: the containers stay `@Bean`s in `ContainersConfiguration`; tests with the same `@IntegrationTest` configuration share one cached context and one set of containers per test run. No static singleton containers, no Testcontainers reuse.
- Alternatives: static singleton containers; additionally Testcontainers reuse between runs
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-102 — Modulith event_publication table as V2
- Date: 2026-09-23
- Area: architecture
- Decision: SOL-84 adds `src/main/resources/db/migration/V2__event_publication.sql` with the table of the Spring Modulith 2.1 JPA Event Publication Registry, typed to pass `ddl-auto: validate` (D-98); V1 stays citext only (D-94). Found in SOL-84 TC-4: validate failed with "missing table [event_publication]".
- Alternatives: the table in V1 together with citext (revise D-94); `ddl-auto: none` until the table exists (revise D-98)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-103 — Flyway validates migration file names
- Date: 2026-09-23
- Area: architecture
- Decision: `application.yml` sets `spring.flyway.validate-migration-naming: true` (tests and application start). Found in the SOL-84 D-99 proof: with the Flyway default (`false`) a file named `V3_bad_name.sql` was silently ignored and the tests stayed green.
- Alternatives: only in `application-test.yml`; accept the gap (deviation from D-99)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-104 — SOL-83 creates no module packages
- Date: 2026-09-23
- Area: architecture
- Decision: SOL-83 adds no application module packages under `com.solarianofc.gameservice`; each module package is created by the slice that first fills it (`shared` in SOL-85/SOL-86, `account` in phase 1, ...). SOL-83 delivers the verification sensors only.
- Alternatives: only `shared` now; all eight modules of `docs/PROJECT.md` §3.1 as empty packages with marker types
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-105 — Module detection: direct sub-packages
- Date: 2026-09-23
- Area: architecture
- Decision: Spring Modulith uses its default detection strategy: every direct sub-package of `com.solarianofc.gameservice` is an application module; its `internal` sub-package is hidden from other modules (`docs/PROJECT.md` §3.1). No `spring.modulith.detection-strategy` setting.
- Alternatives: explicitly annotated modules (`@ApplicationModule` in `package-info.java`, `detection-strategy: explicitly-annotated`)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-106 — Permanent negative test for module boundaries
- Date: 2026-09-23
- Area: harness
- Decision: the Modulith boundary sensor is protected by a permanent test: a separate fixture root package in the test sources holds two fixture modules where one uses a type from the other's `internal` package; the test asserts that `verify()` on that fixture fails with a violation naming the internal type.
- Alternatives: one-time proof with a temporary violation in the main sources (D-47 style); both
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-107 — ArchUnit via archunit-junit5
- Date: 2026-09-23
- Area: harness
- Decision: ArchUnit rules are `@ArchTest` fields run by `com.tngtech.archunit:archunit-junit5`, declared in `gradle/libs.versions.toml` with the same version as the ArchUnit core that Spring Modulith 2.1.1 brings transitively (1.4.2) and added as `testImplementation`.
- Alternatives: ArchUnit core only (`ClassFileImporter` + `rule.check(...)` in plain JUnit tests), no new dependency
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-108 — Outbox behaviour proven by an integration test
- Date: 2026-09-23
- Area: architecture
- Decision: SOL-83 proves the Event Publication Registry as a transactional outbox with an `@IntegrationTest` using a test-only event and `@ApplicationModuleListener`s in the test sources: a successful listener leaves a completed publication, a failing listener leaves an incomplete publication in `event_publication`, a rolled-back publishing transaction leaves no publication.
- Alternatives: only the context start (registry bean present, table passes validate); behaviour tested in the slice with the first real event
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-109 — Event publication completion mode UPDATE
- Date: 2026-09-23
- Area: architecture
- Decision: `spring.modulith.events.completion-mode` stays at the Spring Modulith default `UPDATE` (completed publications keep their row with `completion_date`); no setting is added. Cleanup of completed publications is a later slice.
- Alternatives: `DELETE`; `ARCHIVE` (extra `event_publication_archive` migration)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-110 — Republish of incomplete publications deferred
- Date: 2026-09-23
- Area: architecture
- Decision: `spring.modulith.events.republish-outstanding-events-on-restart` stays at the default `false` in SOL-83; the retry policy for incomplete publications (on restart or scheduled via `IncompleteEventPublications`) is decided in the slice with the first real domain event; SOL-83 close creates a Backlog issue for it.
- Alternatives: enable republish on restart now
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-111 — ArchUnit rule set of SOL-83
- Date: 2026-09-23
- Area: harness
- Decision: three ArchUnit rules on the main classes, each with a permanent negative test on a fixture: (1) no field injection (`GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION`); (2) no access to `System.out`/`System.err` and no `java.util.logging` (`GeneralCodingRules`); (3) classes annotated with `@RestController`/`@Controller` reside in `..internal.web..`. Rules allow an empty `should` while the application has no matching classes. `NullMarkedPackagesTest` (D-64) stays as it is.
- Alternatives: additionally move the `@NullMarked` check to ArchUnit; only rules (1) and (2)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-116

### D-112 — Architecture test names and fixture package
- Date: 2026-09-23
- Area: harness
- Decision: test classes in `src/test/java/com/solarianofc/gameservice/`: `ModularityTests` (`verify()` on the application + the D-106 fixture must fail), `ArchitectureRulesTests` (the D-111 `@ArchTest` rules on the main classes + negative tests on fixtures), `EventPublicationRegistryIntegrationTests` (D-108). Fixtures with deliberate violations live outside the application package, in `com.solarianofc.archfixtures` (`modules/alpha`, `modules/beta/internal`, `rules/`), so component scan and module detection never see them; fixture violations must not need PMD/Checkstyle suppressions.
- Alternatives: fixtures under `com.solarianofc.gameservice.archfixtures`, excluded from component scan and from the Modulith/ArchUnit import by package
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-113 — Asynchronous module listeners (@EnableAsync)
- Date: 2026-09-23
- Area: architecture
- Decision: `GameServiceApplication` is annotated with `@EnableAsync`, so `@ApplicationModuleListener`s run asynchronously after the commit on the task executor auto-configured by Spring Boot; pool settings are left to a later slice. The outbox test (D-108) waits for listener outcomes with Awaitility (transitive via `spring-modulith-starter-test`).
- Alternatives: decide in the slice with the first real event (listeners run synchronously after commit until then)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: superseded by D-117

### D-114 — Outbox test in its own context
- Date: 2026-09-23
- Area: harness
- Decision: the test event and listeners of `EventPublicationRegistryIntegrationTests` are registered by a nested `@TestConfiguration` of that class only; the test gets its own Spring context and container set (D-101), other integration tests do not see the listeners.
- Alternatives: listeners as `@Component`s in the test sources under `com.solarianofc.gameservice` (one shared context for all integration tests)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-115 — Modulith Documenter deferred
- Date: 2026-09-23
- Area: architecture
- Decision: generating module documentation (Spring Modulith `Documenter`: C4/PlantUML diagrams, Module Canvas) is out of scope of SOL-83 (no modules yet, D-104); SOL-83 close creates a Backlog issue for it.
- Alternatives: out of scope without an issue; in SOL-83
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-116 — ArchUnit rule set of SOL-83 (strict java.util.logging rule)
- Date: 2026-09-23
- Area: harness
- Decision: as D-111, except the java.util.logging part of rule (2): instead of `GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING` (it only flags setting a field of a `java.util.logging` type — found in SOL-83 TC-3, a direct `Logger.getLogger(...)` call passed) a custom rule `noClasses().should().dependOnClassesThat().resideInAPackage("java.util.logging..")`. Rule set: (1) no field injection; (2) no `System.out`/`System.err` access (`GeneralCodingRules`) and no dependency on `java.util.logging`; (3) `@RestController`/`@Controller` classes reside in `..internal.web..`; empty `should` allowed where the main classes have no matching elements; `NullMarkedPackagesTest` stays.
- Alternatives: keep the library rule (JUL loggers in fields only, fixture changed to a static field); both the library and the custom rule
- Source: user (AskUserQuestion)
- Supersedes: D-111
- Status: active

### D-117 — Asynchronous listeners via the Modulith auto-configuration
- Date: 2026-09-23
- Area: architecture
- Decision: no `@EnableAsync` in the application: Spring Modulith 2.1.1 enables async processing itself (`EventPublicationAutoConfiguration$AsyncEnablingConfiguration`: `@EnableAsync` + `@ConditionalOnMissingBean`), so `@ApplicationModuleListener`s already run asynchronously on the Boot task executor. `EventPublicationRegistryIntegrationTests` pins the asynchronous execution; its sensitivity is proven once with a temporarily synchronous listener (`@TransactionalEventListener`). D-113 was approved on the wrong premise that listeners run synchronously without an explicit `@EnableAsync` (found in SOL-83 TC-5).
- Alternatives: keep an explicit `@EnableAsync` on `GameServiceApplication` (same behaviour, the Modulith auto-configuration then backs off)
- Source: user (AskUserQuestion)
- Supersedes: D-113
- Status: active

### D-118 — Structured log format: ECS
- Date: 2026-09-23
- Area: architecture
- Decision: console logs use Spring Boot built-in structured logging in the Elastic Common Schema format (`logging.structured.format.console: ecs`).
- Alternatives: Logstash; GELF
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-119 — Profiles with JSON logs
- Date: 2026-09-23
- Area: architecture
- Decision: JSON console logs are configured in `application.yml` (default: production / Compose); the `local` and `test` profiles switch back to plain text. The test that checks the JSON format enables it for its own context only.
- Alternatives: JSON in every profile; JSON in default and `test`, plain text only in `local`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-120 — Tracer: OpenTelemetry bridge without export
- Date: 2026-09-23
- Area: architecture
- Decision: `traceId`/`spanId` come from Micrometer Tracing with the OpenTelemetry bridge: `org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry` + `io.micrometer:micrometer-tracing-bridge-otel` (versions from the Boot 4.1.1 BOM). No span exporter; OTLP export (e.g. to Grafana Tempo) can be added later without changing the tracer. Recommended by the agent: Boot 4 has dedicated OpenTelemetry modules, the standard is vendor-neutral and fits the Grafana stack; Brave is tied to Zipkin, which the project does not use.
- Alternatives: Brave bridge without export; `spring-boot-starter-opentelemetry` (adds OTLP trace and metrics exporters that need a collector); defer `traceId` to a later issue
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-121 — Trace sampling probability 1.0
- Date: 2026-09-23
- Area: architecture
- Decision: `management.tracing.sampling.probability: 1.0`.
- Alternatives: keep the Boot default
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-122 — Management port 8081, exposure health + prometheus
- Date: 2026-09-23
- Area: architecture
- Decision: Actuator runs on a separate management server port `8081` (`management.server.port`); web exposure is exactly `health` and `prometheus` (`docs/PROJECT.md` §7); the Prometheus registry comes from `io.micrometer:micrometer-registry-prometheus` (version from the Boot BOM). Network isolation of the port is part of SOL-81 (Docker Compose).
- Alternatives: port 9000; port from an environment variable with default 8081
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-123 — Probes only on the management port
- Date: 2026-09-23
- Area: architecture
- Decision: liveness and readiness are served only as `/actuator/health/liveness` and `/actuator/health/readiness` on the management port; no additional `/livez` / `/readyz` paths on the main port (`management.endpoint.health.probes.add-additional-paths` stays `false`). Reason: the app runs in Docker Compose, not Kubernetes; extra paths only widen the public surface of the main port.
- Alternatives: additionally `/livez` and `/readyz` on the main port
- Source: user (AskUserQuestion) — delegated to the agent ("choose yourself")
- Supersedes: -
- Status: active

### D-124 — Actuator and application security chains
- Date: 2026-09-23
- Area: architecture
- Decision: two `SecurityFilterChain` beans: (1) management chain (`EndpointRequest.toAnyEndpoint()`): `health` (incl. its groups) and `prometheus` `permitAll`, every other request `denyAll`; protection of `prometheus` is the network isolation of the management port (D-122). (2) application chain: every request `denyAll` until the `account` module replaces it in phase 1. The second chain is required because a user-defined `SecurityFilterChain` switches off both Boot default chains (verified in Boot 4.1.1: the default management chain permits only health and uses formLogin/httpBasic for the rest). `/security-review` at the end of SOL-86.
- Alternatives: HTTP Basic for `prometheus` with a user from the environment; defer security (prometheus answers 401 until a later issue)
- Source: user (AskUserQuestion) — delegated to the agent ("decide yourself")
- Supersedes: -
- Status: active

### D-125 — Readiness composition and its outage test
- Date: 2026-09-23
- Area: architecture
- Decision: the readiness group includes `readinessState`, `db`, `redis`, `rabbit` (indicator names verified in the Boot 4.1.1 jars). Tests: (a) composition — the readiness group reports `db`, `redis`, `rabbit` UP on the shared `@IntegrationTest` context; (b) one real outage in its own context with its own containers (as D-114): the RabbitMQ container is stopped -> readiness answers 503 / `DOWN`, liveness stays `UP`.
- Alternatives: a real outage of each of the three dependencies (three extra contexts); composition only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-126 — Security configuration placement
- Date: 2026-09-23
- Area: architecture
- Decision: the first code of the `shared` module (D-104): `com.solarianofc.gameservice.shared.internal.security.SecurityConfiguration` with the beans `managementSecurityFilterChain` and `applicationSecurityFilterChain`; the package has a `package-info.java` with `@NullMarked`.
- Alternatives: package `shared.internal.config`; two classes `ManagementSecurityConfiguration` + `ApplicationSecurityConfiguration`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-127 — Integration tests run a real server on random ports
- Date: 2026-09-23
- Area: architecture
- Decision: as D-95 (meta-annotation `@IntegrationTest` in the root test package with `@ActiveProfiles("test")` and `@Import(ContainersConfiguration.class)`), but with `@SpringBootTest(webEnvironment = RANDOM_PORT)`: every integration test starts the main and the management server on random ports; one shared cached context stays (D-101).
- Alternatives: a separate meta-annotation with `RANDOM_PORT` for actuator tests only, `@IntegrationTest` stays `MOCK`
- Source: user (AskUserQuestion)
- Supersedes: D-95
- Status: active

### D-128 — Separate contexts for the log and outage tests
- Date: 2026-09-23
- Area: architecture
- Decision: the JSON log test (JSON enabled for its own context, D-119) and the RabbitMQ outage test (D-125) are two independent classes, each with its own context and container set; no test-order dependency.
- Alternatives: one class and context with ordered tests (logs first, then the RabbitMQ stop)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-129 — HTTP client for integration tests: RestTestClient
- Date: 2026-09-23
- Area: architecture
- Decision: integration tests against the real server use Spring Framework 7 `RestTestClient` (already on the test classpath, no new dependency); the management port is reached with `RestTestClient.bindToServer().baseUrl(...)` and `@LocalManagementPort`.
- Alternatives: `TestRestTemplate` (Boot 4 `spring-boot-resttestclient`); `java.net.http.HttpClient`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-130 — Metrics export enabled in integration tests
- Date: 2026-09-23
- Area: architecture
- Decision: `@IntegrationTest` carries `@AutoConfigureMetrics`, so the shared context has the real Prometheus registry and `/actuator/prometheus` as in production. Reason: Boot 4.1.1 `MetricsContextCustomizerFactory` sets `management.defaults.metrics.export.enabled=false` for every `@SpringBootTest` without it (found in SOL-86 TC-2).
- Alternatives: `@AutoConfigureMetrics` only on `ActuatorEndpointsIntegrationTests` (extra context); `spring.test.metrics.export: true` in `application-test.yml`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-131 — pmd at the test-case checkpoint
- Date: 2026-09-23
- Area: harness
- Decision: the checkpoint step of the inner loop (docs/HARNESS.md §5 C, AGENTS.md "Checkpoint") also runs `./gradlew pmdMain pmdTest`, so PMD violations surface per test case, not only in the verify step (L-27). Implemented as a separate harness change after SOL-86.
- Alternatives: keep PMD only in the outer loop (`./gradlew build`)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-132 — Hook against writing Java files through the shell
- Date: 2026-09-23
- Area: harness
- Decision: a PreToolUse hook on Bash/PowerShell returns `ask` when a command writes to a `*.java` file (sed -i, output redirect, python/heredoc naming a .java path), turning the L-22 anti-pattern into a sensor (L-28). Implemented as a separate harness change after SOL-86, proven on a deliberate violation (D-47) and on a real tool call (L-4).
- Alternatives: keep the rule in AGENTS.md only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active
