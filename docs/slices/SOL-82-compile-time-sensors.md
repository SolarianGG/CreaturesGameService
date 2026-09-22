# SOL-82 — Compile-time sensors: Spotless, Error Prone, NullAway
Linear: https://linear.app/solarianofc/issue/SOL-82/static-analysis-sensors-spotless-error-pronenullaway-checkstyle-pmd
Status: done | Phase: 0
Spec approved: 2026-09-22

## Goal
Every Java change is formatted and checked at compile time — formatting drift, bug patterns and null-safety
violations fail the build — before any feature code is written (D-22, D-23, D-58).

## Scope
- Green baseline: remove `spring-boot-starter-flyway` / `-flyway-test` from `build.gradle` (D-59),
  replace them with `spring-boot-starter` / `spring-boot-starter-test` (D-66, approved during TC-1).
- Version catalog `gradle/libs.versions.toml` with the compile-time sensor versions (D-60).
- Spotless with palantir-java-format for `src/**/*.java`; `spotlessCheck` runs in `check` (D-63).
- Error Prone on main and test sources, default check set, javac `-Werror` (D-61, D-62).
- NullAway on main sources only, JSpecify mode, ERROR (D-61).
- `package-info.java` with `@NullMarked` for every existing main package.
- JUnit test: every main package has `package-info.java` with `@NullMarked` (D-64).
- `spotless_apply.py` hook becomes active (it is a no-op until Spotless is in `build.gradle`) — proven live (L-4).

## Out of scope
- Checkstyle, PMD, SpotBugs, JaCoCo — separate issue split from SOL-82 (D-58).
- Spring dependencies, profiles, Flyway / DataSource (SOL-80, SOL-84).
- CI (SOL-87).

## Acceptance criteria
- `./gradlew build` is green on the skeleton with all compile-time sensors enabled.
- Each sensor is shown to fail on a deliberate violation, with the check name in the output (D-47, D-65).
- Editing a `.java` file through the agent's file tools reformats it automatically (hook proven live).

## Decisions
D-22, D-23, D-47, D-58, D-59, D-60, D-61, D-62, D-63, D-64, D-65, D-66

## Test cases
Acceptance level for this slice (no API): the sensor-failure demonstration of each test case.

- [x] TC-1 Baseline: without the Flyway starters `./gradlew build` is green (`contextLoads` passes).
- [x] TC-2 Spotless: a misformatted temporary Java file fails `spotlessCheck` naming that file;
      after `spotlessApply` the check passes.
- [x] TC-3 Hook live: writing a misformatted `.java` file with the Write/Edit tool leaves it formatted
      (`spotless_apply.py` + `-PspotlessIdeHook` work with Spotless 8).
- [x] TC-4 Error Prone ERROR check: a temporary main file with a self-assignment fails `compileJava`
      with `[SelfAssignment]`.
- [x] TC-5 Error Prone WARNING check + `-Werror`: a temporary main file with a missing `@Override`
      fails `compileJava` with `[MissingOverride]`.
- [x] TC-6 Error Prone on tests: the same WARNING violation in a temporary test file fails `compileTestJava`.
- [x] TC-7 NullAway on main: dereferencing a `@Nullable` value in a `@NullMarked` package fails
      `compileJava` with `[NullAway]`.
- [x] TC-8 NullAway not on tests: the same dereference in a temporary test file compiles
      (no `[NullAway]` error).
- [x] TC-9 `@NullMarked` presence test: a temporary main package without `package-info.java` fails the
      test naming the package; with the root package's `package-info.java` the test passes.
- [x] TC-10 Clean-up: all temporary files deleted, `./gradlew build` green.

## Journal (append-only)
- 2026-09-22 Baseline `./gradlew build` on `main` (4851019): FAILED — `contextLoads`,
  `DataSourceProperties$DataSourceBeanCreationException` (Flyway starter without DataSource) -> D-59.
- Spec approved (gate 1); branch slice/SOL-82-compile-time-sensors created from main (4851019).
- TC-1: Flyway starters are the only Spring dependencies (transitive starter/starter-test) -> asked -> D-66.
- TC-1 green: `./gradlew build` BUILD SUCCESSFUL with spring-boot-starter / -test (contextLoads passes).
- TC-2 RED: `spotlessCheck` task missing (no sensor). Added catalog + Spotless -> `spotlessJavaCheck FAILED`,
  names SpotlessProbe.java (unused `import java.util.Map` removed, imports ordered) + reformats skeleton files
  (GameServiceApplication, GameServiceApplicationTests). `spotlessApply` -> `spotlessCheck` BUILD SUCCESSFUL.
