# GameService — Current State
Updated: 2026-09-23

## Position
- Phase: 0 — Skeleton
- Active slice: docs/slices/SOL-80-dependencies-and-profiles.md
- Loop step: B — spec approved 2026-09-23; waiting for the docs commit on `main`

## Next action
User commits the docs on `main` (SOL-87 leftovers + SOL-80 spec, D-82..D-89) and pushes; then the agent verifies a
clean tree, runs `git switch -c slice/SOL-80-dependencies-and-profiles` and starts TC-1.

## Blockers / waiting for user
- `stash@{0}` ("pre-pull: conflicted STATE + SOL-87 slice") can be dropped — its content was re-applied
  (copies also in the session scratchpad)

## Backlog (phase 0)
- [x] SOL-82 Spotless + Error Prone/NullAway (narrowed)  — done
- [x] SOL-138 Checkstyle + PMD + SpotBugs + JaCoCo (split from SOL-82)  — done
- [x] SOL-87 GitHub Actions CI  — done
- [ ] SOL-80 Dependencies and profiles (local, test)  <- active (spec)
- [ ] SOL-84 Base Testcontainers test and Flyway baseline
- [ ] SOL-83 Spring Modulith setup and architecture test
- [ ] SOL-86 Structured JSON logs and Actuator
- [ ] SOL-85 Global error handling + ProblemDetail
- [ ] SOL-137 OpenAPI documentation
- [ ] SOL-81 Docker Compose

## Pending Linear replication
- none (SOL-87 replicated and Done; the Linear/GitHub integration closes issues on PR merge)
