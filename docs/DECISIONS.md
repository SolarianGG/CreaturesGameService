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
- Status: active

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