- TC-3 green (live): Write of misformatted HookProbe.java -> formatted on disk; Edit introducing bad
  formatting -> reformatted (`return 3;`). Harness reported "PostToolUse hook modified ... (likely a formatter)".
  Probe files deleted; compile + test + spotlessCheck BUILD SUCCESSFUL.
- TC-4 RED: ErrorProneErrorProbe (`value = value;`) compiled without Error Prone. Added errorprone plugin 5.1.1 +
  error_prone_core 2.50.0 -> `compileJava FAILED`: `error: [SelfAssignment] Variable assigned to itself`. Probe deleted, green.
- TC-5 RED: ErrorProneWarningProbe (toString without @Override) -> `warning: [MissingOverride]`, BUILD SUCCESSFUL.
  Added `-Werror` to all JavaCompile tasks -> `compileJava FAILED`: `error: warnings found and -Werror specified`.
- TC-6: same probe moved to src/test -> `compileTestJava FAILED` with [MissingOverride] + -Werror. No RED phase:
  Error Prone and -Werror already applied to every JavaCompile task after TC-4/TC-5. Probe deleted, green.
- TC-7 RED: NullAwayProbe (`@Nullable String text; text.length()`) compiled. JSpecify 1.0.1 was already transitive
  (Boot BOM 1.0.1); declared explicitly from the catalog. Added NullAway 0.14.1 on compileJava (error, OnlyNullMarked,
  JSpecifyMode) + root package-info @NullMarked -> `error: [NullAway] dereferenced expression 'text' is @Nullable`.
- TC-8: probe moved to src/test -> compileTestJava BUILD SUCCESSFUL. Reason check: with
  `disable('NullAway')` commented out, compileTestJava crashes with "NullAway configuration is incorrect" (NullAway
  is active on tests by default) -> the exclusion comes from the disable line. Line restored, probe deleted, green.
- Note: NullAway JSpecify mode on local JDK 21.0.5 raised no warning; full JSpecify semantics support on JDK 21
  not verified (see report).
- TC-9 RED: temp package `probe` without package-info + NullMarkedPackagesTest -> FAILED: "Expecting empty but was:
  [com.solarianofc.gameservice.probe]". Probe package deleted -> test BUILD SUCCESSFUL.
- TC-10: no *Probe* files left; `./gradlew clean build` BUILD SUCCESSFUL (spotlessCheck part of check).
- Verify: /simplify (four angles reviewed inline, diff ~60 lines) — no changes; findings skipped: explicit Spotless
  target (D-63), catalog version for jspecify (D-60), text-based @NullMarked check (behavior approved in D-64).
  Diff traced to D-59..D-66; no Cyrillic; /security-review not required (no account/security code).
- Close: report + retro filled, lessons L-12 / L-13 recorded, STATE.md -> gate 2 (review, Linear replication
  preview, commit + merge pending).

## Report (filled at STOP)
- Done: green baseline (Flyway starters -> spring-boot-starter / -test), version catalog, Spotless (palantir),
  Error Prone (main + test, -Werror), NullAway (main, JSpecify mode, OnlyNullMarked), root package-info @NullMarked,
  NullMarkedPackagesTest; spotless_apply.py hook active and proven live.
- Sensors: `./gradlew clean build` BUILD SUCCESSFUL. Failure proofs: [spotlessJavaCheck], [SelfAssignment],
  [MissingOverride] + -Werror (main and test), [NullAway], presence test naming the unmarked package.
- Deviations from the approved spec: D-66 added during TC-1 (user approved); TC-6 and TC-8 had no RED phase
  (behavior already in place), TC-8 pass reason verified by removing the disable line.
- Open points: NullAway JSpecify mode on JDK 21.0.5 gave no warning — full JSpecify semantics on JDK 21 not verified;
  the presence test reads package-info text (a commented-out annotation would pass) — candidate for ArchUnit in SOL-83.

## Retro (-> LESSONS L-<n>)
- L-12 (mistake): D-59 options offered without checking what the removed dependencies bring transitively.
- L-13 (success): verifying WHY a check passes (TC-8) by removing the mechanism under test.
