# SOL-83 — Spring Modulith setup and architecture test
Linear: https://linear.app/solarianofc/issue/SOL-83/spring-modulith-setup-and-architecture-test
Status: done | Phase: 0
Spec approved: 2026-09-23

## Goal
Module boundaries and the agreed architecture rules become build-breaking sensors before the first module exists,
each proven by a permanent negative test, and the Event Publication Registry is proven to work as a transactional
outbox with asynchronous module listeners, so phase 1+ slices add modules and events on a verified base.

## Scope
- `ModularityTests`: `ApplicationModules.of(GameServiceApplication.class).verify()` on the application (default
  detection: direct sub-packages, D-105); a permanent negative test on the fixture root
  `com.solarianofc.archfixtures.modules` (module `alpha` uses a type from `beta.internal`) asserts that `verify()`
  fails with a violation naming the internal type (D-106, D-112).
- Dependency `com.tngtech.archunit:archunit-junit5` 1.4.2 in `gradle/libs.versions.toml` + `testImplementation` (D-107).
- `ArchitectureRulesTests`: three `@ArchTest` rules on the main classes (D-111), empty `should` allowed:
  (1) no field injection; (2) no `System.out`/`System.err` access and no `java.util.logging`;
  (3) `@RestController`/`@Controller` classes reside in `..internal.web..`.
  Each rule has a negative test on a fixture in `com.solarianofc.archfixtures.rules` asserting the violation names
  the fixture class; rule (3) also has a positive fixture in `..internal.web..` (D-112).
- `@EnableAsync` on `GameServiceApplication` (D-113).
- `EventPublicationRegistryIntegrationTests` (`@IntegrationTest`, own context via nested `@TestConfiguration`, D-114):
  test event + `@ApplicationModuleListener`s; asserts async execution, completed / incomplete publications and
  no publication after a rollback (D-108, D-109).

## Out of scope
- Application module packages (`shared`, `account`, ...) — created by the slices that fill them (D-104).
- Republish / retry of incomplete publications (D-110) and Modulith Documenter (D-115) — Backlog issues at close.
- Cleanup of completed publications (D-109); task executor pool settings (D-113).
- `@ApplicationModuleTest` / `Scenario` module tests (with the first module); Modulith observability.
- Moving `NullMarkedPackagesTest` to ArchUnit (D-111).

## Acceptance criteria
- `ModularityTests`: the application verifies; the fixture with a cross-module access to `internal` fails `verify()`
  with a violation that names `beta.internal.BetaInternal`.
- `ArchitectureRulesTests`: the three rules pass on the main classes; each negative fixture fails its rule with a
  message naming the fixture class; the positive controller fixture passes rule (3).
- `EventPublicationRegistryIntegrationTests`: the listener runs on a thread other than the publisher's; a successful
  listener -> the publication row has `completion_date`; a failing listener -> the row stays without
  `completion_date`; a rolled-back publishing transaction -> no row.
- Fixture violations need no PMD/Checkstyle suppressions; `./gradlew build` is green locally and in CI.

## Decisions
D-6, D-22, D-47, D-85, D-101, D-102, D-104, D-105, D-106, D-107, D-108, D-109, D-110, D-111 (superseded by D-116), D-112, D-113, D-114, D-115, D-116, D-117 (supersedes D-113)

## Test cases
Acceptance level for this slice (no API): architecture tests and a Spring context test (`@IntegrationTest`).

- [x] TC-1 Module boundaries: RED — `ModularityTests` negative test on `com.solarianofc.archfixtures.modules`
      (import including test classes) fails while the fixture does not exist ("Expecting code to raise a throwable");
      GREEN — fixture `alpha.AlphaService` -> `beta.internal.BetaInternal`, violation names `BetaInternal`;
      `verify()` on the application passes.
- [x] TC-2 ArchUnit + rule (1) no field injection: RED — negative test on `rules.FieldInjection` fails while the
      fixture does not exist; GREEN — archunit-junit5 dependency, `@ArchTest` rule, fixture with an `@Autowired` field.
- [x] TC-3 Rule (2) standard streams / JUL: RED — negative tests on `rules.StandardStreams` (field access to
      `System.out`, no `println`) and `rules.JavaUtilLogging` fail while the fixtures do not exist; GREEN — rules + fixtures.
- [x] TC-4 Rule (3) controller placement: RED — negative test on `rules.MisplacedController` fails while the fixture
      does not exist; GREEN — rule + fixture; positive fixture `rules.internal.web.PlacedController` passes the rule.
- [x] TC-5 Outbox + async: RED — `EventPublicationRegistryIntegrationTests` "listener runs on another thread" fails
      without `@EnableAsync`; GREEN — `@EnableAsync`. Completed / incomplete / rollback assertions are expected green
      on the first run (registry from D-85/D-102) — recorded as characterization in the journal.
- [~] TC-6 `./gradlew build` green (spotless, checkstyle, pmd incl. `pmdTest` on fixtures, spotbugs, jacoco);
      after the user's push the CI run on the PR is green.

