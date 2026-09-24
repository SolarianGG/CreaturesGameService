# SOL-137 — OpenAPI documentation: springdoc, security schemes, shared ProblemDetail responses, snapshot sensor
Linear: https://linear.app/solarianofc/issue/SOL-137/openapi-documentation-springdoc-security-schemes-shared-problemdetail
Status: done (awaiting review / commit) | Phase: 0
Spec approved: 2026-09-24

## Goal
The API contract is generated from code by springdoc and committed as `docs/api/openapi.yaml`; a snapshot test makes
every contract change visible in review. The document already carries the security schemes and the shared
`ProblemDetail` error components, so every phase 1 endpoint only references them to meet the D-54 DoD.

## Scope
- Dependency `org.springdoc:springdoc-openapi-starter-webmvc-ui` 3.1.1 in `gradle/libs.versions.toml` +
  `build.gradle` (D-172). Compatibility with Boot 4.1.1 / Jackson 3 is the first test case; if it fails -> stop and
  ask (D-53).
- Configuration (D-56, D-175):
  - `application.yml`: `springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`,
    `springdoc.override-with-generic-response: false`.
  - `application-local.yml`: `springdoc.api-docs.enabled: true`, `springdoc.swagger-ui.enabled: true`.
- Package `com.solarianofc.gameservice.shared.internal.openapi` (D-178):
  - `OpenAPI` bean: `info` title "GameService API", version "v1", short description (purpose, errors as RFC 9457
    `application/problem+json`, error codes) (D-176); `servers: [{url: "/"}]` (D-180).
  - Security schemes (D-177): `bearerAuth` (`http`, `bearer`, `bearerFormat: JWT`); `clientCredentials` (`oauth2`,
    flow `clientCredentials`, `tokenUrl` `/api/v1/auth/service-token`, no scopes). No root-level `security` —
    each operation declares its own (D-183).
  - Shared error components (D-175, D-181):
    - schema `ProblemDetail`: `type` (uri), `title`, `status` (int32), `detail`, `instance` (uri-reference),
      `errorCode` (string, pattern `^[A-Z][A-Z0-9_]*$`, common codes listed in the description, D-182), `traceId` (32 hex, optional), `errors[]` (optional, items `InvalidField`
      {`field`, `message`}), `retryAfterSeconds` (optional, int64); each field with description / required / format.
    - responses per status: 400, 401, 403, 404, 405 (header `Allow`), 409, 415, 429 (header `Retry-After`,
      optional), 500 — content `application/problem+json`, schema `ProblemDetail`, description lists the codes.
    - examples: one per `CommonErrorCode`, named after the code (`VALIDATION_ERROR` with `errors[]`,
      `RATE_LIMITED` with `retryAfterSeconds`), detail texts from D-162.
  - `SecurityFilterChain` for `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` with `permitAll`, created only
    when `springdoc.api-docs.enabled=true`, ordered before the application chain (D-174).
- Snapshot sensor (D-55, D-173, D-179):
  - `OpenApiSnapshotTests`: own context with springdoc enabled, GET `/v3/api-docs.yaml`, compare as exact text after
    LF normalisation with `docs/api/openapi.yaml`; failure message shows the difference and the update command.
  - With system property (name pinned at GREEN, recorded in the journal) the test overwrites the file instead.
  - Gradle task `updateOpenApiSnapshot` runs only this test with that property.
  - `.gitattributes`: `docs/api/openapi.yaml text eol=lf`.
- Docs: `docs/PROJECT.md` §6 "API documentation" (update command, components, links D-172..D-181);
  `docs/HARNESS.md` §2.1 sensor row links D-173/D-179 and the update command.

## Out of scope
- Documentation of concrete endpoints (every phase 1 slice, D-54); how `client_id` / `client_secret` are sent
  (phase 1 service-token slice, D-177).
- Actuator endpoints in the document (management port, not part of the API contract) — springdoc default
  `springdoc.show-actuator=false` stays (D-184).
- Scalar UI, spec linting (Spectral / Redocly), response validation against the spec.

## Acceptance criteria
- springdoc 3.1.1 starts on Boot 4.1.1 / Jackson 3; `/v3/api-docs` answers valid OpenAPI 3 JSON.
- The document has the info block, `servers` `/`, both security schemes and the shared error components exactly as
  above; every `CommonErrorCode` has a named example.
- `docs/api/openapi.yaml` is committed; `OpenApiSnapshotTests` is green; a deliberate contract change without a
  snapshot update fails it (proof in the journal, D-47); `./gradlew updateOpenApiSnapshot` restores green.
- springdoc enabled (local): `/v3/api-docs`, `/v3/api-docs.yaml`, `/swagger-ui/index.html` answer 200;
  springdoc disabled (base / test): the same paths answer 401 `UNAUTHORIZED` problem+json.
