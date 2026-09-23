# SOL-141 — Harness: PMD at the test-case checkpoint and a guard against shell writes to Java files
Linear: https://linear.app/solarianofc/issue/SOL-141/harness-pmd-at-the-test-case-checkpoint-and-a-guard-against-shell
Status: in progress | Phase: 0
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
- [ ] TC-1 Checkpoint with PMD (D-131): docs/HARNESS.md §5 C and AGENTS.md updated; `./gradlew pmdMain pmdTest` run
      once on `main` code (green) as the new checkpoint command.
- [ ] TC-2 Guard RED: the pipe-test script runs against the not yet existing hook -> fails (no script / no output).
- [ ] TC-3 Guard GREEN: `guard_java_shell_writes.py`; pipe matrix — ask: `sed -i`, `>`, `>>`, `tee`, heredoc,
      `cp`, `mv`, `Set-Content`, `Out-File`, `Add-Content`, `python -`, `python script.py X.java`; silent: grep, cat,
      `sed -n`, git diff, `./gradlew spotlessApply`, `echo > notes.md`, empty / non-JSON payload.
- [ ] TC-4 Registration + live proof: `.claude/settings.json` (protected config) gets the PreToolUse entry; user
      reloads `/hooks`; live Bash write to a scratch `.java` prompts, live grep does not; scratch file removed.
- [ ] TC-5 Docs + verify: HARNESS.md §7 table, AGENTS.md anti-pattern line; `./gradlew build` green.

## Journal (append-only)
- Preconditions: SOL-86 PR #5 merged (`origin/main` 24446c9, fresh `git fetch`), Linear SOL-86 Done. SOL-141 created
  in Linear (Backlog) per D-133. Decisions D-134..D-136 approved (AskUserQuestion).
- Spec approved (gate 1). Commit plan: one docs commit on `main` by the user (SOL-86 close + SOL-141 spec), then
  the slice branch from the clean `main`.

## Report (filled at STOP)

## Retro (-> LESSONS L-<n>)
