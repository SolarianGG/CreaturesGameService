# SOL-138 — Build-time sensors: Checkstyle, PMD, SpotBugs, JaCoCo
Linear: https://linear.app/solarianofc/issue/SOL-138/build-time-analysis-sensors-checkstyle-pmd-spotbugs-jacoco
Status: done | Phase: 0
Spec approved: 2026-09-22

## Goal
Naming/import/size rules, bug and security patterns in bytecode and test coverage are enforced by `./gradlew build`
with zero tolerance, completing the sensor set from docs/HARNESS.md §2.1 before CI (SOL-87) is added (D-22..D-24, D-58).

## Scope
- Versions in `gradle/libs.versions.toml` (D-67).
- Checkstyle with `config/checkstyle/checkstyle.xml` (D-68), on main and test (D-71), any violation fails the build.
- PMD with `config/pmd/ruleset.xml` = bestpractices + errorprone (D-69), on main and test (D-71), any violation fails.
- SpotBugs effort max, threshold low, find-sec-bugs (D-70), main only (D-71), any finding fails the build.
- JaCoCo report + `jacocoTestCoverageVerification` in `check`: ≥ 70% lines, ≥ 60% branches, BUNDLE;
  `GameServiceApplication` excluded with a comment (D-72).

## Out of scope
- Compile-time sensors (done in SOL-82). CI (SOL-87). Spring dependencies (SOL-80).
- Rule exclusions: only when a real finding appears, each with user approval.

## Acceptance criteria
- `./gradlew build` is green on the skeleton and runs all four sensors as part of `check`.
- Each sensor is shown to fail on a deliberate violation, with the rule / pattern / counter name in the output (D-47, D-65).

## Decisions
D-22, D-23, D-24, D-47, D-58, D-60, D-65, D-67, D-68, D-69, D-70, D-71, D-72

## Test cases
Acceptance level for this slice (no API): the sensor-failure demonstration of each test case.
The concrete probe rule / pattern is chosen so that Error Prone (SOL-82) does not catch it first.

- [x] TC-1 Checkstyle on main: a temporary main file with a naming violation and a star import fails
      `checkstyleMain` naming the check (e.g. `MethodName`, `AvoidStarImport`).
- [x] TC-2 Checkstyle on tests: the same violation in a temporary test file fails `checkstyleTest`.
- [x] TC-3 PMD errorprone on main: a temporary main file with an errorprone-category violation fails `pmdMain`
      naming the rule.
- [x] TC-4 PMD bestpractices on tests: a temporary test file with a bestpractices violation fails `pmdTest`
      naming the rule.
- [x] TC-5 SpotBugs on main: a temporary main file with a bug pattern fails `spotbugsMain` naming the pattern.
- [x] TC-6 find-sec-bugs: a temporary main file with a security pattern fails `spotbugsMain` naming the pattern.
- [x] TC-7 SpotBugs not on tests: the TC-5 pattern in a temporary test file does not fail the build;
      reason verified by showing that it fails when SpotBugs runs on tests (L-13).
- [x] TC-8 JaCoCo lines: a temporary untested main class drops line coverage below 70% and fails
      `jacocoTestCoverageVerification` naming the lines counter; without it the skeleton passes
      (GameServiceApplication excluded).
- [x] TC-9 JaCoCo branches: a temporary main class with an untested branch drops branch coverage below 60%
      and fails verification naming the branches counter.
- [x] TC-10 Clean-up: all temporary files deleted, `./gradlew clean build` green, all four sensors in `check`.

## Journal (append-only)
- Spec approved (gate 1); branch slice/SOL-138-build-time-sensors created from main (472cd07).
- TC-1 RED: CheckstyleProbe (`import java.util.*`, method `Bad_Name`) -> build green without Checkstyle. Added
  Checkstyle 14.1.0 + config/checkstyle/checkstyle.xml (D-68), maxWarnings 0 -> `checkstyleMain FAILED`:
  [AvoidStarImport], [MethodName]. Output was Russian (JVM locale) -> asked -> D-73 localeLanguage=en -> English.
- TC-2: probe moved to src/test -> `checkstyleTest FAILED` with the same two checks (no RED phase: the plugin
  covers test sources by default). Probe deleted, `./gradlew build` green.
- TC-3 RED: PmdProbe (`while ((read = reader.read()) != -1)`) -> build green without PMD. Added PMD 7.27.0 +
  config/pmd/ruleset.xml (D-69) -> `pmdMain FAILED`: `AssignmentInOperand: Avoid assignment to read in operand`.
- TC-4 (deviation: real finding instead of a temporary file): the same run failed `pmdTest` on the skeleton test —
  `GameServiceApplicationTests.java:10 UnitTestShouldIncludeAssert` -> asked -> D-74: contextLoads asserts the
  GameServiceApplication bean -> pmdTest green. Probe deleted, `./gradlew build` green.
- Observation: PMD prints "Adding current platform ...jrt-fs.jar to auxClasspath, which could be the wrong java
  version" (tool notice, not a violation; the toolchain JDK is the same 21) — see report.
- TC-5 probe 1 (`return wins / games` as double, ICAST_IDIV_CAST_TO_DOUBLE) rejected: Error Prone caught it first
  (`[NarrowCalculation]` + -Werror). Probe 2 (`@Nullable Boolean` returning null, NP_BOOLEAN_RETURN_NULL) -> RED:
  build green without SpotBugs.