- `application.yml` disables and `application-local.yml` enables springdoc (profile configuration test).
- `./gradlew build` green locally and in CI; `/security-review` (new security chain).

## Decisions
D-47, D-53..D-57, D-124, D-151, D-153, D-160, D-162, D-166, D-172..D-191

## Test cases
Acceptance level: HTTP against the main port of an `@IntegrationTest` context with springdoc enabled by test
properties (the local profile needs localhost services); the disabled case uses the plain `test` profile. JSON
assertions on concrete paths; negative cases assert `errorCode`, not only the status.

- [x] TC-1 Compatibility (D-53): RED — GET `/v3/api-docs` with springdoc enabled -> 401 (no dependency / chain);
      GREEN — dependency + docs chain; 200, `openapi` starts with `3.`. Incompatibility -> stop and ask.
- [x] TC-2 Info, servers, security schemes: `info.title` / `info.version` / `servers[0].url` = `/`;
      `bearerAuth` and `clientCredentials` exactly as D-177.
- [x] TC-3 Shared error components: schema `ProblemDetail` fields, required list and formats; responses for the 9
      statuses with `application/problem+json` and `Allow` / `Retry-After` headers; examples: the set of example
      names equals the set of `CommonErrorCode` codes (breaks when a code is added without an example); each example's
      `errorCode` / `status` match its name.
- [x] TC-4 Exposure (D-174, D-56): enabled -> `/v3/api-docs`, `/v3/api-docs.yaml`, `/swagger-ui/index.html` 200;
      disabled (`test` profile) -> each 401 `UNAUTHORIZED` problem+json; `ProfileConfigurationTests`: base config
      disables, `local` enables springdoc, `override-with-generic-response` false.
- [x] TC-5 Snapshot sensor (D-55, D-173, D-179): RED — no `docs/api/openapi.yaml` -> test fails; GREEN — test +
      Gradle task, `./gradlew updateOpenApiSnapshot` writes the file, `./gradlew test` green; sensor proof: temporary
      change of `info.description` -> test fails with the difference -> reverted -> green (journal).
- [~] TC-6 Verify: `/simplify`, `./gradlew build`, `/security-review`, CI on the PR.

## Journal (append-only)
- 2026-09-24 Spec drafted from SOL-137; draft values approved as D-172..D-181.
- 2026-09-24 Open points approved as D-182..D-184; spec approved (gate 1).
- 2026-09-24 TC-1 RED: `OpenApiDocumentIntegrationTests.apiDocsAnswerAnOpenApi3Document` -> `Status expected:<200 OK>
  but was:<401 UNAUTHORIZED>` (no springdoc, path denied by the application chain).
- 2026-09-24 TC-1 GREEN: springdoc 3.1.1 starts on Boot 4.1.1 / Jackson 3, `/v3/api-docs` -> 200, `openapi` 3.x (D-53
  compatibility confirmed). `OpenApiSecurityConfiguration` (`@ConditionalOnBooleanProperty("springdoc.api-docs.enabled")`,
  `@Order(2)`); application chain moved from `@Order(2)` to `@Order(3)` so the docs chain has its own position between
  management (1) and application. Checkpoint: `shared.*`, Modularity, ArchitectureRules, Actuator, NullMarked tests +
  pmdMain pmdTest green.
- 2026-09-24 TC-2 RED: 4 of 6 fail as expected — title "OpenAPI definition", no `components.securitySchemes`,
  server `http://localhost:<random port>` "Generated server url"; the root-`security` guard passes already.
- 2026-09-24 TC-2 GREEN: `OpenApiConfiguration` (`OpenAPI` bean: info, `servers` `/`, `bearerAuth`,
  `clientCredentials`). `info.description` wording is mine, shown for review in the report.
- 2026-09-24 pmdTest attempt 1/3: `UnitTestContainsTooManyAsserts` in two TC-2 tests -> one AssertJ chain over the
  `info` map / whole scheme map (L-27 style). Checkpoint green: `shared.*`, Modularity, ArchitectureRules, PMD.
- 2026-09-24 TC-3: response names and example values asked before RED -> D-185, D-186.
- 2026-09-24 TC-3 RED: 19 of 25 `OpenApiErrorComponentsIntegrationTests` fail — no `responses`, `examples`,
  `InvalidField`; springdoc derived its own `ProblemDetail` schema from Spring's class (`instance` uri, `properties`
  object, no `errorCode`). `OpenApiDocuments.fetch` extracted from the TC-1/TC-2 test.
