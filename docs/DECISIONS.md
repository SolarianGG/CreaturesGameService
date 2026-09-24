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
- Status: superseded by D-200

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

### D-133 — Harness changes run as a slice
- Date: 2026-09-23
- Area: harness
- Decision: the harness change D-131 + D-132 is a regular slice: its own Linear issue (project GameService, milestone "Phase 0 — Skeleton", no label, as SOL-138), slice file, branch `slice/SOL-<n>-...`, the usual loop and PR; it runs before SOL-85.
- Alternatives: a branch without Linear and slice file; fold it into SOL-85 as the first test case
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-134 — Detection rule of the Java shell-write guard
- Date: 2026-09-23
- Area: harness
- Decision: `guard_java_shell_writes.py` returns `ask` for a Bash/PowerShell command that contains a `.java` path AND a write indicator: output redirect `>` / `>>`, `sed -i`, `tee`, heredoc `<<`, `cp` / `mv`, `Set-Content` / `Out-File` / `Add-Content`, or a script interpreter run (`python`, `py`, `perl`, `node`) — a script naming a .java path counts as a write. Read-only use (grep, cat, head, `sed -n`, git diff/show/log) does not ask. A false positive costs one confirmation prompt.
- Alternatives: ask on any `.java` mention except a read-only allowlist
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-135 — Hook proof by pipe tests and a live call
- Date: 2026-09-23
- Area: harness
- Decision: the guard is proven as earlier hooks (L-4, L-5): payloads built with `json.dumps` in the real PreToolUse format (L-3) by a script in the session scratchpad, results recorded in the slice journal; then a live tool call after the user reloads hooks (`/hooks`). No permanent hook test in the repository.
- Alternatives: a permanent stdlib `unittest` file next to the hook, run manually
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-136 — Guard hook file name
- Date: 2026-09-23
- Area: harness
- Decision: `.claude/hooks/guard_java_shell_writes.py`, registered in `.claude/settings.json` under PreToolUse with matcher `Bash|PowerShell`.
- Alternatives: `protect_java_sources.py`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-137 — Root-cause follow-ups to L-22 in the Backlog
- Date: 2026-09-23
- Area: harness
- Decision: one Backlog issue (created at the SOL-141 close) with three items from the SOL-141 altitude review: (1) `.gitattributes` rule for `*.java` line endings (e.g. `*.java text eol=lf`, possible one-time renormalize); (2) inner loop runs `./gradlew spotlessApply compileJava compileTestJava` explicitly, so every changed Java file is formatted whatever wrote it; (3) IDE MCP write tools (`mcp__idea__apply_patch`, `create_new_file`, ...) bypass the Spotless and config hooks — deny them in permissions or cover them with hooks. Each item is decided in that issue's spec.
- Alternatives: record in the retro only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-138 — External login gate with a provider SPI
- Date: 2026-09-23
- Area: account
- Decision: external platforms log in through one endpoint `POST /api/v1/auth/external/{provider}` with the platform token in the body, in the `account` module. Inside, an `ExternalIdentityProvider` interface verifies the token and returns the external id; a new platform is a new implementation, the API does not change. The gate finds the user by `(provider, external_id)` or creates one on the first login, then reuses the existing account logic: our access JWT + refresh token (D-3), roles, bans.
- Alternatives: one endpoint per provider (`/auth/steam`, ...); a separate auth-gateway service
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-139 — Password login stays alongside the gate
- Date: 2026-09-23
- Area: account
- Decision: registration and login by username/email + password (`docs/PROJECT.md` §4.1) remain available to everyone, as one more login method next to the external gate (D-138).
- Alternatives: password only for ADMIN (no public registration); no password login at all
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-140 — External identities in a `user_identities` table
- Date: 2026-09-23
- Area: account
- Decision: the link to an external platform is stored in a `user_identities` table: `(provider, external_id)` unique → `user_id`. `users.email` and `users.password_hash` become nullable for accounts created through the gate.
- Alternatives: a nullable unique `users.steam_id` column
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-141 — Username chosen by the player after the first external login
- Date: 2026-09-23
- Area: account
- Decision: an account created through the gate has no username yet; the login response tells the client that a username must be chosen, and the player picks it through a separate call. The username rules of `docs/PROJECT.md` §4.1 apply (3–20 characters, `[A-Za-z0-9_]`, case-insensitively unique). The flag and endpoint names and what the player may do before choosing are decided separately.
- Alternatives: generated name changeable later; Steam persona name
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-142 — Platform bans are ignored
- Date: 2026-09-23
- Area: account
- Decision: bans reported by the platform (Steam `vacbanned`, `publisherbanned`) do not block login; only the service's own bans (ADMIN, `docs/PROJECT.md` §4.1) apply.
- Alternatives: `publisherbanned` blocks login; both flags block login
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-143 — Linking several providers to one account is deferred
- Date: 2026-09-23
- Area: account
- Decision: for now one external identity = one account. Linking/unlinking several providers to one account is a separate Backlog issue; the `user_identities` schema (D-140) already allows it.
- Alternatives: link/unlink in phase 1; never allow linking
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-144 — Steam is the only external provider in phase 1
- Date: 2026-09-23
- Area: account
- Decision: phase 1 implements one `ExternalIdentityProvider` — Steam. Other platforms (EOS, Epic, ...) are separate issues when needed.
- Alternatives: Steam + EOS
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-145 — Steam provider tested against WireMock
- Date: 2026-09-23
- Area: account
- Decision: tests never call the real Steam Web API; acceptance tests of the Steam provider go through the real HTTP client to a WireMock stub of the Steam Web API (response parsing, Steam errors, timeouts). WireMock is a new test dependency; its version is agreed when it is added.
- Alternatives: a fake `ExternalIdentityProvider` bean only; WireMock for the provider + a fake for other tests
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-146 — No access without a username
- Date: 2026-09-23
- Area: account
- Decision: until a gate-created account has chosen its username (D-141), every PLAYER endpoint except choosing the username answers `403` `ProblemDetail` with `errorCode = USERNAME_REQUIRED`. Players without a username never appear in the queue, matches or the leaderboard.
- Alternatives: full access with an empty username shown as a placeholder
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-147 — Steam settings from environment variables
- Date: 2026-09-23
- Area: account
- Decision: the Steam Web API key and app id are read from `STEAM_WEB_API_KEY` and `STEAM_APP_ID`; a git-ignored `.env` feeds them to the local Docker Compose run, a committed `.env.example` shows app id `480` (Spacewar, the game's test app) and an empty key. The `test` profile uses fake values and the WireMock URL (D-145). No key is ever committed. Whether `AuthenticateUserTicket` accepts a user (non-publisher) Web API key for app `480` is verified with one real call in the slice.
- Alternatives: environment variables only, without `.env` files
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-148 — Steam provider disabled when no key is set
- Date: 2026-09-23
- Area: account
- Decision: without a Steam key the application still starts and password login works; `POST /api/v1/auth/external/steam` answers a `ProblemDetail` "provider unavailable" (status and `errorCode` defined in the slice spec).
- Alternatives: fail fast at startup in every profile except `test`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-149 — Steam auth session ticket
- Date: 2026-09-23
- Area: account
- Decision: the Steam provider accepts the hex-encoded ticket from `GetAuthSessionTicket` (UE 4.27 ships Steamworks SDK 1.51 on Windows, checked in `Engine/Source/ThirdParty/Steamworks/Steamworks.build.cs`; `GetAuthTicketForWebApi` needs SDK 1.57+). The client gets it from `IOnlineIdentity::GetAuthToken()`; the service calls `AuthenticateUserTicket` without `identity`. The gate API (D-138) does not depend on the ticket type, so `GetAuthTicketForWebApi` can be added later. If the personal Web API key turns out not to work for app `480` (D-147), the fallback is decided after that check.
- Alternatives: upgrade the Steamworks SDK in the engine and use `GetAuthTicketForWebApi`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-150 — ProblemDetail `type` URI
- Date: 2026-09-23
- Area: architecture
- Decision: `type` = `https://gameservice.local/problems/<slug>`, where the slug is the `errorCode` in kebab case (`VALIDATION_ERROR` -> `validation-error`). The URI is a stable identifier, it does not have to resolve.
- Alternatives: `about:blank`; `urn:gameservice:problem:<slug>`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-151 — Common error codes
- Date: 2026-09-23
- Area: architecture
- Decision: SOL-85 introduces the common codes `VALIDATION_ERROR` (Bean Validation, with `errors[]`), `MALFORMED_REQUEST` (unreadable JSON, wrong parameter type, missing parameter), `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `CONFLICT`, `RATE_LIMITED`, `INTERNAL_ERROR`. Business codes are added by the modules.
- Alternatives: only the statuses listed in PROJECT.md §6 (other framework 4xx mapped to the nearest); one code per HTTP status name
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-152 — Module errors through a base exception in `shared`
- Date: 2026-09-23
- Area: architecture
- Decision: `shared` exposes an abstract `ApiException` (HTTP status, `ErrorCode`, detail); modules throw subclasses and the global advice maps them uniformly. Extra fields (e.g. `retryAfterSeconds`) go into the `ProblemDetail` properties.
- Alternatives: one `@RestControllerAdvice` per module; decide in the first phase 1 slice
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-153 — Security 401/403 as ProblemDetail
- Date: 2026-09-23
- Area: architecture
- Decision: an `AuthenticationEntryPoint` and an `AccessDeniedHandler` in `shared` write the same `ProblemDetail` (`UNAUTHORIZED`, `FORBIDDEN`) for responses produced by the security filter chains.
- Alternatives: keep the Spring Security defaults until the `account` module (phase 1)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-154 — Error handling tested through a test controller
- Date: 2026-09-23
- Area: harness
- Decision: the acceptance tests of SOL-85 use a controller in `src/test` whose endpoints raise each kind of error, plus a test `SecurityFilterChain` (higher precedence) that permits only that controller's path; everything else goes through the production chains. Real HTTP (`RANDOM_PORT`), raw JSON strings as request bodies, own context (as D-128).
- Alternatives: `@WebMvcTest` slice; HTTP test + Spring-free unit tests of the advice
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-155 — `traceId` omitted without a current span
- Date: 2026-09-23
- Area: architecture
- Decision: `traceId` is taken from the Micrometer `Tracer` (the same ID as in the logs, D-120); without a current span the field is omitted from the response.
- Alternatives: always present (generated ID when no span); always present with `null`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-156 — Error logging
- Date: 2026-09-23
- Area: architecture
- Decision: unexpected exceptions (500) are logged at `ERROR` with the stack trace, the response carries only a generic `detail`; 4xx are expected client errors and are not logged.
- Alternatives: 4xx at `DEBUG`; 4xx at `WARN`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-157 — Own `ErrorController`
- Date: 2026-09-23
- Area: architecture
- Decision: Boot's `BasicErrorController` is replaced by an `ErrorController` in `shared` that answers the `/error` dispatch (exceptions and statuses outside Spring MVC) with the same `ProblemDetail`; the application security chain permits the `ERROR` dispatch. Every error response of the main port is `application/problem+json`.
- Alternatives: leave `/error` as is, separate Backlog issue
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-158 — `ErrorCode` interface with enums per module
- Date: 2026-09-23
- Area: architecture
- Decision: `shared` exposes `interface ErrorCode { String code(); }` and `enum CommonErrorCode implements ErrorCode` with the D-151 codes; each module declares its own enum (e.g. `MatchmakingErrorCode`). `shared` knows nothing about module codes.
- Alternatives: one enum with all codes in `shared`; plain string constants
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-159 — Sources of `errors[]`
- Date: 2026-09-23
- Area: architecture
- Decision: `errors[]` (`field`, `message`) is filled for `@Valid @RequestBody` (`MethodArgumentNotValidException`, `field` = JSON property path such as `username` or `items[0].type`) and for method parameter validation (`HandlerMethodValidationException`, `field` = query/path parameter name). `message` is the Bean Validation message in English.
- Alternatives: request body only
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-160 — `Retry-After` on 429
- Date: 2026-09-23
- Area: architecture
- Decision: a `RATE_LIMITED` exception may carry a `Duration`; then the response has the `Retry-After: <seconds>` header and the `retryAfterSeconds` property. The rate limiter itself comes in phase 1.
- Alternatives: status and code only, header added by the rate limiter slice
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-161 — Packages of the error handling
- Date: 2026-09-23
- Area: architecture
- Decision: the public API (`ApiException`, `ErrorCode`, `CommonErrorCode`) lives in `com.solarianofc.gameservice.shared.error`, exposed as a Modulith named interface; the implementation (advice, `ErrorController`, `ProblemDetail` factory) in `shared.internal.error`; the security entry point and access denied handler in `shared.internal.security` next to the chains.
- Alternatives: public API in the `shared` root package
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-162 — ProblemDetail `title` and common `detail` texts
- Date: 2026-09-23
- Area: architecture
- Decision: `title` = the HTTP reason phrase of the status. Generic English `detail` texts: `VALIDATION_ERROR` "Request contains invalid fields"; `MALFORMED_REQUEST` "Request could not be read"; `UNAUTHORIZED` "Authentication is required"; `FORBIDDEN` "Access is denied"; `NOT_FOUND` "Resource not found"; `METHOD_NOT_ALLOWED` "Method is not supported for this resource"; `UNSUPPORTED_MEDIA_TYPE` "Content type is not supported"; `INTERNAL_ERROR` "An unexpected error occurred". `CONFLICT`, `RATE_LIMITED` and module codes carry the detail given by the thrower.
- Alternatives: not recorded (approved with the SOL-85 spec)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-163 — PMD `ImplicitFunctionalInterface` suppressed on `ErrorCode`
- Date: 2026-09-23
- Area: harness
- Decision: `ErrorCode` carries `@SuppressWarnings("PMD.ImplicitFunctionalInterface")` with a comment: it is implemented by the module enums (D-158) and is not meant as a lambda target.
- Alternatives: `@FunctionalInterface`; a second abstract method so the interface is no longer functional
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-164 — Fixed English locale for web requests
- Date: 2026-09-23
- Area: architecture
- Decision: `spring.web.locale: en` and `spring.web.locale-resolver: fixed` in `application.yml`: the server ignores `Accept-Language`, so Bean Validation messages in `errors[]` are English regardless of the client and the JVM locale (D-159).
- Alternatives: own `LocalValidatorFactoryBean` with an English-only message interpolator; messages in the request locale (relax D-159)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-165 — Error code by status for other framework exceptions
- Date: 2026-09-24
- Area: architecture
- Decision: framework exceptions without a dedicated mapping keep their HTTP status and get the code by status: 400 `MALFORMED_REQUEST`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 404 `NOT_FOUND`, 405 `METHOD_NOT_ALLOWED`, 409 `CONFLICT`, 415 `UNSUPPORTED_MEDIA_TYPE`, 429 `RATE_LIMITED`; any other 4xx (e.g. 406, 413) `MALFORMED_REQUEST`; any 5xx (e.g. 503) `INTERNAL_ERROR` with the generic detail and an `ERROR` log (D-156). The set of common codes (D-151) does not grow.
- Alternatives: add `NOT_ACCEPTABLE`, `PAYLOAD_TOO_LARGE`, `SERVICE_UNAVAILABLE`; code = HTTP status name for unmapped statuses
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-166 — Anonymous denied requests answer 401
- Date: 2026-09-24
- Area: architecture
- Decision: the problem `AuthenticationEntryPoint` (D-153) is wired into the application chain, so an anonymous request to a denied path of the main port answers 401 `UNAUTHORIZED`; 403 `FORBIDDEN` is for authenticated callers without the required rights. The SOL-86 main-port tests change from 403 to 401 problem+json; 403 is tested through a test chain with HTTP Basic and an in-memory user (test sources only). No `WWW-Authenticate` header until the JWT slice (phase 1).
- Alternatives: the entry point answers 403 `FORBIDDEN` until phase 1
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-167 — `ErrorController` in `shared.internal.web`
- Date: 2026-09-24
- Area: architecture
- Decision: the own `ErrorController` (D-157) is `shared.internal.web.ProblemErrorController`, because the ArchUnit rule of D-116 requires every `@Controller` in an `internal.web` package; the rest of the error handling stays in `shared.internal.error` (D-161), whose `ProblemDetailFactory` becomes public for it.
- Alternatives: keep it in `shared.internal.error` and exempt `ErrorController` implementations from the D-116 rule
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-168 — SpotBugs exclusions for the error handling
- Date: 2026-09-24
- Area: harness
- Decision: a SpotBugs exclude filter `config/spotbugs/exclude.xml`, wired with `spotbugs { excludeFilter }` in `build.gradle`, every entry commented with its reason: `SPRING_ENDPOINT` (find-sec-bugs SECSC, an informational list of endpoints) excluded project-wide; `SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING` only for `shared.internal.web.ProblemErrorController` (error dispatch only, a direct request is denied, stateless API without cookie sessions); `CRLF_INJECTION_LOGS` only for `shared.internal.error.UnexpectedErrorLog` (line breaks are replaced before logging, which the detector does not recognise; ECS JSON escapes them too). Escalated after spotbugs 3/3 in SOL-85.
- Alternatives: SECSC only for `ProblemErrorController`; listing all HTTP methods on the error mapping; URL-encoding or not logging the method and path; `@SuppressFBWarnings` with a new `spotbugs-annotations` dependency
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-169 — Fixed message for binding failures in `errors[]`
- Date: 2026-09-24
- Area: architecture
- Decision: a `FieldError` that is a binding failure (e.g. a wrong type bound into a `@ModelAttribute` bean) keeps its `field` in `errors[]` but gets the fixed message "Invalid value" instead of Spring's conversion text, which names Java types and reflects the input (D-156, D-162). Code stays `VALIDATION_ERROR`. Found by the SOL-85 security review.
- Alternatives: the whole binding failure as `MALFORMED_REQUEST` without `errors[]`; a Backlog issue
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-170 — External login work split into two phase 1 issues
- Date: 2026-09-24
- Area: account
- Decision: the external login work (D-138..D-149) is two phase 1 issues: (A) the gate with the Steam provider (`user_identities`, endpoint, SPI, Steam, settings, disabled without a key) and (B) choosing the username with the `USERNAME_REQUIRED` block (D-141, D-146); B is blocked by A, both by SOL-88 and SOL-91. Linking several providers (D-143) is a Backlog issue without a milestone. The issues are created in Linear now, not at the phase 1 start.
- Alternatives: one issue; three issues (gate, Steam provider, username); create at the phase 1 start
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-171 — Nullable `users` columns belong to SOL-88
- Date: 2026-09-24
- Area: account
- Decision: SOL-88 creates the `users` table with `username`, `email` and `password_hash` nullable for gate-created accounts (D-140, D-141); the gate issue adds no `ALTER TABLE` for them. SOL-97 lists `POST /api/v1/auth/external/{provider}` among the candidate rate-limited endpoints.
- Alternatives: the gate issue alters `users` in its own migration
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-172 — springdoc artifact and version
- Date: 2026-09-24
- Area: architecture
- Decision: `org.springdoc:springdoc-openapi-starter-webmvc-ui` 3.1.1 (built against Spring Boot 4.1.0), declared in `gradle/libs.versions.toml`; it provides `/v3/api-docs` and Swagger UI.
- Alternatives: `springdoc-openapi-starter-webmvc-api` without Swagger UI
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-173 — OpenAPI snapshot update through a Gradle task
- Date: 2026-09-24
- Area: harness
- Decision: `./gradlew updateOpenApiSnapshot` runs only the snapshot test with a system property that makes it overwrite `docs/api/openapi.yaml`; a normal `test` / `build` only compares and fails with the difference.
- Alternatives: a `-P` flag on the `test` task; the test writes the generated file to `build/` and the snapshot is updated by copying it
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-174 — Security for the API docs paths
- Date: 2026-09-24
- Area: architecture
- Decision: a separate `SecurityFilterChain` permits `/v3/api-docs/**`, `/swagger-ui/**` and `/swagger-ui.html`; it exists only when `springdoc.api-docs.enabled=true` (the `local` profile). In every other profile these paths fall to the application chain and answer 401 `UNAUTHORIZED` problem+json.
- Alternatives: the application chain always permits these paths, outside `local` they answer 404 `NOT_FOUND`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-175 — Shared error responses as components, referenced explicitly
- Date: 2026-09-24
- Area: architecture
- Decision: the OpenAPI components hold the `ProblemDetail` schema, one response per error status and one example per `errorCode`; every endpoint references its own error responses explicitly (D-54). springdoc's generic responses derived from `@RestControllerAdvice` are switched off (`springdoc.override-with-generic-response=false`).
- Alternatives: a customizer adds common responses (500, 400 `MALFORMED_REQUEST`, 401/403 on secured operations) to every operation automatically, endpoint-specific ones explicitly
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-176 — OpenAPI info block
- Date: 2026-09-24
- Area: architecture
- Decision: `info.title` "GameService API", `info.version` "v1" (contract version, not the Gradle project version), a short `info.description`: purpose of the service, errors as RFC 9457 `application/problem+json`, reference to the error codes.
- Alternatives: `info.version` = Gradle project version
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-177 — OpenAPI security schemes
- Date: 2026-09-24
- Area: architecture
- Decision: two security schemes: `bearerAuth` (`http`, `bearer`, `bearerFormat: JWT`) and `clientCredentials` (`oauth2`, flow `clientCredentials`, `tokenUrl` `/api/v1/auth/service-token`, no scopes). How `client_id` / `client_secret` are sent is decided by the phase 1 service-token slice.
- Alternatives: `clientBasic` (`http`, `basic`) with `client_id:client_secret` in `Authorization`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-178 — Package of the OpenAPI configuration
- Date: 2026-09-24
- Area: architecture
- Decision: the `OpenAPI` bean, the shared error components and the `SecurityFilterChain` for the docs paths (D-174) live in `com.solarianofc.gameservice.shared.internal.openapi`.
- Alternatives: the docs chain in `shared.internal.security` next to the other chains
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-179 — OpenAPI snapshot compared as text with LF
- Date: 2026-09-24
- Area: harness
- Decision: the snapshot test compares the YAML from `/v3/api-docs.yaml` with `docs/api/openapi.yaml` as exact text after normalizing line endings to LF; `.gitattributes` gets `docs/api/openapi.yaml text eol=lf`. Any change, including key order, fails the test.
- Alternatives: semantic comparison of the parsed trees, no `.gitattributes` change
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-180 — OpenAPI servers block
- Date: 2026-09-24
- Area: architecture
- Decision: the document has `servers: [{url: "/"}]`, fixed in the `OpenAPI` bean, so Swagger UI calls the same host and the random test port never reaches the snapshot.
- Alternatives: no `servers` block
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-181 — Error examples as named components
- Date: 2026-09-24
- Area: architecture
- Decision: `components/examples` holds one complete problem+json example per common `errorCode`, named after the code (e.g. `VALIDATION_ERROR` with `errors[]`, `RATE_LIMITED` with `retryAfterSeconds`); endpoints reference them through `$ref`. Modules add examples for their own codes the same way.
- Alternatives: one inline example per component response, codes listed in its description
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-182 — `errorCode` in the OpenAPI `ProblemDetail` schema
- Date: 2026-09-24
- Area: architecture
- Decision: `errorCode` is `type: string` with `pattern: ^[A-Z][A-Z0-9_]*$`; its description lists the common codes. The codes of a concrete endpoint are listed in the descriptions of its responses and in its examples; modules never change the shared schema.
- Alternatives: an `enum` of all codes, extended by modules through a customizer
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-183 — No global OpenAPI security requirement
- Date: 2026-09-24
- Area: architecture
- Decision: the document has no root-level `security`; every operation declares its own requirement (`bearerAuth`, `clientCredentials` or none for public endpoints).
- Alternatives: root `security: [bearerAuth]`, public endpoints override with `security: []`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-184 — Actuator endpoints not in the OpenAPI document
- Date: 2026-09-24
- Area: architecture
- Decision: Actuator endpoints (management port, D-122) are not part of the API contract; springdoc's default `springdoc.show-actuator=false` stays.
- Alternatives: `springdoc.show-actuator=true`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-185 — Names of the shared error responses
- Date: 2026-09-24
- Area: architecture
- Decision: `components/responses` are named after the reason phrase: `BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `MethodNotAllowed`, `Conflict`, `UnsupportedMediaType`, `TooManyRequests`, `InternalServerError`. `BadRequest` carries both examples `VALIDATION_ERROR` and `MALFORMED_REQUEST`.
- Alternatives: one response per `errorCode` (`ValidationError`, `MalformedRequest`, ...); by status (`Problem400`, ...)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-186 — Values in the error examples
- Date: 2026-09-24
- Area: architecture
- Decision: `type`, `title`, `status`, `detail` exactly as `ProblemDetailFactory` builds them (D-150, D-162); request-dependent values are neutral: `instance` "/api/v1/example", `traceId` "4bf92f3577b34da6a3ce929d0e0e4736", `CONFLICT` detail "Resource is in a conflicting state", `RATE_LIMITED` detail "Too many requests" with `retryAfterSeconds` 30, `errors[]` = [{`field` "username", `message` "size must be between 3 and 32"}].
- Alternatives: examples without `instance` / `traceId`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-187 — `/error` hidden from the OpenAPI document
- Date: 2026-09-24
- Area: architecture
- Decision: `ProblemErrorController` (D-157, D-167) carries `@io.swagger.v3.oas.annotations.Hidden`: it only serves the container's error dispatch, a direct request is denied, so `/error` is not part of the API contract. A test asserts that the document has no `/error` path.
- Alternatives: `springdoc.paths-to-exclude: /error` in `application.yml`; keep `/error` in the document
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-188 — Separate test context for springdoc-enabled tests
- Date: 2026-09-24
- Area: harness
- Decision: the springdoc-enabled integration tests keep their own cached context (own containers, ~10-25 s per full run); one meta-annotation in the test package guarantees they all share it. The `test` profile keeps springdoc off like production. Shared static containers for every context variant are a separate Backlog issue.
- Alternatives: static singleton containers in `ContainersConfiguration` within SOL-137; springdoc enabled in `application-test.yml`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-189 — OpenAPI document keys sorted
- Date: 2026-09-24
- Area: architecture
- Decision: `springdoc.writer-with-order-by-keys: true` — springdoc sorts every map of the document when serializing, so the text snapshot (D-179) is deterministic whatever map type a module uses for its schemas and examples.
- Alternatives: insertion order, guarded only by the snapshot test and ordered maps / records
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-190 — Catch-all application chain ordered last
- Date: 2026-09-24
- Area: architecture
- Decision: the application `SecurityFilterChain` (matches every request) is `@Order(Ordered.LOWEST_PRECEDENCE)`; specific chains (management 1, API docs 2, later ones) take lower values without renumbering existing chains.
- Alternatives: explicit numbering 1/2/3
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-191 — API docs chain paths from springdoc properties
- Date: 2026-09-24
- Area: architecture
- Decision: the docs chain (D-174) builds its matchers from `SpringDocConfigProperties` (`api-docs.path`, its `/**` and `.yaml` variants) and `SwaggerUiConfigProperties` (`path`, and the Swagger UI resources under `/swagger-ui/**`), so a changed path in the configuration keeps the access rule; this refines the path list written in D-174.
- Alternatives: an explicit list incl. `/v3/api-docs.yaml`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-192 — Application image: multi-stage Dockerfile
- Date: 2026-09-24
- Area: architecture
- Decision: the `app` image of Docker Compose is built by a multi-stage `Dockerfile`: the first stage runs the Gradle build inside Docker (JDK 21), the second stage is a JRE 21 runtime with the boot jar, so `docker compose up --build` works on a clean machine without a local build. A `.dockerignore` keeps the build context small. Base images are agreed in the SOL-81 spec.
- Alternatives: a Dockerfile that only copies a jar built on the host by `./gradlew bootJar`; `./gradlew bootBuildImage` (Paketo buildpacks, no Dockerfile)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-193 — Compose infrastructure images as in Testcontainers
- Date: 2026-09-24
- Area: architecture
- Decision: Docker Compose uses the same images as the Testcontainers configuration: `postgres:18-alpine`, `redis:8-alpine`, `rabbitmq:4-management-alpine` (major version fixed, minor floats, as D-96 / D-97).
- Alternatives: exact pinned versions in Compose
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-194 — Compose ports: infrastructure on loopback, management port not published
- Date: 2026-09-24
- Area: architecture
- Decision: Compose publishes the application port `8080` and the infrastructure ports PostgreSQL `5432`, Redis `6379`, RabbitMQ `5672` / `15672` (management UI) / `61613` (STOMP) bound to `127.0.0.1` only, so the `local` profile run from the IDE can use them. The management port `8081` is not published: it is reachable only inside the Compose network (Prometheus in SOL-133). This is the network isolation of D-122.
- Alternatives: additionally `8081` on `127.0.0.1`; only `8080` published, infrastructure not reachable from the host
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-195 — Compose acceptance: CI check + local run
- Date: 2026-09-24
- Area: harness
- Decision: "the application starts in Compose and `/actuator/health` = UP" is checked permanently in GitHub Actions (`docker compose up --build --wait` on the app healthcheck, then a health check) and once locally with the proof in the slice journal.
- Alternatives: local run only; a JUnit test with Testcontainers `ComposeContainer` inside `./gradlew build`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-196 — Compose credentials from a git-ignored .env
- Date: 2026-09-24
- Area: architecture
- Decision: Compose reads user names and passwords (PostgreSQL, RabbitMQ) from a git-ignored `.env`; a committed `.env.example` lists every variable (as D-147). `compose.yaml` has no default values: a missing variable fails `docker compose` with a clear message (`${VAR:?...}`). CI copies `.env.example` to `.env`.
- Alternatives: `.env` plus defaults in `compose.yaml` (`${VAR:-value}`); plain values in `compose.yaml`
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-197 — Compose runs the app without a profile
- Date: 2026-09-24
- Area: architecture
- Decision: the `app` service runs without an active profile (base `application.yml`: ECS JSON logs per D-118, springdoc off per D-56); connections and credentials come from `SPRING_*` environment variables set in `compose.yaml` from `.env`.
- Alternatives: profile `local` with host overrides; a new profile `compose` (`application-compose.yml`)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-198 — Compose volumes: PostgreSQL only
- Date: 2026-09-24
- Area: architecture
- Decision: only PostgreSQL keeps its data between runs, in a named volume; Redis and RabbitMQ are ephemeral. Reset: `docker compose down -v`.
- Alternatives: PostgreSQL + RabbitMQ; PostgreSQL + Redis + RabbitMQ; no volumes
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-199 — Dockerfile base images: Temurin Alpine, non-root
- Date: 2026-09-24
- Area: architecture
- Decision: the build stage uses `eclipse-temurin:21-jdk-alpine`, the runtime stage `eclipse-temurin:21-jre-alpine` with the plain boot jar (no layer extraction); the application runs as an unprivileged user; the container healthcheck calls `/actuator/health/readiness` on port `8081` with BusyBox `wget`.
- Alternatives: Temurin Ubuntu (noble) images with `curl`; Alpine with layered jar extraction (`-Djarmode=tools extract --layers`)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-200 — Local connection values with a RabbitMQ user of our own
- Date: 2026-09-24
- Area: architecture
- Decision: as D-86, except RabbitMQ: `application-local.yml` uses plain values that match the Compose `.env.example`: PostgreSQL `localhost:5432`, database `gameservice`, user `gameservice`, password `gameservice`; Redis `localhost:6379` without a password; RabbitMQ `localhost:5672`, user `gameservice`, password `gameservice`. Reason: RabbitMQ accepts `guest` only over loopback, and connections to the Compose broker (from the `app` container or from the IDE through the published port) are not loopback. Compose creates the user with `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS` from `.env`.
- Alternatives: keep `guest` and allow it remotely (`loopback_users = none` in `rabbitmq.conf`); an own user in Compose only, `local` profile unchanged (does not work against the Compose broker)
- Source: user (AskUserQuestion)
- Supersedes: D-86
- Status: active

### D-201 — RabbitMQ STOMP plugin from a committed enabled_plugins file
- Date: 2026-09-24
- Area: architecture
- Decision: the Compose `rabbitmq` service mounts `docker/rabbitmq/enabled_plugins` (`[rabbitmq_management,rabbitmq_stomp].`) read-only as `/etc/rabbitmq/enabled_plugins`; no custom RabbitMQ image, no command override.
- Alternatives: `command` override with `rabbitmq-plugins enable --offline rabbitmq_stomp`; an own RabbitMQ Dockerfile
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-202 — Compose check as a separate CI job
- Date: 2026-09-24
- Area: harness
- Decision: the D-195 check is a job `compose` in `.github/workflows/build.yml`, parallel to `build`, with the same triggers: copy `.env.example` to `.env`, `docker compose up --build --wait` with a wait timeout, check the health of `app`, print the logs on failure, `docker compose down -v`.
- Alternatives: a step after `./gradlew build` in the `build` job; a separate workflow file
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-203 — Compose healthchecks and start order
- Date: 2026-09-24
- Area: architecture
- Decision: every Compose service has a healthcheck — `postgres` `pg_isready`, `redis` `redis-cli ping`, `rabbitmq` `rabbitmq-diagnostics -q ping`, `app` readiness (D-199); `app` depends on the three with `condition: service_healthy`. No restart policy.
- Alternatives: the same plus `restart: unless-stopped`; a healthcheck on `app` only, `depends_on` without a condition
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-204 — Compose environment variable names
- Date: 2026-09-24
- Area: architecture
- Decision: `.env` / `.env.example` use the names the official images read: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`; `compose.yaml` passes them to `postgres` / `rabbitmq` and builds the `SPRING_*` variables of `app` from them. `.env.example` values: `gameservice` for every one (D-200).
- Alternatives: own names with a `GS_` prefix (`GS_DB_NAME`, `GS_DB_USER`, ...)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-205 — Dockerfile: Gradle cache mount, no JVM options
- Date: 2026-09-24
- Area: architecture
- Decision: the build stage runs `./gradlew bootJar --no-daemon` with a BuildKit cache mount for the Gradle user home (`RUN --mount=type=cache,target=/root/.gradle`); tests and analyzers do not run in the image build (the CI `build` job owns them). The runtime stage sets no JVM options (container-aware defaults).
- Alternatives: `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75` plus the cache mount; no options and no cache mount
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-206 — Compose healthcheck and CI timings
- Date: 2026-09-24
- Area: architecture
- Decision: healthchecks use `interval: 5s`, `timeout: 3s`, `retries: 10`; `app` additionally `start_period: 60s`. The CI `compose` job uses `docker compose up --wait --wait-timeout 180` and `timeout-minutes: 20`.
- Alternatives: interval 10s, timeout 5s, retries 12, app start period 120s, wait timeout 300, job timeout 30 minutes
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-207 — Compose usage documented in PROJECT.md §8
- Date: 2026-09-24
- Area: architecture
- Decision: how to run the stack (services, ports, `.env`, `docker compose up --build`, `docker compose down -v`) is documented in `docs/PROJECT.md` §8 with links to D-192..D-206; no README is added.
- Alternatives: a new `README.md` with a quick start; both
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-208 — Application port on loopback too
- Date: 2026-09-24
- Area: architecture
- Decision: the `app` port `8080` is published on `127.0.0.1` like the infrastructure ports; clarifies D-194, whose wording left the interface of `8080` open. Nothing in Compose is reachable from other machines.
- Alternatives: `0.0.0.0:8080` (reachable from the local network)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-209 — LF for files read inside Linux containers
- Date: 2026-09-24
- Area: harness
- Decision: `.gitattributes` pins `text eol=lf` for `Dockerfile`, `compose.yaml`, `.env.example` and `docker/**`, like `/gradlew`: with `core.autocrlf=true` a Windows checkout would give CRLF, which today's consumers tolerate but the next one (a shell script, `rabbitmq.conf`) may not, and that break would show only on Windows. From /simplify of SOL-81.
- Alternatives: no rule (every consumer tolerates CRLF today); a global `* text=auto eol=lf`
- Source: user (AskUserQuestion) — delegated to the agent ("choose what you consider important")
- Supersedes: -
- Status: active

### D-210 — Test: .env.example matches the local profile
- Date: 2026-09-24
- Area: harness
- Decision: `ProfileConfigurationTests` reads the committed `.env.example` and asserts that the `local` profile uses its database name, users and passwords (D-200); the `test` task declares `.env.example` as an input so a change of the file alone reruns the tests (as the OpenAPI snapshot, D-55). From /simplify of SOL-81.
- Alternatives: keep the match enforced by comments only
- Source: user (AskUserQuestion) — delegated to the agent
- Supersedes: -
- Status: active

### D-211 — /simplify items of SOL-81 not applied
- Date: 2026-09-24
- Area: architecture
- Decision: kept as they are: the nine `${VAR:?set it in .env (copy .env.example)}` messages in `compose.yaml` (user); no Docker layer cache in the CI `compose` job until its duration becomes a problem (user); healthcheck timings written per service, no YAML anchor, and the `build/libs/*.jar` glob in the Dockerfile, no fixed `bootJar` file name in `build.gradle` (agent, delegated). Applied without a decision (no behavior change): the Dockerfile copies `src/main` only.
- Alternatives: built-in `${VAR:?}` messages; buildx with `cache type=gha` now or as a Backlog issue; `x-healthcheck` anchor; `bootJar { archiveFileName = 'app.jar' }`
- Source: user (AskUserQuestion), partly delegated to the agent
- Supersedes: -
- Status: active

### D-212 — CI compose job listed as a sensor
- Date: 2026-09-24
- Area: harness
- Decision: `docs/HARNESS.md` §2.1 lists the CI job `compose` as a computational sensor: the stack starts in Docker Compose and `/actuator/health` is `UP` (D-195, D-202, D-206); reaction: the job fails.
- Alternatives: not listed (only in the slice file and PROJECT.md §8)
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active

### D-213 — New infrastructure files are protected configs
- Date: 2026-09-24
- Area: harness
- Decision: `.env.example`, `.dockerignore` and `docker/**` join the infrastructure group of the protected config files (`docs/HARNESS.md` §3, `.claude/hooks/protect_configs.py`): edits through Write/Edit ask the user. Proven on a real tool call (L-4).
- Alternatives: leave them unprotected
- Source: user (AskUserQuestion)
- Supersedes: -
- Status: active
