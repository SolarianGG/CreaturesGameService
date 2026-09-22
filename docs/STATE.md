# GameService — Current State
Updated: 2026-09-22

## Position
- Phase: 0 — Skeleton
- Active slice: none
- Loop step: A done (backlog approved, D-58) — next: B for SOL-82

## Next action
Step B for SOL-82: create docs/slices/SOL-82-compile-time-sensors.md from the Linear issue
(narrowed scope: Spotless + Error Prone/NullAway), list open questions and test cases for approval.

## Blockers / waiting for user
- Commit of the step A result (D-58 + this file) on `main` before the slice branch is created
  (verified with git log/status 2026-09-22: main = 6a6c37f; D-58, STATE.md, LESSONS.md L-11, AGENTS.md rule uncommitted)
- Live proof of `session_state.py` (needs /clear or a session restart); `state_guard.py` proven live 2026-09-22

## Backlog (phase 0)
- [ ] SOL-82 Spotless + Error Prone/NullAway (narrowed)  <- next
- [ ] NEW    Checkstyle + PMD + SpotBugs + JaCoCo (split from SOL-82; Linear ID assigned at replication)
- [ ] SOL-87 GitHub Actions CI
- [ ] SOL-80 Dependencies and profiles (local, test)
- [ ] SOL-84 Base Testcontainers test and Flyway baseline
- [ ] SOL-83 Spring Modulith setup and architecture test
- [ ] SOL-86 Structured JSON logs and Actuator
- [ ] SOL-85 Global error handling + ProblemDetail
- [ ] SOL-137 OpenAPI documentation
- [ ] SOL-81 Docker Compose

## Pending Linear replication
- Create issue (project GameService, milestone "Phase 0 — Skeleton", Backlog, blocks SOL-87):
  "Build-time analysis sensors: Checkstyle, PMD, SpotBugs, JaCoCo" (split from SOL-82, D-58)
- SOL-82: title and description narrowed to Spotless + Error Prone/NullAway (with its approved spec, D-52)
- Relations (D-58): SOL-86 blocks SOL-85; SOL-84 blocks SOL-83; SOL-80 blocks SOL-83;
  SOL-86 blocks SOL-81; SOL-80 blocks SOL-86
