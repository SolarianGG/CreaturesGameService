# GameService — Agent Development Harness

Approved by the user on 2026-09-22. Any change to this document, the sensors, permissions or limits must be agreed with the user (AskUserQuestion).

Harness = **guides** (what steers the agent before it acts: `AGENTS.md`, `docs/PROJECT.md`, this file) + **sensors** (what gives feedback after it acts) + **permissions/limits** (what the agent is allowed to do).

---

## 1. Ground rules

- **No assumptions.** Anything the agent is not sure about (requirements, design, values, limits, libraries, versions, naming, scope) is a question to the user via AskUserQuestion. "Sensible defaults" are offered as options, never picked silently. Values in `docs/PROJECT.md` that the user has not explicitly approved are drafts.
- **English only.** Everything in the repository — documentation, code, identifiers, comments, logs, error messages, configs, scripts — is written in English. The same applies to commit messages, pull requests and issues. Chat with the user (including end-of-slice reports) is not affected.

## 2. Sensors

### 2.1. Computational (deterministic)

| Sensor | Checks | Reaction |
|---|---|---|
| **Spotless + palantir-java-format** | Formatting | `spotlessApply` automatically (hook), `spotlessCheck` in build |
| **Error Prone + NullAway** | Bugs and NPEs at compile time | Compilation error |
| **Checkstyle** | Minimal custom rule set: naming, imports, sizes (style is owned by Spotless) | Fails the build |
| **PMD** | Minimal custom rule set: bestpractices + errorprone | Fails the build |
| **SpotBugs** | Bytecode analysis: bugs, concurrency, security | Fails the build |
| **Spring Modulith verify** | Module boundaries, no cycles, no access to other modules' `internal` | Test fails |
| **ArchUnit** | Custom architecture rules (added over time, each rule agreed with the user) | Test fails |
| **Flyway validate** | Migrations apply to a clean DB, checksums, ordering | Test fails |
| **JUnit 5 / Mockito / Testcontainers** | Behavior | Test fails |
| **OpenAPI snapshot test** | Generated OpenAPI document equals the committed `docs/api/openapi.yaml` as text with LF (D-55, D-179); intended changes: `./gradlew updateOpenApiSnapshot` (D-173) | Test fails |
| **JaCoCo** | Coverage: **≥ 70% lines, ≥ 60% branches** | Fails the build |
| **CI job `compose`** | The stack starts in Docker Compose (`up --build --wait`) and `/actuator/health` is `UP` (D-195, D-202, D-212) | Job fails |
| **Failure signal** (`sensor_events.py`, `quality_signal.py`) | The agent seems stuck (D-223): a Gradle task failed 3 times without a green run of it; a task has been failing > 60 min; the active slice costs > 2x the median of the closed slices; a journal line `attempt 3/3 \| FAIL` | Agent: stop, blocker in `STATE.md`, ask the user; user: message + desktop notification at the end of the turn (D-224, D-229, D-231) |

**Strictness: zero tolerance.** Any violation of any analyzer fails the build (warnings = errors). Suppressions (`@SuppressWarnings`, exclusions in configs) are allowed only locally, with a justification comment, and **only with the user's approval**.

Not yet approved — to be agreed **in phase 0** when the tools are added to `build.gradle`: the exact Checkstyle/PMD rule lists, plugin versions, NullAway settings (annotated packages etc.), SpotBugs configuration (effort/threshold, find-sec-bugs).

Deliberately not selected: PIT (mutation testing), separate unit/integration test tasks, OWASP Dependency-Check, generated module documentation.

### 2.2. Inferential (AI)

| Sensor | When |
|---|---|
| `/security-review` | Mandatory at the end of a slice if `account` or security code in `shared` was touched |

## 3. Permissions (`.claude/settings.json`)

| | Rules |
|---|---|
| **allow** (no confirmation) | Gradle tasks (`./gradlew …`), read-only git: `status`, `diff`, `log`, `show`, `blame`, `branch` (listing) |
| **deny** (forbidden) | `git push`, `git reset --hard`, any git command with `--force` |
| **ask** (everything else) | Confirmation prompt by default |

