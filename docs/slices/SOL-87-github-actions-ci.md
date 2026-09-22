# SOL-87 — GitHub Actions CI: build and tests
Linear: https://linear.app/solarianofc/issue/SOL-87/github-actions-ci-build-and-tests
Status: in progress | Phase: 0
Spec approved: 2026-09-22

## Goal
Every push to `main` and every pull request runs the full sensor set (`./gradlew build`) on a clean machine,
so a green local build is confirmed independently before the next slices add real code (D-58).

## Scope
- `.github/workflows/build.yml`: triggers push to `main`, pull_request to `main`, workflow_dispatch (D-75).
- Runner `ubuntu-latest`, Temurin JDK 21 via `actions/setup-java` (D-76).
- `gradle/actions/setup-gradle` for caching and the job summary (D-77).
- Single job running `./gradlew build` (the whole sensor set from SOL-82 and SOL-138).
- On failure: upload `build/reports` and `build/test-results` with `actions/upload-artifact` (D-78).
- Action versions per D-79; `permissions: contents: read`; concurrency group cancelling superseded runs.

## Out of scope
- Testcontainers-based integration tests (they arrive with SOL-84; the workflow needs no change for them on
  `ubuntu-latest`, which has Docker).
- The manual k6 workflow (SOL-135), release/publish workflows, branch protection rules.

## Acceptance criteria
- The workflow file is valid YAML with the documented GitHub Actions schema fields.
- A real run on GitHub is green for `main` (requires the user to create the remote and push — the agent cannot push).
- A deliberately broken commit (sensor violation) makes the run red, and the uploaded artifacts contain the reports.

## Decisions
D-22, D-23, D-47, D-58, D-65, D-75, D-76, D-77, D-78, D-79

## Test cases
Acceptance level for this slice (no API): the CI run result on GitHub.
The user performs every push; the agent prepares the commits and reads the run results.

- [x] TC-1 Local pre-check: `./gradlew build` green on the slice branch (same command CI runs).
- [x] TC-2 Workflow file parses as YAML and declares the agreed triggers, runner, JDK, steps and permissions.
- [ ] TC-3 Real run: after the user pushes the slice branch and opens a PR to `main`, the run is green
      (checkout, JDK, Gradle cache, `./gradlew build`).
- [ ] TC-4 Sensor proof (D-47): a temporary commit with a deliberate violation (e.g. a star import) makes the
      run red, the log names the sensor, and the failure artifacts contain `build/reports`; the commit is then removed.
- [ ] TC-5 Merge check: after merge into `main` the push-triggered run on `main` is green.

## Journal (append-only)
- Preconditions checked: no git remote configured, `gh` not installed, `git push` is denied to the agent (D-27)
  -> user creates the GitHub remote and performs all pushes.
- Spec approved (gate 1); branch slice/SOL-87-github-actions-ci created from main (6a8e9ae).
- TC-1 green: `./gradlew build` BUILD SUCCESSFUL on the slice branch.
- TC-2 RED: structure check (scratchpad check_workflow.py, PyYAML installed with user approval) -> "workflow file
  missing". Added .github/workflows/build.yml -> 13/13 PASS (triggers, runner, permissions, concurrency,
  checkout v7 / setup-java v6 temurin 21 / setup-gradle v6 / upload-artifact v7 on failure, artifact paths,
  `./gradlew build`).
- Found before the first run: `gradlew` was staged as 100644 (Windows core.fileMode=false) -> `./gradlew` would fail
  with Permission denied on ubuntu -> asked -> D-80, `git update-index --chmod=+x gradlew` -> 100755.

## Report (filled at STOP)

## Retro (-> LESSONS L-<n>)
