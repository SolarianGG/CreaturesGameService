# GameService — Current State
Updated: 2026-09-23

## Position
- Phase: 0 — Skeleton
- Active slice: docs/slices/SOL-87-github-actions-ci.md
- Loop step: C — build (test case 4/5), waiting for a commit + push from the user

## Next action
TC-4: the user commits and pushes CiProbe.java (deliberate `[AvoidStarImport]` violation, verified locally),
we read the red run and check that the `reports` artifact was uploaded; then the probe is removed.
Handover state: CiProbe.java untracked (must be in the commit), STATE.md and the slice file modified.

## Blockers / waiting for user
- GitHub remote: the user creates the repository, adds `origin` and pushes `main`
- Live proof of `session_state.py` (needs /clear or a session restart); `state_guard.py` proven live 2026-09-22

## Backlog (phase 0)
- [x] SOL-82 Spotless + Error Prone/NullAway (narrowed)  — done
- [x] SOL-138 Checkstyle + PMD + SpotBugs + JaCoCo (split from SOL-82)  — done
- [ ] SOL-87 GitHub Actions CI  <- active
- [ ] SOL-80 Dependencies and profiles (local, test)
- [ ] SOL-84 Base Testcontainers test and Flyway baseline
- [ ] SOL-83 Spring Modulith setup and architecture test
- [ ] SOL-86 Structured JSON logs and Actuator
- [ ] SOL-85 Global error handling + ProblemDetail
- [ ] SOL-137 OpenAPI documentation
- [ ] SOL-81 Docker Compose

## Pending Linear replication
- none