Commits: the agent **proposes** a commit message, **the user commits**.

### Config files

Editing these files requires confirmation (PreToolUse hook `.claude/hooks/protect_configs.py` returns `ask`):

- **Build:** `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/**`
- **Application:** `src/main/resources/application*.properties|yml|yaml`
- **Test configuration:** `src/test/resources/application*.properties|yml|yaml` — the test profile overrides the main config and controls the sensors (Flyway, Testcontainers, security), so it is protected against "tuning" tests into passing
- **Infrastructure and CI:** `docker-compose*`, `compose*.yml|yaml`, `Dockerfile*`, `.dockerignore`, `.env.example`, `docker/**` (D-213), `.github/**`, paths containing `prometheus`/`grafana`
- **Sensors and harness:** `config/**` (checkstyle/pmd/spotbugs), `.claude/**`, `AGENTS.md`

Limitation: the hook intercepts file tools only (Write/Edit/NotebookEdit). Editing configs through shell commands (sed, redirects) is not caught — the agent must not do that (rule in `AGENTS.md`).

## 4. Limits

| Limit | Value |
|---|---|
| Consecutive fix attempts for one failing sensor | **3**, then stop and ask the user (with a description of attempts and hypotheses) |
| Scope of one development cycle | **One vertical slice** (one feature end-to-end: migration + domain + service + API + tests), then stop for user review |

## 5. Development loop

Approved as D-41, D-43..D-45, D-47, D-49..D-52. Memory and state handling (checkpoints, `STATE.md`, slice files) is described in §8.

```
A. PHASE START (once per phase)                                         D-50
   - read the Linear issues of the phase milestone (with blocks relations)
   - propose order and adjustments -> user approval (AskUserQuestion) -> docs/STATE.md
   - from here on the local files are the source of truth (D-48)

B. SLICE SPEC                                             [GATE 1: spec approval]
   - create docs/slices/SOL-<n>-<name>.md from the Linear issue:            D-49
     goal, scope / out of scope, acceptance criteria,
     test cases (positive + negative with the specific rejection reason)
   - every open question -> one AskUserQuestion batch -> D-<n> in docs/DECISIONS.md
   - spec approved -> git switch -c slice/SOL-<n>-<name> from main (permission prompt)  D-51
   - STATE.md: Active slice = this file

C. BUILD, for each test case (outside-in, double loop)                   D-43
   RED      acceptance test at the API level -> run -> show it FAILS for the expected reason
            inner: unit test of domain/service code -> run -> show it FAILS
   GREEN    minimal code
   INNER LOOP (after every change)                                       D-44
            spotlessApply                            (automatic, PostToolUse hook)
            ./gradlew compileJava compileTestJava    (Error Prone + NullAway)
            ./gradlew test --tests '<test class(es) of the current test case>'
   REFACTOR -> inner loop again
   CHECKPOINT (test case closed)
            ./gradlew test --tests '<affected module package>.*'
            ./gradlew pmdMain pmdTest                (PMD per test case, not only in verify; D-131)
            slice file: [x] test case + journal line; STATE.md if the next action changed

D. VERIFY                                                                D-45
   /simplify
   OUTER LOOP
            ./gradlew build
              spotlessCheck, checkstyle, pmd, spotbugs
              all tests (Modulith verify, ArchUnit, Flyway, integration)
              jacoco 70% / 60%
            /security-review — if account / security code was touched
   diff vs slice spec and active D-<n>: every change traces to one of them,
   otherwise ask or revert (L-7)
   slice adds/changes endpoints -> documented per D-54, snapshot updated

E. CLOSE                                         [GATE 2: user review and commit]
   - slice file: status done, report, retro -> lessons in docs/LESSONS.md
   - harness change proposals -> AskUserQuestion
   - STATE.md: backlog item checked, next slice or "Active slice: none"
   - STOP -> report to the user: what was done, sensor results, deviations from
     docs/PROJECT.md, retro, proposed commit message,
     preview of every Linear write (D-52)
   - LINEAR REPLICATION (each call confirmed by the user in the permission prompt):
     status -> In Review; comment with the report; description = approved spec;
     new Backlog issues for deferred out-of-scope items (+ STATE "Pending Linear replication")
   -> user review -> the user commits and merges -> Linear issue -> Done
```