- SpotBugs wiring attempt 1/3 FAIL: `libs.versions.spotbugs.get()` — catalog keys spotbugs + spotbugs-plugin make
  it a group -> `asProvider().get()`. Attempt 2/3 FAIL: `Confidence.LOW` resolved to the enum constant's nested
  class in Groovy -> `Confidence.valueOf('LOW')`. Build then ran SpotBugs (effort MAX, report level LOW) but found
  nothing: SpotBugs honours JSpecify @Nullable, so probe 2 was not a violation.
- Probe 3 (mutable `public static String defaultRegion`) -> `spotbugsMain FAILED`:
  `H V MS: ...SpotBugsProbe.defaultRegion isn't final but should be` (MS_SHOULD_BE_FINAL). Probe deleted.
- TC-6 RED: FindSecBugsProbe (`MessageDigest.getInstance("MD5")`) -> build green. Added find-sec-bugs 1.14.0 to
  spotbugsPlugins -> `spotbugsMain FAILED`: `H S SECMD5: This API MD5 (MDX) is not a recommended cryptographic hash
  function` (WEAK_MESSAGE_DIGEST_MD5). Probe deleted.
- TC-7 RED + reason: MS probe in src/test -> `spotbugsTest FAILED` (`H V MS: ...defaultRegion isn't final`), i.e.
  SpotBugs does run on tests until excluded. (Also spotlessJavaCheck failed: the probe was written via shell,
  bypassing the format hook -> spotlessApply.) Added `spotbugsTest.enabled = false` (D-71) -> build green,
  `spotbugsTest SKIPPED`. Probe deleted.
- TC-8 RED: untested CoverageProbe -> build green without JaCoCo. Added JaCoCo 0.8.15, report after test,
  verification in check (BUNDLE, LINE 0.70, BRANCH 0.60), GameServiceApplication excluded -> 
  `jacocoTestCoverageVerification FAILED`: `lines covered ratio is 0.00, but expected minimum is 0.70`.
  Probe deleted -> build green. Reason check: exclusion list emptied -> `lines covered ratio is 0.33` FAILED;
  exclusion restored. Note: with the exclusion the skeleton has no measurable code, so verification passes on 0/0.
- TC-9: BranchProbe (`score > 0 ? "win" : "loss"`) + BranchProbeTest covering one branch -> `branches covered ratio
  is 0.50, but expected minimum is 0.60` (lines pass). No separate RED: the BRANCH limit was added with LINE in TC-8.
  Probe + test deleted.
- TC-10: no *Probe* files; `./gradlew clean build` BUILD SUCCESSFUL; check runs checkstyleMain/Test, pmdMain/Test,
  spotbugsMain (spotbugsTest SKIPPED), jacocoTestCoverageVerification, spotlessCheck, test.
- Verify: /simplify (reviewed inline) -> two simplifications: JaCoCo exclusions via one
  `tasks.withType(JacocoReportBase)` block; `finalizedBy jacocoTestReport` merged into the existing `test` block.
  `./gradlew clean build` green again (exclusion still effective: without it the skeleton fails at 0.33).
  Diff traced to D-67..D-74; no Cyrillic; /security-review not required.
- Close: report + retro filled, lessons L-14 / L-15, STATE.md -> gate 2.
- Linear replication (user confirmed): SOL-138 -> In Review, description = approved spec, report comment.
- Merged into main (verified: 6a8e9ae, fast-forward; clean build on main green); SOL-138 -> Done in Linear.

## Report (filled at STOP)
- Done: Checkstyle 14.1.0 (config/checkstyle/checkstyle.xml, English output), PMD 7.27.0 (config/pmd/ruleset.xml:
  bestpractices + errorprone), SpotBugs 4.10.4 (effort MAX, level LOW) + find-sec-bugs 1.14.0 on main only,
  JaCoCo 0.8.15 (report after test, verification in check: BUNDLE lines >= 70%, branches >= 60%,
  GameServiceApplication excluded); contextLoads now asserts the application bean (D-74).
- Sensors: `./gradlew clean build` BUILD SUCCESSFUL. Failure proofs: [AvoidStarImport], [MethodName] (main + test),
  AssignmentInOperand (pmdMain), UnitTestShouldIncludeAssert (pmdTest, real finding), MS_SHOULD_BE_FINAL
  (spotbugsMain), SECMD5 (find-sec-bugs), spotbugsTest failing before exclusion, lines 0.00 < 0.70, branches 0.50 < 0.60.
- Deviations from the approved spec: D-73 (English Checkstyle output) and D-74 (contextLoads assertion) added during
  the slice with user approval; TC-4 proven on a real finding instead of a temporary file; TC-9 had no separate RED.
- Open points: JaCoCo passes on 0/0 until the first real code exists (skeleton code is excluded); PMD prints an
  auxClasspath notice about jrt-fs.jar (tool notice, not a violation); SpotBugs wiring took 2 attempts (L-14).

## Retro (-> LESSONS L-<n>)
- L-14 (mistake): catalog key collision and Groovy enum resolution in the SpotBugs DSL — 2 attempts.
- L-15 (mistake): probes must not be caught by earlier sensors or suppressed by the tool's annotation semantics.
