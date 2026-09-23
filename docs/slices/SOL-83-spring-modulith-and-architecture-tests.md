# SOL-83 — Spring Modulith setup and architecture test
Linear: https://linear.app/solarianofc/issue/SOL-83/spring-modulith-setup-and-architecture-test
Status: spec | Phase: 0
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
D-6, D-22, D-47, D-85, D-101, D-102, D-104, D-105, D-106, D-107, D-108, D-109, D-110, D-111, D-112, D-113, D-114, D-115

## Test cases
Acceptance level for this slice (no API): architecture tests and a Spring context test (`@IntegrationTest`).

- [ ] TC-1 Module boundaries: RED — `ModularityTests` negative test on `com.solarianofc.archfixtures.modules`
      (import including test classes) fails while the fixture does not exist ("Expecting code to raise a throwable");
      GREEN — fixture `alpha.AlphaService` -> `beta.internal.BetaInternal`, violation names `BetaInternal`;
      `verify()` on the application passes.
- [ ] TC-2 ArchUnit + rule (1) no field injection: RED — negative test on `rules.FieldInjection` fails while the
      fixture does not exist; GREEN — archunit-junit5 dependency, `@ArchTest` rule, fixture with an `@Autowired` field.
- [ ] TC-3 Rule (2) standard streams / JUL: RED — negative tests on `rules.StandardStreams` (field access to
      `System.out`, no `println`) and `rules.JavaUtilLogging` fail while the fixtures do not exist; GREEN — rules + fixtures.
- [ ] TC-4 Rule (3) controller placement: RED — negative test on `rules.MisplacedController` fails while the fixture
      does not exist; GREEN — rule + fixture; positive fixture `rules.internal.web.PlacedController` passes the rule.
- [ ] TC-5 Outbox + async: RED — `EventPublicationRegistryIntegrationTests` "listener runs on another thread" fails
      without `@EnableAsync`; GREEN — `@EnableAsync`. Completed / incomplete / rollback assertions are expected green
      on the first run (registry from D-85/D-102) — recorded as characterization in the journal.
- [ ] TC-6 `./gradlew build` green (spotless, checkstyle, pmd incl. `pmdTest` on fixtures, spotbugs, jacoco);
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

## Report (filled at STOP)

## Retro (-> LESSONS L-<n>)
