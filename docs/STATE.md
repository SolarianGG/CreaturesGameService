# GameService — Current State
Updated: 2026-09-24

## Position
- Phase: 0 — Skeleton
- Active slice: none (last: docs/slices/SOL-85-global-error-handling.md)
- Loop step: E — SOL-85 merged as PR #7 (6d54557 on `origin/main`, fresh `git fetch` 2026-09-24)

## Next action
User commits the D-170/D-171 docs on `main` (SOL-85 close-out is in e2c8aea). Then step B for SOL-137 (OpenAPI).

## Blockers / waiting for user
- `stash@{0}` ("pre-pull: conflicted STATE + SOL-87 slice") can be dropped — its content was re-applied
  (still present on 2026-09-23, checked with `git stash list`)
- Before the Steam slice (SOL-143): user gets a personal Steam Web API key (steamcommunity.com/dev/apikey); one real
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
- [x] SOL-85 Global error handling + ProblemDetail  — done (PR #7 merged)
- [ ] SOL-137 OpenAPI documentation  <- next
- [ ] SOL-81 Docker Compose
- [ ] SOL-142 Harness: close the L-22 root cause (D-137)  — Backlog, position to be agreed

## Phase 1 notes
- External login issues created in Linear 2026-09-24 (D-170): SOL-143 gate + Steam, SOL-144 choose username
  (blocked by SOL-143); SOL-145 linking providers — Backlog, no milestone. SOL-88 / SOL-97 updated (D-171).
  Phase 1 order is still agreed at its step A.
