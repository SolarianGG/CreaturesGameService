# SOL-141 — Harness: PMD at the test-case checkpoint and a guard against shell writes to Java files
Linear: https://linear.app/solarianofc/issue/SOL-141/harness-pmd-at-the-test-case-checkpoint-and-a-guard-against-shell
Status: done | Phase: 0
Spec approved: 2026-09-23

## Goal
Two harness changes from the SOL-86 retro become part of the loop: PMD violations surface at every test-case
checkpoint (L-27), and writing Java files through the shell — the L-22 anti-pattern that recurred in L-28 — is caught
by a sensor instead of a guide rule.

## Scope
- D-131: the checkpoint step runs `./gradlew pmdMain pmdTest` in addition to the module tests —
  docs/HARNESS.md §5 C and the AGENTS.md "Checkpoint" line.
- D-132, D-134, D-136: `.claude/hooks/guard_java_shell_writes.py` — PreToolUse on `Bash|PowerShell`, returns `ask`
  when the command contains a `.java` path and a write indicator (`>`/`>>`, `sed -i`, `tee`, heredoc `<<`, `cp`/`mv`,
  `Set-Content`/`Out-File`/`Add-Content`, `python`/`py`/`perl`/`node`); read-only use passes silently. Registered in
  `.claude/settings.json`.
- Documentation: docs/HARNESS.md §7 hook table; AGENTS.md anti-pattern (L-22) points to the hook.
- Proof (D-47, D-135, L-4): pipe tests with `json.dumps` payloads in the real format (L-3) from a scratchpad script,
  recorded in the journal; a live call after the user reloads hooks.

## Out of scope
- Blocking (`deny`) instead of `ask`; catching Java writes that name no `.java` path (e.g. `./gradlew spotlessApply`,
  a script that builds the path at runtime).
- A permanent hook test in the repository (D-135); any change to other hooks or sensors.

## Acceptance criteria
- Pipe tests: every write form from the scope on a `.java` path yields `permissionDecision: ask` with a reason naming
  the rule; read-only commands on `.java` files (grep, cat, `sed -n`, git diff) and writes to non-Java files yield no
  output; malformed or non-command payloads exit 0 without output.
- Live: after `/hooks`, a real Bash command writing a scratch `.java` file shows the confirmation prompt; a real
  `grep` on a `.java` file does not.
- docs/HARNESS.md §5 C, §7 and AGENTS.md describe both changes; `./gradlew build` stays green.

## Decisions
D-47, D-131, D-132, D-133, D-134, D-135, D-136; lessons L-3, L-4, L-5, L-22, L-27, L-28

## Test cases
- [x] TC-1 Checkpoint with PMD (D-131): docs/HARNESS.md §5 C and AGENTS.md updated; `./gradlew pmdMain pmdTest` run
      once on `main` code (green) as the new checkpoint command.
- [x] TC-2 Guard RED: the pipe-test script runs against the not yet existing hook -> fails (no script / no output).
- [x] TC-3 Guard GREEN: `guard_java_shell_writes.py`; pipe matrix — ask: `sed -i`, `>`, `>>`, `tee`, heredoc,
      `cp`, `mv`, `Set-Content`, `Out-File`, `Add-Content`, `python -`, `python script.py X.java`; silent: grep, cat,
      `sed -n`, git diff, `./gradlew spotlessApply`, `echo > notes.md`, empty / non-JSON payload.
- [x] TC-4 Registration + live proof: `.claude/settings.json` (protected config) gets the PreToolUse entry; user
      reloads `/hooks`; live Bash write to a scratch `.java` prompts, live grep does not; scratch file removed.
- [x] TC-5 Docs + verify: HARNESS.md §7 table, AGENTS.md anti-pattern line; `./gradlew build` green.

## Journal (append-only)
- Preconditions: SOL-86 PR #5 merged (`origin/main` 24446c9, fresh `git fetch`), Linear SOL-86 Done. SOL-141 created
  in Linear (Backlog) per D-133. Decisions D-134..D-136 approved (AskUserQuestion).
- Spec approved (gate 1). Commit plan: one docs commit on `main` by the user (SOL-86 close + SOL-141 spec), then
  the slice branch from the clean `main`.
- Docs commit 5be9ab6 on `main` (pushed; `git fetch` + `git log`); branch slice/SOL-141-... created from it.
- TC-1: docs/HARNESS.md §5 C + AGENTS.md "Checkpoint" run `./gradlew pmdMain pmdTest` (D-131); command green on the
  current code (UP-TO-DATE after the SOL-86 build).
- TC-2 RED: scratchpad `pipe_test_guard.py` (26 cases, `json.dumps` payloads in the PreToolUse format, piped as the
  harness does) against the missing hook -> 26 failures (exit 2, no such file).