- 2026-09-24 TC-3 GREEN: `ProblemDetailComponents` (schemas `ProblemDetail` + `InvalidField`, 9 responses D-185,
  10 examples D-186) replaces the derived schema. Examples are built by `ProblemDetailFactory.forCode` (made `public`)
  so `type` / `title` / `detail` cannot drift from the real responses. Schema, response and header description texts
  are mine, shown for review in the snapshot.
- 2026-09-24 pmdTest attempt 1/3 (TC-3): `AvoidDuplicateLiterals` x3, `AvoidLiteralsInIfCondition` -> constants.
  Also fixed a false-green check: `String.valueOf(null)` is "null", so a missing description passed ->
  `Objects.toString(value, "")`. Checkpoint green: 77 tests (`shared.*`, Modularity, ArchitectureRules), PMD.
- 2026-09-24 TC-4 RED: profile tests (no `springdoc.*` keys), `springdocServesNeitherTheDocumentNorSwaggerUi`
  (`OpenApiResource` / `SwaggerWelcomeCommon` beans present in the `test` profile) and — a real gap —
  `/v3/api-docs.yaml` -> 401 with springdoc enabled: `/v3/api-docs/**` does not match `/v3/api-docs.yaml`.
  The disabled-side HTTP test (401 `UNAUTHORIZED` on 4 paths) was already green, because the docs chain is missing
  without the property; the bean test is what tells "off" from "closed".
- 2026-09-24 TC-4 GREEN: `/v3/api-docs.yaml` added to the docs chain matcher; `application.yml` springdoc off +
  `override-with-generic-response: false`, `application-local.yml` springdoc on (spec, D-56, D-175).
- 2026-09-24 pmdTest attempt 1/3 (TC-4): `AvoidDuplicateLiterals` "false" in `ProfileConfigurationTests`;
  attempt 2/3: `Boolean.FALSE.toString()` rejected by Error Prone `BooleanLiteral` (-Werror) -> `OFF` / `ON`
  constants. Checkpoint green: 99 tests (`shared.*`, Profile, Modularity, ArchitectureRules, Actuator), PMD.
- 2026-09-24 TC-5 RED: `OpenApiSnapshotTests` fails, no `docs/api/openapi.yaml` (expected "" but was the document).
- 2026-09-24 TC-5 GREEN: update property pinned as `openapi.snapshot.update`; Gradle task `updateOpenApiSnapshot`
  (`Test`, only this class, never up to date); `test` declares `docs/api/openapi.yaml` as an input so a snapshot-only
  change reruns it; `.gitattributes` `docs/api/openapi.yaml text eol=lf` (checked: `git check-attr` eol lf, no CR, no BOM).
- 2026-09-24 snapshot test attempt 1/3: the committed file differed from the next run — `errors[0]` of the
  `VALIDATION_ERROR` example came out `field, message` or `message, field`: `Map.of` iterates in a per-JVM random
  order. Fixed with an insertion-ordered map; 3 separate `--rerun` runs green.
- 2026-09-24 Sensor proof (D-47): `info.description` + " SENSOR PROOF." without a snapshot update -> test fails with
  exactly that line as the difference -> reverted -> green. Snapshot-only drift (appended `# drift`) -> `test` reran
  without `--rerun` and failed -> `./gradlew updateOpenApiSnapshot` -> green.
- 2026-09-24 Review of the snapshot: `/error` (`ProblemErrorController`, 7 methods, 200) was in `paths` -> asked ->
  D-187 `@Hidden`; RED `theErrorDispatchIsNotPartOfTheContract` -> GREEN, snapshot `paths: {}`.
  Checkpoint green: 101 tests (`shared.*`, Profile, Modularity, ArchitectureRules, Actuator), PMD.
- 2026-09-24 Docs: `docs/PROJECT.md` §6 "API documentation" (version, package, document-level parts, security
  schemes, shared error components, update command, exposure; links D-172..D-187) and `docs/HARNESS.md` §2.1 sensor
  row (text with LF, `updateOpenApiSnapshot`) updated per spec scope.
