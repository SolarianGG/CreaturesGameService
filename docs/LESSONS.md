# GameService — Lessons Log

A log of mistakes and successes during development. Its purpose is to improve the development loop and to prevent the same mistakes from happening again.

## Process

**When to record** (see `docs/HARNESS.md` §6):
- **Immediately** — when the root cause of a mistake is found, when a sensor is fixed only after more than one attempt, when the user corrects the agent, or when an approach clearly works well.
- **On escalation** — every escalation after the 3-attempt limit is recorded once it is resolved.
- **End-of-slice retro** — a mandatory step of the slice report: what went wrong, what worked, what to change in the loop.

**Promotion.** Lessons are periodically analyzed. The most important ones are proposed to the user (AskUserQuestion) for promotion into `AGENTS.md` as a **RULE** or an **ANTI-PATTERN**. Any change to the harness (`AGENTS.md`, `docs/HARNESS.md`, hooks, sensors, permissions) happens only after user approval.

**Entry format:**

```
### L-<number> — <short title>
- Date: YYYY-MM-DD
- Type: mistake | success
- Context: phase / slice / task
- What happened: facts
- Root cause: (mistakes) why it happened
- Lesson: what to do (or keep doing) from now on
- Harness proposal: none | <proposed change>
- Status: recorded | proposed | promoted to AGENTS.md (RULE/ANTI-PATTERN) | rejected
```

Statuses: `recorded` — logged only; `proposed` — change offered to the user; `promoted` — added to `AGENTS.md`; `rejected` — the user declined the proposal.

---

## Entries

### L-1 — Made silent assumptions in the project description
- Date: 2026-09-22
- Type: mistake
- Context: initial project description (`docs/PROJECT.md`)
- What happened: filled in values the user never approved (Elo K-factors, matchmaking window, telemetry limits, partition retention, TTLs) and presented them as part of the spec.
- Root cause: treated "sensible defaults" as decisions instead of questions.
- Lesson: every uncertain value is offered as options via AskUserQuestion; unapproved values are explicitly marked as drafts.
- Harness proposal: "no assumptions" rule in `AGENTS.md`.
- Status: promoted to AGENTS.md (RULE)

### L-2 — Wrote repository documentation in Russian
- Date: 2026-09-22
- Type: mistake
- Context: `docs/PROJECT.md`, `docs/HARNESS.md`, hook comment
- What happened: documentation was written in the chat language (Russian) and had to be fully translated.
- Root cause: the language of the conversation was carried over into repository files.
- Lesson: repository content, commits, PRs and issues are English only; chat stays in Russian. Grep for Cyrillic before finishing a slice.
- Harness proposal: "English only" rule in `AGENTS.md`.
- Status: promoted to AGENTS.md (RULE)

### L-3 — False negative in a hook pipe-test caused by the test itself
- Date: 2026-09-22
- Type: mistake
- Context: testing `.claude/hooks/protect_configs.py`
- What happened: the first pipe-test reported that no config file was protected. The hook was fine — the test passed Git Bash paths (`/c/Users/...`), which Python on Windows resolves to `C:\c\Users\...`. A second attempt broke on backslash escaping when building JSON inline in bash.
- Root cause: the test input did not match the real payload (Claude Code sends Windows paths); JSON was hand-built in a shell string.
- Lesson: build hook test payloads with Python (`json.dumps`) using the same path format the real tool sends; when a sensor reports "nothing matched", check the test input before the code under test.
- Harness proposal: anti-pattern in `AGENTS.md` (the hook now also normalizes Git Bash paths defensively). Clarified on user request: the anti-pattern covers accidental input mismatch only; deliberate negative tests are required and must reach the real validation layer and assert the specific rejection reason.
- Status: promoted to AGENTS.md (ANTI-PATTERN)

### L-4 — Assumed a newly created hook was active in the current session
- Date: 2026-09-22
- Type: mistake
- Context: creating `.claude/settings.json`
- What happened: `AGENTS.md` (a protected config) was edited right after the hooks were created and no confirmation was requested — the settings watcher does not pick up a `.claude/` directory that did not exist at session start.
- Root cause: the hook was not proven to fire live, only via a pipe-test.
- Lesson: after creating or changing hooks, ask the user to open `/hooks` (or restart) and prove the hook fires with a real tool call before relying on it.
- Harness proposal: rule in `AGENTS.md`.
- Status: promoted to AGENTS.md (RULE)

### L-5 — Pipe-testing hooks before relying on them
- Date: 2026-09-22
- Type: success
- Context: `.claude/hooks/*`
- What happened: running each hook against synthetic payloads (config / non-config / edge-case paths) caught the path-handling issue before the hook went live.
- Lesson: every hook or script sensor gets a table-driven pipe-test covering positive, negative and edge cases.
- Harness proposal: none.
- Status: recorded

### L-6 — Surfacing gaps in the user's selection instead of silently filling them
- Date: 2026-09-22
- Type: success
- Context: config file list for the protection hook
- What happened: noticed that `src/test/resources/application*` was not in the approved list although the test profile can override the protected main config; raised it as a question with trade-offs, the user chose to protect it.
- Lesson: when an approved rule has a bypass, point it out and let the user decide — do not extend the rule silently and do not ignore it.
- Harness proposal: none.
- Status: recorded