## Journal (append-only)
- Preconditions: PR #3 merged, `main` == `origin/main`, clean tree (fresh `git status`/`fetch`). Linear SOL-83 read
  (Backlog). Build already has Modulith 2.1.1 starters core/jpa/test (D-85) and `event_publication` V2 (D-102);
  ArchUnit core 1.4.2 and Awaitility 4.3.0 are transitive via `spring-modulith-starter-test`.
- Decisions D-104..D-115 approved (AskUserQuestion, three batches; ArchUnit rule set chosen on my recommendation).
- Risk noted: `@ArchTest` fields (ArchUnit engine) and `@Test` methods (Jupiter) in one class — both engines run it;
  confirm both are reported in TC-2.
- Spec approved (gate 1), incl. the extra fixtures `rules.JavaUtilLogging` and `rules.internal.web.PlacedController`.
  Commit plan: one docs commit on `main` by the user, then the slice branch from the clean `main`.
- Docs commit 2f784a0 on `main` by the user (checked with `git log`); branch
  slice/SOL-83-spring-modulith-and-architecture-tests created from it.
- TC-1 RED 1 (fixture package absent): `IllegalArgumentException: No classes found in packages
  [com.solarianofc.archfixtures.modules]!` from `ApplicationModules.of` — valid, but not the reason in the spec, so a
  second RED step was added: fixture modules without the violation -> `Expecting code to raise a throwable.`
  (verify passes; proves the test depends on the violation itself, not on the fixture's existence).
- TC-1 green: `AlphaService` uses `beta.internal.BetaInternal` -> `Violations` with
  "depends on non-exposed type com.solarianofc.archfixtures.modules.beta.internal.BetaInternal" (the assertion
  checks this reason, not only the type name); `verify()` on the application passes (no modules yet, D-104).
  Package checkpoint green: 16 tests, 0 failed.
- TC-2: `archunit-junit5` 1.4.2 added (`libs.versions.toml` + `testImplementation`, D-107). Engine risk resolved:
  `@ArchTest` field and `@Test` method of one class are both reported (2 test cases in the XML report).
- TC-2 RED (fixture with constructor injection): negative test -> `Expecting code to raise a throwable.`; in the same
  run the `@ArchTest` rule on the main classes failed with "failed to check any classes" — the rule checks fields and
  the main classes have none -> `allowEmptyShould(true)` as D-111 foresees.
- TC-2 green: fixture with an `@Autowired` field -> violation "Field <...FieldInjection.dependency> is annotated with".
- pmdTest attempt 1/3 FAIL: `AvoidFieldNameMatchingMethodName` (fixture) and `LooseCoupling` (`JavaClasses` local
  variable) -> fixture getter renamed to `getDependency()`, helper `checkFixture(rule, classes...)` without the local
  variable; pmdTest, checkstyleTest, spotlessCheck green. Also: files created through a Bash heredoc skip the Spotless
  PostToolUse hook (spotlessCheck failed) -> `spotlessApply` run; new files go through Write from now on.
  Package checkpoint green: 18 tests, 0 failed.
- TC-3 RED (compliant fixtures): both negative tests -> `Expecting code to raise a throwable.`; the rules pass on
  the main classes.
- TC-3 finding: with the violating fixtures `standardStreamAccessViolatesTheRule` went green, but
  `javaUtilLoggingViolatesTheRule` stayed RED — `GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING` is
  `setFieldWhere(java.util.logging..)` (javap of archunit 1.4.2): it misses a direct `Logger.getLogger(...)` call.
  Asked -> D-116 (supersedes D-111): custom rule `noClasses().should().dependOnClassesThat()
  .resideInAPackage("java.util.logging..")`. Spec rule (2) now reads per D-116 (user-approved).
- TC-3 green: violations "Method <...StandardStreams.getOut()> gets field <java.lang.System.out>" and
  "Method <...JavaUtilLogging.getLogger()> calls method <java.util.logging.Logger.getLogger...".
- spotlessCheck FAIL (not a code defect): `sed -i` on the fixtures converted CRLF to LF (L-22 again) ->
  `spotlessApply`; spotlessCheck, pmdTest, checkstyleTest green. Package checkpoint green: 22 tests, 0 failed.
- TC-4 RED (fixture `MisplacedController` without annotation): `Expecting code to raise a throwable.`; the positive
  fixture `internal.web.PlacedController` (`@RestController`) passes.
- TC-4 green: `@RestController` on `MisplacedController` -> "Class <...MisplacedController> does not reside in a
  package '..internal.web..'" (rule uses `areMetaAnnotatedWith(Controller.class)`, so the violation also proves the
  meta-annotation path). The positive test runs the rule with `allowEmptyShould(false)`, so it cannot pass by
  skipping the fixture. Static checks green; package checkpoint green: 25 tests, 0 failed.
- TC-5 RED attempt 1: all 4 tests green without `@EnableAsync` — test defect: the thread assertion read
  `Thread.currentThread()` inside `untilAsserted`, which Awaitility evaluates on its own polling thread. Fixed: the
  publisher thread name is captured before `await()`.
