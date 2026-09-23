# GameService — Current State
Updated: 2026-09-24

## Position
- Phase: 0 — Skeleton
- Active slice: docs/slices/SOL-85-global-error-handling.md
- Loop step: E — closed locally; awaiting user review, commit and PR (branch slice/SOL-85-global-error-handling)

## Next action
User reviews and commits on the slice branch, pushes, opens / merges the PR. Linear replication for SOL-85 after the
user confirms the preview (In Review, report comment, description = approved spec). Then SOL-137 (OpenAPI).

## Blockers / waiting for user
- `stash@{0}` ("pre-pull: conflicted STATE + SOL-87 slice") can be dropped — its content was re-applied
  (still present on 2026-09-23, checked with `git stash list`)
- Before the Steam slice: user gets a personal Steam Web API key (steamcommunity.com/dev/apikey); one real
  `AuthenticateUserTicket` call with app id 480 decides whether it works (D-147, D-149)

## Backlog (phase 0)
- [x] SOL-82 Spotless + Error Prone/NullAway (narrowed)  — done
- [x] SOL-138 Checkstyle + PMD + SpotBugs + JaCoCo (split from SOL-82)  — done
- [x] SOL-87 GitHub Actions CI  — done
- [x] SOL-80 Dependencies and profiles (local, test)  — done
- [x] SOL-84 Base Testcontainers test and Flyway baseline  — done (PR #3 merged)
- [x] SOL-83 Spring Modulith setup and architecture test  — done (PR #4 merged, Linear Done)
- [x] SOL-86 Structured JSON logs and Actuator  — done (PR #5 merged, Linear Done)
- [x] SOL-141 Harness: PMD at checkpoint + Java shell-write guard (D-131..D-137)  — done (PR #6 merged, Linear Done)
- [x] SOL-85 Global error handling + ProblemDetail  — done locally (D-150..D-169), awaiting commit / PR
- [ ] SOL-137 OpenAPI documentation  <- next
- [ ] SOL-81 Docker Compose
- [ ] SOL-142 Harness: close the L-22 root cause (D-137)  — Backlog, position to be agreed

## Pending Linear replication
- SOL-85 -> In Review + report comment + description = approved spec (preview given to the user)
- Phase 1 issues for the external login gate + Steam (D-138..D-148) and a Backlog issue for linking several
  providers (D-143) — created at phase 1 start (step A), previewed to the user first