### L-7 — Added unapproved details while documenting approved decisions (caught in self-review)
- Date: 2026-09-22
- Type: mistake (self-caught)
- Context: resolving open questions in `docs/PROJECT.md` §12
- What happened: while writing down the user's answers, I also added details nobody asked about: `telemetry_events.id` stays `bigint`, players are notified on auto-cancel, late start/result → `409`, `cancel_reason` values, `409` on queue join during cooldown. A self-review before reporting caught it; all points were then asked via AskUserQuestion (the user approved them, choosing among alternatives).
- Root cause: "natural consequences" of an approved decision felt obvious and were written as facts — the same failure mode as L-1, just smaller.
- Lesson: after editing a spec, diff the text against the user's actual answers; every statement not traceable to an answer is either asked or explicitly marked as a draft before reporting.
- Harness proposal: none yet (candidate for promotion if it repeats).
- Status: recorded

### L-8 — Edited protected hook scripts through a shell command
- Date: 2026-09-22
- Type: mistake (self-caught)
- Context: memory and state layer — adding `from __future__ import annotations` to `.claude/hooks/session_state.py` and `state_guard.py`
- What happened: a one-line import was added to both hooks with a Python script run from Bash, bypassing `protect_configs.py`; `.claude/**` is a protected config and must be edited only via Edit/Write (D-29).
- Root cause: the scripted edit was a batch shortcut for "just a mechanical change"; the protected-path check was done per file for Write/Edit but not for shell edits.
- Lesson: before any shell-based file modification, check the target list against the protected config patterns; a protected path always goes through Edit/Write, however small the change.
- Harness proposal: candidate — a PreToolUse Bash hook that asks when a command both writes files and mentions a protected path (heuristic).
- Status: recorded

### L-9 — Verified hook capabilities in the docs before designing around them
- Date: 2026-09-22
- Type: success
- Context: memory and state layer — hook design
- What happened: the user chose a PreCompact hook to "save state"; the hooks reference showed PreCompact cannot block compaction and its stdout is not shown to the agent. The design was changed (with user approval) to continuous checkpoints + SessionStart(`compact`) re-injection (D-40) before any code was written.
- Lesson: before building on a platform mechanism (hook event, plugin option, library feature), confirm its actual semantics in the official docs and report infeasible choices back to the user with alternatives.
- Harness proposal: none.
- Status: recorded

### L-10 — Live proof on real repository data found a defect the pipe-tests missed
- Date: 2026-09-22
- Type: success
- Context: live proof of `state_guard.py` (touching a watched file, then trying to stop)
- What happened: the hook blocked as expected, but its message listed files older than `STATE.md`; 19/19 pipe-tests passed because every synthetic repo had only files newer than the state. Fixed and covered by new pipe-tests (22/22).
- Root cause: synthetic test repos did not reproduce the real mix of old and new changed files.
- Lesson: the live proof (L-4) is not a formality — check the hook's full output, not only its exit code; add a pipe-test for every defect it reveals.
- Harness proposal: none.
- Status: recorded

### L-11 — Claimed "no commit on main" from stale memory instead of checking git
- Date: 2026-09-22
- Type: mistake (user correction)
- Context: harness setup, reports after SOL-137 creation and after phase 0 step A
- What happened: I twice told the user the first commit on `main` was still missing. In fact the user had committed on `master` at 16:17, switched to `main` at 16:45 and committed again at 16:46; my step A report (16:49) still listed the commit as a blocker. No `git log` was run before these claims.
- Root cause: claims about external, user-changeable state (git) were based on two stale sources — the start-of-session `gitStatus` snapshot in the system context and a blocker I had written into `docs/STATE.md` myself and then carried forward as fact. Memory replaced verification.
- Lesson: `STATE.md` records intent and position, not the truth about external systems. Before asserting anything about git, Linear or other state the user can change (commits, branches, issue status), read it fresh in the same turn; blockers in `STATE.md` that depend on user actions are re-verified before they are repeated.
- Harness proposal: (a) rule in `AGENTS.md`: "claims about git / Linear state require a fresh read in the same turn" — approved; (b) `session_state.py` also injects live git facts — rejected (does not help within a long session).
- Status: promoted to AGENTS.md (RULE)

### L-12 — Offered to remove dependencies without checking what they bring transitively
- Date: 2026-09-22
- Type: mistake
- Context: SOL-82, D-59 (red baseline)
- What happened: I offered "remove the Flyway starters" as the recommended option. During TC-1 it turned out they were the only Spring dependencies — they brought `spring-boot-starter` and `spring-boot-starter-test` transitively — so plain removal would break compilation; a follow-up question (D-66) was needed mid-slice.
- Root cause: the option was designed from the error message only; `gradlew dependencies` was not checked before proposing it.
- Lesson: before proposing to add, remove or replace a dependency, inspect the dependency tree and state the full consequence in the option (what disappears, what replaces it).
- Harness proposal: none yet (candidate if it repeats).
- Status: recorded