- TC-5 RED attempt 2: still green without `@EnableAsync` — Spring Modulith 2.1.1 enables async itself
  (`EventPublicationAutoConfiguration$AsyncEnablingConfiguration`, `@EnableAsync` + `@ConditionalOnMissingBean`,
  javap). D-113 was approved on my wrong premise ("synchronous without @EnableAsync"). Asked -> D-117 (supersedes
  D-113): no `@EnableAsync`, `GameServiceApplication` unchanged. Spec items "@EnableAsync" read per D-117.
- TC-5 sensitivity proof (D-117): `SucceedingEvent` listener temporarily `@TransactionalEventListener` (synchronous)
  -> `listenerRunsOnAnotherThreadThanThePublisher` FAILED: `"Test worker" not to be equal to "Test worker"`;
  reverted (grep: no `TransactionalEventListener` left). Characterization (green on the first run, as the spec
  expected): successful listener -> status `COMPLETED` + `completion_date`; failing listener -> status `FAILED`,
  no `completion_date`; rolled-back transaction -> no row.
- TC-5 green; static checks green; package checkpoint green: 29 tests, 0 failed.
- Lessons recorded: L-22 (Bash-created Java files skip the Spotless hook), L-23 (two-step RED for negative sensor
  tests), L-24 (unverified framework default in a decision question), L-25 (Awaitility polling thread).
- Step D started: /simplify — four review agents (reuse, simplification, efficiency, altitude) running on the code
  diff (`git diff HEAD` incl. new test files); index verified clean after the diff was gathered.
- /simplify results (4 agents). Applied: `ImportOption.OnlyIncludeTests` instead of `location -> true`
  (efficiency + altitude); `assertViolation(rule, fixture, message)` helper, single-class `check`, JUL rule moved
  next to its test (simplification); `assertPublicationEventually(id, status, completed)` helper; empty bodies for the
  controller fixtures. Skipped: `Scenario` instead of TransactionTemplate + Awaitility (reuse; rollback test still
  needs TransactionTemplate, the agent itself advised to keep), shared class-import cache between ArchUnit and
  Modulith (efficiency, < 1 s). Altitude: no changes (allowEmptyShould per rule, custom JUL rule, message matching,
  SQL on event_publication confirmed at the right depth).
- While applying: the Spotless hook removed static/regular imports that were unused between two Edits
  (`tuple`, `ImportOption`) -> compile errors, imports restored. pmdTest attempt 1/3 FAIL:
  `UnitTestShouldIncludeAssert` on the two tests using the helper `awaitPublication` -> renamed to
  `assertPublicationEventually` (PMD treats `assert*` methods as assertions); green. Package checkpoint: 29 tests, 0 failed.
- TC-6: `./gradlew build` green locally (spotless, checkstyle, pmd main+test, spotbugs, all tests, jacoco
  verification). Diff vs spec and active D-<n>: `src/main` unchanged (D-104, D-117); dependency -> D-107; fixtures ->
  D-106/D-112; rules -> D-116; outbox test -> D-108/D-114/D-117; nothing untraceable. No account/security code, no
  endpoints -> no /security-review, no OpenAPI change. CI on the PR pending the user's push (TC-6 stays [~] until then).
- Close: harness proposal approved -> AGENTS.md ANTI-PATTERN for L-22 (L-22 status promoted). Linear replication
  confirmed by the user: SOL-83 description = approved spec (with the D-116/D-117 notes), status In Review, report
  comment; Backlog SOL-139 (retry policy, D-110) and SOL-140 (Documenter, D-115), both related to SOL-83.

## Report (filled at STOP)
- Done: `ModularityTests` (application `verify()` + permanent fixture negative with the exact Modulith violation);
  `ArchitectureRulesTests` (field injection, standard streams, strict java.util.logging, controllers in
  `internal.web`; one negative fixture per rule, one positive fixture); `EventPublicationRegistryIntegrationTests`
  (async listener, COMPLETED / FAILED publication rows, no row after rollback, own context); `archunit-junit5` 1.4.2.
- Sensors: build green locally, 29 tests in `com.solarianofc.gameservice`; every new sensor proven on a deliberate
  violation (two-step RED, L-23); async sensitivity proven with a temporarily synchronous listener.
- Deviations from the approved spec (both user-approved): D-116 (custom JUL rule instead of the library rule),
  D-117 (no `@EnableAsync`: Modulith enables async itself). Deviations from docs/PROJECT.md: none.
- Deferred (Backlog issues at close): retry policy for incomplete publications (D-110), Modulith Documenter (D-115).

## Retro (-> LESSONS L-<n>)
- Went wrong: Java files created or edited through Bash skipped the Spotless hook twice (L-22); a decision question
  stated a framework default from memory (L-24 -> D-113 re-asked as D-117); an Awaitility assertion read the wrong
  thread (L-25, caught by RED-first).
- Worked: two-step RED for negative sensor tests (L-23); negative tests on library rules exposed a real gap
  (JUL rule, D-116), same pattern as L-21.
- Harness proposal: see the close report (L-22 recurred within the slice).

## Retro (-> LESSONS L-<n>)