- 2026-09-24 TC-6 started: `/simplify` running (4 review agents: reuse, simplification, efficiency, altitude).
- 2026-09-24 /simplify (4 agents) -> asked -> D-188 (keep the separate springdoc test context; shared static
  containers -> new Backlog issue at close), D-189 (`writer-with-order-by-keys`), D-190 (application chain
  `LOWEST_PRECEDENCE`), D-191 (docs chain paths from `SpringDocConfigProperties` / `SwaggerUiConfigProperties`).
  Applied: example built from `problem.getProperties()` + `RateLimitedException#getProperties()` (no re-typed
  `errorCode` / `retryAfterSeconds`, `LinkedHashMap` workaround dropped for D-189); `codeList` helper; `OpenAPI` bean
  conditional on `springdoc.api-docs.enabled` like the docs chain; `jacoco.enabled = false` on `updateOpenApiSnapshot`;
  tests: `@SpringdocIntegrationTest` meta-annotation, `OpenApiDocuments.client`, dead `containsOnlyKeys` removed,
  9-entry response map -> `values().containsOnly`, redundant `/v3/api-docs` case dropped. Snapshot regenerated
  (keys sorted); `BadRequest` examples now `MALFORMED_REQUEST, VALIDATION_ERROR`.
  Skipped: JsonPath instead of the map-walking helpers (same size, still needs names); fetching the document once
  per class (~0.2 s, shared mutable state); shared test fixture for type URI / detail texts with
  `ErrorHandlingIntegrationTests` (the expected values are an independent check); status <-> code table from one
  source (`everyCommonErrorCodeHasANamedExample` + example status test cover it).
  Checkpoint green: 100 tests, PMD; snapshot stable in 2 `--rerun` runs.
- 2026-09-24 `./gradlew build` attempt 1/3: spotbugsMain `NP` (M D) — `problem.getProperties()` checked for null and
  called again -> local variable. Attempt 2: BUILD SUCCESSFUL (116 tests, spotless, checkstyle, PMD, SpotBugs,
  JaCoCo verification).
- 2026-09-24 PROJECT.md §6 links D-189..D-191 added; coverage after build: lines 97% (7 of 273 missed), branches
  80%. `/security-review` started (sub-agent on the working-tree diff).
- 2026-09-24 `/security-review`: no findings (docs chain matcher only springdoc paths, firewall rejects traversal;
  chain order unchanged without the docs chain; springdoc off outside `local`, OpenAPI bean + permit chain on the same
  flag; document holds no secrets / host; Swagger UI `queryConfigEnabled` default off). Next: diff vs spec, close.
- 2026-09-24 Diff vs spec and active D-<n>: every change traces to the spec or D-172..D-191. Beyond the frozen spec
  wording: application chain `@Order(LOWEST_PRECEDENCE)` (D-190, spec did not touch it), docs chain paths from the
  springdoc settings incl. `.yaml` (D-191 refines D-174), keys sorted (D-189), `/error` hidden (D-187),
  `ProblemDetailFactory.forCode` public (examples from the factory, D-186), `OpenAPI` bean conditional on
  `springdoc.api-docs.enabled` (/simplify, same flag as D-56), `test` task input + `jacoco.enabled = false` on
  `updateOpenApiSnapshot` (D-55 / D-173). TC-6 stays [~] until CI runs on the PR.

## Report
- Done: springdoc 3.1.1 on Boot 4.1.1 / Jackson 3; OpenAPI document with info `GameService API` / `v1`, `servers` `/`,
  `bearerAuth` + `clientCredentials`, shared `ProblemDetail` / `InvalidField` schemas, 9 error responses and 10
  examples built from `ProblemDetailFactory`; `/v3/api-docs`, `/v3/api-docs.yaml` and Swagger UI only in `local`
  (elsewhere 401 `UNAUTHORIZED`); committed snapshot `docs/api/openapi.yaml` with the `OpenApiSnapshotTests` sensor and
  `./gradlew updateOpenApiSnapshot`. No production endpoint added (`paths: {}`).
- Sensors: `./gradlew build` green (spotless, checkstyle, pmd, spotbugs, 116 tests incl. Modulith/ArchUnit/Flyway,
  jacoco lines 97% / branches 80%). Snapshot sensor proven by a deliberate `info.description` change (D-47). Fix
  attempts: pmdTest 1/3 at TC-2 and TC-3, 2/3 at TC-4 (PMD vs Error Prone, L-35); snapshot test 1/3 (`Map.of` order,
  L-33); spotbugs 1/3 in verify (NP on a double `getProperties()` call). `/security-review`: no findings.
- Decisions added: D-172..D-191 (D-182..D-187 while building, D-188..D-191 after /simplify).
- Deviations from PROJECT.md / spec wording: see the diff-vs-spec journal line; PROJECT.md §6 and HARNESS.md §2.1
  updated.
- Deferred (new Backlog issue at replication): shared static Testcontainers for every test context variant (D-188).

## Retro
- Went wrong: `Map.of` order broke the first snapshot (L-33); the Spotless hook removed an import between two Edits and
  a parallel Edit batch formatted an intermediate state (L-34); a PMD fix tripped Error Prone (L-35).
- Worked: reviewing the generated snapshot itself found `/error` in the contract and the `.yaml` path gap; a bean-absence
  assertion turned an already-green RED into a real test (L-36); asking the open drafts in batches before coding kept
  the build free of guessed values.
- Loop changes: none proposed beyond the lessons.