- TC-3 GREEN: `.claude/hooks/guard_java_shell_writes.py` (D-134, D-136) -> 26/26 pass: ask for sed -i, `>`, `>>`,
  tee, heredoc, cp, mv, `python -` heredoc (the exact L-28 form), `python fix.py X.java`, perl -pi, Set-Content,
  Out-File, Add-Content, Get-Content|Set-Content; silent for grep, cat, `sed -n`, `head ... 2>/dev/null`, git diff,
  `./gradlew spotlessApply`, `echo > notes.md`, `find -name '*.java'`, Get-Content|Select-String, empty stdin,
  non-JSON, Edit payload. Reason text names the matched indicator(s).
- TC-4 (part 1): PreToolUse entry `Bash|PowerShell` -> `guard_java_shell_writes.py` added to `.claude/settings.json`
  (config prompt confirmed); JSON valid. Live proof pending: user reloads `/hooks` (L-4).
- TC-5 docs written ahead of the live proof: HARNESS.md §7 row, AGENTS.md L-22 anti-pattern points to the hook.
- TC-4 live (after the session was resumed with reloaded hooks): Bash `echo 'class HookProbe {}' >
  build/tmp/HookProbe.java` showed the confirmation prompt with the guard's reason (confirmed by the user in
  AskUserQuestion — the agent cannot see prompts); Bash `grep -n class build/tmp/HookProbe.java` ran without a prompt.
  Scratch file removed. `./gradlew build` green (no source change).
- /simplify (4 agents). Applied: dead `git\s+mv` alternative removed; `Copy-Item`/`Move-Item` removed — not in D-134
  (L-7; PowerShell `cp`/`mv` aliases stay caught); HARNESS.md §7 row cites D-134 instead of a third copy of the list;
  reason text cites "L-22; D-132, D-134". Skipped: shared helper for the ask JSON (standalone scripts, coupling),
  redundant tool-name check (kept as a guard against matcher edits), per-call Python start cost (same as other hooks).
  Altitude proposals -> D-137 (Backlog issue).
- Pipe tests re-run after the cleanup: 26/26. Live re-proof (hook script changed, settings not): shell write to a
  scratch .java prompted with the new reason text (confirmed by the user); scratch file removed.
- TC-5: HARNESS.md §5 C, §7 and AGENTS.md checked against the spec; `./gradlew build` green. No Java or config
  sources beyond `.claude/settings.json` changed, so /security-review is not required (no account/security code).
- Linear replicated (confirmed in the permission prompts): SOL-141 description = approved spec, In Review, report
  comment; Backlog issue SOL-142 created (D-137).
- 2026-09-23: PR #6 merged into `origin/main` (8fb1fdd, fresh `git fetch`). Linear Done not yet re-read.
- 2026-09-23, outside this slice (phase 1 planning, docs only on `main`): external login gate with a Steam provider
  approved as D-138..D-148 (AskUserQuestion); docs/PROJECT.md §1, §4.1, §5, §6, §11 and STATE.md updated. Open:
  Steam ticket type (user checks the Steamworks SDK version of UE 4.27). No code or config change.
- 2026-09-23, outside this slice: UE 4.27 ships Steamworks SDK 1.51 (read from `Steamworks.build.cs`) -> D-149
  `GetAuthSessionTicket`; fallback for the Web API key decided after the real call.

## Report (filled at STOP)
- Done: D-131 — checkpoint runs `./gradlew pmdMain pmdTest` (HARNESS.md §5 C, AGENTS.md); D-132/D-134/D-136 —
  `.claude/hooks/guard_java_shell_writes.py` (PreToolUse `Bash|PowerShell`, `ask` on a `.java` path + write
  indicator), registered in `.claude/settings.json`, documented in HARNESS.md §7 and the AGENTS.md anti-pattern.
- Sensor proof: RED 26/26 failing without the hook, GREEN 26/26 pipe cases (incl. the exact L-28 form); live: shell
  write prompts, grep does not; re-proven live after the /simplify cleanup. `./gradlew build` green.
- Deviations from the approved spec: none (the extra `Copy-Item`/`Move-Item` indicators were removed as
  untraceable). Deviations from docs/PROJECT.md: none.
- Deferred: D-137 — one Backlog issue (gitattributes line endings, explicit spotlessApply in the inner loop, IDE MCP
  write tools).

## Retro (-> LESSONS L-<n>)
- Worked: RED-first also for a hook (pipe matrix before the script existed); live proof with the user as observer
  (L-29); /simplify caught an untraceable addition (Copy-Item/Move-Item beyond D-134).
- Went wrong: nothing new; the altitude review showed the guard is a behaviour signal, the formatting guarantee
  belongs deeper (D-137).
- Harness proposals: D-137 (Backlog).