Any failing sensor -> fix -> repeat the loop. Every fix attempt is logged in the slice journal (`<sensor> attempt k/3`); after 3 failed attempts on the same sensor — stop and escalate to the user.

**New sensors and rules (D-47):** a sensor or rule is active only after it has been shown to fail on a deliberate violation; the violation is then removed and the proof recorded in the slice journal.

## 6. Lessons (`docs/LESSONS.md`)

The harness improves itself through a lessons log of mistakes **and** successes.

| Trigger | Action |
|---|---|
| Root cause of a mistake found, sensor fixed after more than one attempt, user correction, approach that worked well | Record immediately |
| Escalation after the 3-attempt limit | Record once resolved |
| End of slice | Mandatory retro in the slice report |

Promotion: lessons are periodically analyzed; the most important ones are proposed to the user via AskUserQuestion and, once approved, become a **RULE** or an **ANTI-PATTERN** in `AGENTS.md`. The harness is never changed based on a lesson without user approval.

## 7. Hooks (`.claude/hooks/`, Python)

| Hook | Event | Action |
|---|---|---|
| `protect_configs.py` | PreToolUse (Write/Edit/NotebookEdit) | Asks for confirmation for the config files in §3 |
| `guard_java_shell_writes.py` | PreToolUse (Bash/PowerShell) | Asks for confirmation when a command names a `.java` path and contains a write indicator (list in D-134) — Java sources are edited through Write/Edit only (D-132) |
| `spotless_apply.py` | PostToolUse (Write/Edit) | For `.java` files runs `spotlessApply -PspotlessIdeHook=<file>`; on failure feeds the output back to the agent. No-op until Spotless is added to `build.gradle` |
| `session_state.py` | SessionStart (`startup`, `resume`, `clear`, `compact`) | Prints `docs/STATE.md` and the active slice file into the agent context (D-38), and the session id for the journal session lines (D-222) |
| `state_guard.py` | Stop | Blocks stopping once (exit 2) if a changed file under `src/`, `build.gradle` or `docs/` is newer than `docs/STATE.md` or the active slice file; skips when `stop_hook_active`; fails open without git (D-39) |
| `sensor_events.py` | PostToolUse, PostToolUseFailure (Bash/PowerShell) | Appends every `./gradlew` run to `.claude/metrics/events.jsonl` (git-ignored; time, session, branch, tasks, result, failed tasks; D-215, D-216, D-219); checks triggers 1 and 2 (D-223, D-228) and gives the agent `additionalContext`, the user message is queued for Stop (D-231) |
| `quality_signal.py` | Stop | Triggers 2, 3, 4 (D-223, D-227) and the queued messages: `systemMessage` + `terminalSequence` (OSC 9) for the user, `additionalContext` = one continuation for the agent; never blocks, once per episode (`.claude/metrics/signal_state.json`, D-229) |

**Metrics command (D-225):** `python scripts/harness/metrics.py cost | sensors | rules | trace SOL-<n>` answers the development quality questions from the transcripts, the event log, the journals and `docs/LESSONS.md` (markdown tables; USD at API list prices, D-230).

Environment requirement: `python` (3.x) on PATH.

## 8. Memory and state

The agent's context is lost on `/clear`, compaction and new sessions. Everything needed to continue lives in repository files (D-33); chat is never the only place where state exists.

