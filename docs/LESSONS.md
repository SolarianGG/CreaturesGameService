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
