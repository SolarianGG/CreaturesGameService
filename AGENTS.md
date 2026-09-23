PROJECT: GameService

LANGUAGE: Java 21

BUILD: gradlew build

TEST: gradlew test

DESCRIPTION: see docs/PROJECT.md (architecture, modules, data model, API, roadmap)





RULES:

* Never modify configs without asking
* Run test after every code change
* Never make assumptions. Any decision, detail or context you are not 100% sure about
  (requirements, design choices, naming, values, limits, libraries, versions, scope)
  MUST be clarified with the user via AskUserQuestion before acting.
  This includes values that seem like "sensible defaults" — propose them as options, do not pick silently.
* Values already written in docs/PROJECT.md that the user has not explicitly approved are drafts, not decisions —
  confirm them via AskUserQuestion before implementing. Approved = an active D-<n> in docs/DECISIONS.md;
  record every new approval there immediately (append-only, see its format)
* Never edit config files via shell commands (sed, redirects) — only via Edit/Write so the config hook can ask
* Never commit — propose a commit message, the user commits
* English only: everything in the repository (documentation, code, identifiers, comments, logs,
  error messages, config files, scripts) MUST be in English. No other languages anywhere in the project.
  This also applies to commit messages, pull requests and issues.
  Communication with the user in chat (including end-of-slice reports) is not affected.
* Record lessons (mistakes AND successes) in docs/LESSONS.md, using its entry format:
  - immediately: root cause of a mistake found, sensor fixed after >1 attempt, user correction, approach that worked well
  - on every escalation (3-attempt limit), once resolved
  - end-of-slice retro: mandatory part of the slice report
* Periodically analyze docs/LESSONS.md and propose (via AskUserQuestion) promoting the most important lessons
  into RULES or ANTI-PATTERNS here. Never change the harness (AGENTS.md, docs/HARNESS.md, hooks, sensors,
  permissions) based on a lesson without user approval.
* After creating or changing a hook: ask the user to open /hooks (or restart the session) and prove the hook
  fires on a real tool call before relying on it. A pipe-test alone is not proof. (L-4)
* Hand-over hygiene: before asking the user to commit, push, switch branches or merge, finish the state and journal
  edits, run git status --short, and list what must be in the commit — naming changes that live only in the index
  (file modes, git update-index), since Git clients drop them silently. Leave no uncommitted state edits behind. (L-16, L-17)
* Claims about git or Linear state (commits, branches, uncommitted files, issue status) only after a fresh read
  in the same turn. STATE.md blockers that depend on user actions are re-verified before being repeated;
  the start-of-session gitStatus snapshot is never evidence. (L-11)





MEMORY & STATE (details: docs/HARNESS.md §8):

* docs/STATE.md — where we are and what is next; injected at session start (hook). Keep it compact.
* docs/slices/SOL-<n>-<name>.md — one slice = one Linear issue: spec (frozen after approval)
  + test-case checklist + append-only journal.
* Linear: local files are the source of truth during work; Linear is written only at slice close (replication:
  status In Review, report comment, description = approved spec, new Backlog issues for deferred items;
  Done after the user merges). Every Linear write is previewed in the report and confirmed by the user.
* docs/DECISIONS.md — approved decisions (D-<n>); docs/LESSONS.md — lessons (L-<n>). Read on demand.
* Checkpoint: update the slice file after every test case, decision and sensor fix attempt;
  update STATE.md when position or next action changes. The Stop hook blocks if they are stale.
* The fix-attempt counter for the 3-attempt limit lives in the slice journal, not in chat.
* Agent auto-memory (outside the repo) holds personal preferences only — project facts live in the repo.





HARNESS (details: docs/HARNESS.md):

* Loop (docs/HARNESS.md §5): A. phase backlog -> B. slice spec [gate] -> C. build per test case
  -> D. verify -> E. close -> STOP [gate]
* Phase start: import the phase milestone issues from Linear, propose order/adjustments
  -> approve via AskUserQuestion before the first slice -> backlog in STATE.md
* Work unit: ONE vertical slice (= one Linear issue) per cycle, then STOP for user review
* Before coding a slice: slice spec incl. list of test cases -> approve via AskUserQuestion;
  then create branch slice/SOL-<n>-<name> from main
* API DoD: every added/changed endpoint fully documented in OpenAPI (docs/PROJECT.md §6, D-54),
  snapshot docs/api/openapi.yaml updated
* TDD outside-in: failing API-level acceptance test first, then failing unit tests inside it
  (show each failure), then minimal code
* Inner loop (after every change):
  spotlessApply (auto hook) -> gradlew compileJava compileTestJava -> gradlew test --tests '<current test case class(es)>'
* Checkpoint (test case closed): gradlew test --tests '<module package>.*' -> update slice file (and STATE.md)
* Verify (end of slice): /simplify -> gradlew build (spotlessCheck, checkstyle, pmd, spotbugs, all tests incl.
  Modulith/ArchUnit/Flyway, jacoco 70% lines / 60% branches) + /security-review if account or security code changed
  -> diff vs slice spec and active D-<n>: anything untraceable is asked or reverted (L-7)
* New sensor or rule: active only after it is shown to fail on a deliberate violation (proof in the slice journal)
* Zero tolerance: any analyzer violation fails the build; suppressions only with user approval
* Limit: max 3 fix attempts per failing sensor, then stop and ask the user
* Report at end of slice: what was done, sensor results, deviations from PROJECT.md, retro (lessons recorded in
  docs/LESSONS.md, harness change proposals), proposed commit message





ANTI-PATTERNS:

* Testing with inputs that differ from the real source: hand-building JSON payloads inside shell strings,
  or using a path/data format the real caller never sends (e.g. Git Bash /c/... paths instead of Windows paths).
  Build test payloads programmatically (Python json.dumps) in the real format; when a sensor reports
  "nothing matched", check the test input before the code under test. (L-3)
  This is about ACCIDENTAL input mismatch. Deliberately invalid input (negative tests) is required, with rules:
  - the invalid input must reach the real validation layer the way a real client would send it
    (e.g. raw JSON string, not a typed DTO that cannot even hold the invalid value);
  - assert the specific rejection reason (ProblemDetail errorCode / errors[].field, DLQ routing, etc.),
    not just the HTTP status — otherwise the test can pass for an unrelated reason.
* Creating or editing Java sources through shell commands (heredoc, sed -i): they bypass the Spotless
  PostToolUse hook and line-ending handling. Use Write/Edit; if a shell produced a Java file,
  run ./gradlew spotlessApply before the next check. (L-22)