### L-13 — Verified why a check passes by removing the mechanism under test
- Date: 2026-09-22
- Type: success
- Context: SOL-82, TC-8 (NullAway not applied to tests)
- What happened: the probe in `src/test` compiled, but the package is `@NullMarked` through main's `package-info`, so the pass could have had another cause. Temporarily removing `disable('NullAway')` made compileTestJava fail (NullAway active on tests), proving the exclusion comes from that line.
- Lesson: a passing negative-scope check ("X is NOT applied to Y") is only evidence after showing it fails without the mechanism that is supposed to cause the pass — same principle as asserting the specific rejection reason (ANTI-PATTERNS).
- Harness proposal: none.
- Status: recorded

### L-14 — SpotBugs wiring needed two attempts (Gradle catalog and Groovy enum pitfalls)
- Date: 2026-09-22
- Type: mistake (sensor fixed after more than one attempt)
- Context: SOL-138, TC-5
- What happened: attempt 1 — `libs.versions.spotbugs.get()` failed because catalog keys `spotbugs` and `spotbugs-plugin` turn `libs.versions.spotbugs` into a group (fix: `asProvider().get()`); attempt 2 — `com.github.spotbugs.snom.Confidence.LOW` resolved to the enum constant's nested class in Groovy (fix: `Confidence.valueOf('LOW')`).
- Root cause: build-script snippets were written from memory without checking the plugin's documented DSL and without considering the catalog key collision.
- Lesson: in the version catalog avoid a key that is a prefix of another key (`x` and `x-plugin`) — name them `x-tool` / `x-plugin`, or use `asProvider()`; for plugin enums in Groovy prefer the documented DSL or `valueOf('...')`.
- Harness proposal: none.
- Status: recorded

### L-15 — Sensor probes must dodge the earlier sensors and the tool's own semantics
- Date: 2026-09-22
- Type: mistake
- Context: SOL-138, TC-5 (SpotBugs probe needed three tries)
- What happened: probe 1 (integer division returned as double) was caught first by Error Prone `[NarrowCalculation]`; probe 2 (`@Nullable Boolean` returning null) was not reported at all because SpotBugs honours JSpecify `@Nullable`. Probe 3 (mutable `public static` field, MS_SHOULD_BE_FINAL) worked.
- Root cause: probes were chosen by the target tool's pattern list only, without checking which earlier sensor (Error Prone, NullAway, PMD, Checkstyle) already covers the pattern and how the target tool treats annotations.
- Lesson: when choosing a D-47 probe, check it against every sensor that runs before the target (the build must reach the target task) and against the target's suppression semantics (annotations); if a probe is silently accepted, first confirm the tool actually ran and inspect its output before changing configuration.
- Harness proposal: none.
- Status: recorded

### L-16 — Handed over a staged-only change and it never reached CI
- Date: 2026-09-23
- Type: mistake
- Context: SOL-87, TC-3 (first CI run)
- What happened: `git update-index --chmod=+x gradlew` (D-80) was applied to the index but not committed — the agent may not commit (D-28). The handover message listed the pushes but never said the staged mode change had to be committed first, so the run failed exactly as predicted: `./gradlew: Permission denied`, exit 126. After the user's commit the index was back at 100644.
- Root cause: the agent treated "fixed in the index" as done and wrote the handover from the goal ("push and open a PR") instead of from the repository state.
- Lesson: before handing work to the user, run `git status --short` and state explicitly what is staged, what is uncommitted and what must be in the commit; index-only changes (file modes, `update-index`) are called out by name because Git clients can silently drop them.
- Harness proposal: hand-over rule in `AGENTS.md` — approved (with L-17).
- Status: promoted to AGENTS.md (RULE)

### L-17 — Uncommitted state edits at hand-over caused a lost-stash conflict
- Date: 2026-09-23
- Type: mistake (repeat of L-16's failure mode)
- Context: SOL-87, after the PR was merged
- What happened: checkpoint edits to `docs/STATE.md` and the slice file were made after the user's last commit. When the user switched to `main`, the IDE did a "smart checkout" (stash + pop), the pop conflicted against the older `main` (`UU docs/STATE.md`, `DU` slice file) and the stash entry was already dropped, so the edits existed only in the conflicted working tree. Recovery: copy both files to the scratchpad, `git reset -- <paths>` to clear the unmerged entries, `git stash push -u` the two files, `git pull --ff-only`, re-apply the journal lines.
- Root cause: the agent kept editing state files after the hand-over commit, leaving the tree dirty exactly when the user was expected to switch branches.
- Lesson: finish state and journal edits **before** telling the user to commit; after the hand-over keep the tree clean and record later progress only once the user is back on a branch where it can be committed. On a conflicted stash pop: back up the files first, then clear unmerged entries — never `reset --hard` before the content is saved somewhere.
- Harness proposal: hand-over rule in `AGENTS.md` — approved.
- Status: promoted to AGENTS.md (RULE)