| Layer | File | Changes | Loaded |
|---|---|---|---|
| Knowledge | `AGENTS.md`, `docs/PROJECT.md`, `docs/HARNESS.md` | Rarely, with approval | `AGENTS.md` always; others on demand |
| Decisions | `docs/DECISIONS.md` | Append-only, on every approval (D-34) | On demand |
| Lessons | `docs/LESSONS.md` | Append-only (§6) | On demand |
| State | `docs/STATE.md` | Overwritten when position / next action changes | Injected by `session_state.py` |
| Slice memory | `docs/slices/SOL-<n>-<name>.md` | Checklist and journal during the slice | Injected by `session_state.py` while active |

Agent auto-memory (outside the repository) holds only personal preferences that are not project facts (D-35).

### 8.1. `docs/STATE.md` (D-36, D-48)

```
# GameService — Current State
Updated: YYYY-MM-DD HH:MM

## Position
- Phase: <n> — <name>
- Active slice: docs/slices/SOL-<n>-<name>.md | none
- Loop step: <step of the development loop> (test case <k>/<total>)

## Next action
<one concrete next step>

## Blockers / waiting for user
- none | <item>

## Backlog (phase <n>)
- [x] SOL-<n> <slice>
- [ ] SOL-<n> <slice>  <- active

## Pending Linear replication
- none | <item to create/update in Linear at the next replication>
```

The `Active slice:` line is parsed by the hooks — keep the path in that exact form.

### 8.2. `docs/slices/SOL-<n>-<name>.md` (D-49)

```
# SOL-<n> — <Name>
Linear: <issue URL>
Status: spec | in progress | done | Phase: <n>
Spec approved: YYYY-MM-DD

## Goal
## Scope / Out of scope
## Acceptance criteria
## Decisions            (links to D-<n>)

## Test cases
- [x] TC-1 <description>          (done)
- [~] TC-2 <description>          (in progress)
- [ ] TC-3 <description>

## Journal (append-only)
- YYYY-MM-DD HH:MM | session | <session id> | start
- YYYY-MM-DD HH:MM TC-1 green
- YYYY-MM-DD HH:MM | <sensor> | attempt 1/3 | FAIL | <rule / ref> | <reason> -> <fix>
- YYYY-MM-DD HH:MM | session | <session id> | end

## Report (filled at STOP)
## Retro (-> LESSONS L-<n>)
```

Everything above `## Test cases` is the approved spec: it is not changed without user approval. The journal holds the fix-attempt counter for the 3-attempt limit (§4), so it survives `/clear`.

**Journal line format (D-217, D-222).** Every line starts with the local date and time `YYYY-MM-DD HH:MM`. Two kinds of lines have pipe-separated fields, the only lines scripts parse (`scripts/harness/metrics.py`):

- **Sensor event:** `| <sensor> | attempt k/3 | FAIL|PASS | <rule / ref> | <short text>` — `<sensor>` is the Gradle task (`compileJava`, `pmdTest`, `test`, …) or the sensor name from §2; `<rule / ref>` names the analyzer rule, `D-<n>` or `L-<n>` concerned, `-` if none. The line `attempt 3/3 | FAIL` is the escalation of §4.
- **Session:** `| session | <session id> | start|end` — written when a session starts or stops working on the slice; `session_state.py` prints the complete start line with the current time at session start (D-233), and every other time is read from a clock, never estimated. Usage of that session between `start` and `end` (or the next `start`) counts for the slice even on `main` (D-222).

All other lines are free text after the timestamp. Journals written before SOL-147 are not converted.

### 8.3. Protocol

- **Session start:** state is injected automatically; read `DECISIONS.md` / `LESSONS.md` when the task touches them.
- **Checkpoints (D-40):** update the slice file after every test case, every decision and every sensor fix attempt; update `STATE.md` whenever the position or the next action changes. There is no PreCompact hook — it cannot make the agent save anything — so state must already be on disk when compaction happens.
- **Before stopping:** `STATE.md` and the slice file reflect the latest work; `state_guard.py` enforces this.
- **End of slice:** slice status `done`, report and retro filled, backlog item checked, `Active slice: none` or the next slice.
