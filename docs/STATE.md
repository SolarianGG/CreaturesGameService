# GameService — Current State
Updated: 2026-09-24 18:28

## Position
- Phase: 0 — Skeleton
- Active slice: none (last: docs/slices/SOL-147-development-quality-metrics.md)
- Loop step: E — SOL-147 closed on branch `slice/SOL-147-development-quality-metrics` (uncommitted)

## Next action
User reviews and commits on `slice/SOL-147-development-quality-metrics`, pushes, opens the PR; merge -> Linear
SOL-147 Done. At the next
session start: confirm that `session_state.py` prints the dated session start line (D-233, L-4). Then agree the
next slice (phase 0 backlog below, or phase 1 step A).

## Blockers / waiting for user
- `stash@{0}` ("pre-pull: conflicted STATE + SOL-87 slice") can be dropped — its content was re-applied
  (still present on 2026-09-24, checked with `git stash list`)
- Before the Steam slice (SOL-143): user gets a personal Steam Web API key (steamcommunity.com/dev/apikey); one real
  `AuthenticateUserTicket` call with app id 480 decides whether it works (D-147, D-149)
- Local Compose stack from SOL-81 is still running (checked 2026-09-24 17:40 with `docker ps`; `docker compose down`
  to stop, `down -v` to drop the database)

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
- [x] SOL-137 OpenAPI documentation  — done (PR #8 merged, Linear Done)
- [x] SOL-81 Docker Compose  — done (PR #9 merged, CI green, Linear Done)
- [x] SOL-147 Harness: development quality metrics (D-214..D-235)  — done, waiting for commit / PR
- [ ] SOL-142 Harness: close the L-22 root cause (D-137)  — Backlog, position to be agreed
- [ ] SOL-146 Harness: shared static Testcontainers (D-188)  — Backlog, position to be agreed
- [ ] SOL-148 Harness: shell-write guard for protected config paths (D-234)  — Backlog, no milestone
- [ ] SOL-149 Harness: metrics hardening (D-235)  — Backlog, no milestone

## Phase 1 notes
- External login issues created in Linear 2026-09-24 (D-170): SOL-143 gate + Steam, SOL-144 choose username
  (blocked by SOL-143); SOL-145 linking providers — Backlog, no milestone. SOL-88 / SOL-97 updated (D-171).
  Phase 1 order is still agreed at its step A.

## Pending Linear replication
- none (SOL-147 replicated 2026-09-24 18:30: In Review, description = approved spec, report comment; Backlog issues
  SOL-148 (D-234) and SOL-149 (D-235) created, no milestone; Done after merge)
