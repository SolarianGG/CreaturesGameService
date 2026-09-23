# SOL-85 — Global error handling: @RestControllerAdvice + ProblemDetail
Linear: https://linear.app/solarianofc/issue/SOL-85/global-error-handling-restcontrolleradvice-problemdetail
Status: done | Phase: 0
Spec approved: 2026-09-23

## Goal
Every error response of the main port is an RFC 9457 `application/problem+json` with `errorCode`, optional
`traceId` and, for validation, `errors[]` — whether it comes from Spring MVC, the security chains or the `/error`
dispatch — and modules get one typed way (`ApiException` + their own `ErrorCode` enum) to raise business errors, so
phase 1 endpoints and the OpenAPI slice (SOL-137) build on a fixed error contract.

## Scope
- Public API in `com.solarianofc.gameservice.shared.error` (Modulith named interface, D-161):
  `interface ErrorCode { String code(); }`, `enum CommonErrorCode implements ErrorCode` with the D-151 codes (D-158),
  abstract `ApiException` (HTTP status, `ErrorCode`, detail, optional extra properties) (D-152); a way to raise
  `RATE_LIMITED` with an optional `Duration` (D-160).
- Implementation in `shared.internal.error` (D-161):
  - `ProblemDetail` factory: `type` = `https://gameservice.local/problems/<kebab errorCode>` (D-150), `title` = HTTP
    reason phrase, `status`, `detail`, `instance` = request path, `errorCode`, `traceId` from the Micrometer `Tracer`,
    omitted without a current span (D-155).
  - `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` (Boot's own `ProblemDetailsExceptionHandler`
    stays off, `spring.mvc.problemdetails.enabled` default `false`):
    `MethodArgumentNotValidException` / `HandlerMethodValidationException` -> 400 `VALIDATION_ERROR` + `errors[]`
    (D-159); unreadable body, parameter type mismatch, missing parameter -> 400 `MALFORMED_REQUEST`;
    `NoResourceFoundException` / `NoHandlerFoundException` -> 404 `NOT_FOUND`; 405 `METHOD_NOT_ALLOWED` (with `Allow`);
    415 `UNSUPPORTED_MEDIA_TYPE`; `ApiException` -> its status / code / properties; `RATE_LIMITED` with a duration ->
    `Retry-After` header + `retryAfterSeconds` (D-160); any other exception -> 500 `INTERNAL_ERROR` with a generic
    detail, logged at `ERROR` with stack trace; 4xx not logged (D-156). Any other framework exception handled by
    `ResponseEntityExceptionHandler` keeps its status and gets the nearest D-151 code (listed in the journal at
    GREEN; asked if one does not fit).
  - `ErrorController` replacing `BasicErrorController`: the `/error` dispatch answers the same `ProblemDetail`
    (status from the request attributes, `INTERNAL_ERROR` for 5xx) (D-157).
- `shared.internal.security` (D-153, D-157): `AuthenticationEntryPoint` -> 401 `UNAUTHORIZED`,
  `AccessDeniedHandler` -> 403 `FORBIDDEN`, both writing the factory's `ProblemDetail`; wired into
  `applicationSecurityFilterChain`; that chain permits the `ERROR` dispatcher type (everything else stays `denyAll`).
- Detail texts (English, generic, no internals, D-162): `VALIDATION_ERROR` "Request contains invalid fields";
  `MALFORMED_REQUEST` "Request could not be read"; `UNAUTHORIZED` "Authentication is required";
  `FORBIDDEN` "Access is denied"; `NOT_FOUND` "Resource not found"; `METHOD_NOT_ALLOWED` "Method is not supported
  for this resource"; `UNSUPPORTED_MEDIA_TYPE` "Content type is not supported"; `INTERNAL_ERROR` "An unexpected
  error occurred". `CONFLICT` / `RATE_LIMITED` / module codes: detail given by the thrower.
- docs/PROJECT.md §6 "Errors": list of common codes, `type` rule, links to D-150..D-162.
- Tests (D-154): own-context HTTP test (`RANDOM_PORT`) with a test controller and a test `SecurityFilterChain`
  permitting only its path; raw JSON strings; assertions on `errorCode` / `errors[].field` / headers, not only status.

## Out of scope
- The rate limiter itself, `WWW-Authenticate: Bearer` details for JWT (phase 1, `account`).
- Business error codes of modules (their slices); OpenAPI documentation of the shared `ProblemDetail` responses
  (SOL-137). No production endpoint is added, so the OpenAPI DoD (D-54) does not apply.
- Error responses of the management port (Actuator, port 8081) — unchanged.
- Localisation of messages.

## Acceptance criteria
- Every case below answers `Content-Type: application/problem+json` with `type`, `title`, `status`, `detail`,
  `instance`, `errorCode` as specified; `traceId` present (32 hex) in HTTP responses.
- Validation: body and parameter violations -> 400 `VALIDATION_ERROR` with the exact `errors[].field` values.
- Malformed JSON / wrong parameter type / missing parameter -> 400 `MALFORMED_REQUEST`, no `errors[]`.
- 404 / 405 (+ `Allow`) / 415 -> `NOT_FOUND` / `METHOD_NOT_ALLOWED` / `UNSUPPORTED_MEDIA_TYPE`.
- `ApiException` subclasses: 409 `CONFLICT`; a module-defined code from a test enum with an extra property;
  429 `RATE_LIMITED` with and without `Retry-After` / `retryAfterSeconds`.
- 500: body contains neither the exception message nor a stack trace; the log has one `ERROR` line with the stack
  trace and the same trace ID as the response.
- Security: denied request on the production chain -> 403 `FORBIDDEN`; unauthenticated request on an authenticated
  path -> 401 `UNAUTHORIZED`; both problem+json.
- `/error` dispatch: an exception thrown in a servlet filter -> 500 `INTERNAL_ERROR` problem+json.
- No current span -> `traceId` absent (unit level).
- `./gradlew build` green locally and in CI; `/security-review` done (security code in `shared`).

## Decisions
D-6, D-47, D-54, D-104, D-116, D-120, D-124, D-126, D-128, D-150..D-169

## Test cases
Acceptance level: HTTP against the main port of an own context (`ErrorHandlingIntegrationTests`, D-154) with the
test controller and test chain. Negative cases assert `errorCode` / `errors[].field` / headers. Rejection details
not fixed by the spec are pinned at RED and recorded in the journal.

- [x] TC-1 Body validation: RED — POST raw JSON with an invalid `username` and an invalid `items[0].type` -> not
      problem+json / no `errorCode`; GREEN — factory + advice; asserts 400 `VALIDATION_ERROR`, `type`
      `.../validation-error`, `instance`, `traceId`, `errors[].field` = {`username`, `items[0].type`}.
- [x] TC-2 Parameter validation: `limit=0` on a `@Min(1)` query parameter -> 400 `VALIDATION_ERROR`,
      `errors[].field` = {`limit`}.
- [x] TC-3 Malformed requests: broken JSON body, `limit=abc`, missing required parameter -> 400
      `MALFORMED_REQUEST`, no `errors[]`.
- [x] TC-4 Framework statuses: unknown path under the test prefix -> 404 `NOT_FOUND`; DELETE on a GET endpoint ->
      405 `METHOD_NOT_ALLOWED` + `Allow`; `text/plain` body on a JSON endpoint -> 415 `UNSUPPORTED_MEDIA_TYPE`.
- [x] TC-5 `ApiException`: 409 `CONFLICT`; a test-enum code (e.g. `SAMPLE_BUSINESS_RULE`) with an extra property;
      429 `RATE_LIMITED` with a duration (`Retry-After` + `retryAfterSeconds`) and without (neither present).
- [x] TC-6 Unexpected exception: 500 `INTERNAL_ERROR`, generic detail, the secret exception message absent from the
      body; captured log has the `ERROR` line with the stack trace and the response's trace ID.
- [x] TC-7 Security (amended by D-166): `/api/v1/anything` (production chain, `denyAll`) -> 403 `FORBIDDEN` problem+json; a test-chain
      path requiring authentication, no credentials -> 401 `UNAUTHORIZED` problem+json.
- [x] TC-8 `/error` dispatch: a test servlet filter throws for a marker path -> 500 `INTERNAL_ERROR` problem+json
      (RED: Boot's `BasicErrorController` JSON or 403 from `denyAll` on the error dispatch).
- [x] TC-9 No span: unit test of the factory with no current span -> no `traceId` property.
- [x] TC-10 Verify: `/simplify`, `./gradlew build` green (incl. Modulith verify with the `shared.error` named
      interface, jacoco), `/security-review`, diff vs spec and D-<n>; CI on the PR green after the user's push.

## Journal (append-only)
- Preconditions: `main` == `origin/main` at 5225853, clean tree (fresh `git fetch`/`status`); Linear SOL-141 Done,
  SOL-85 Backlog. Verified in the Boot 4.1.1 jar: `spring.mvc.problemdetails.enabled` default `false`,
  `ProblemDetailsErrorHandlingConfiguration` backs off when a `ResponseEntityExceptionHandler` bean exists.
- Decisions D-150..D-161 recorded (AskUserQuestion, three batches).
- Spec approved (gate 1) incl. `title` = reason phrase and the detail texts (D-162). Commit plan: one docs commit
  on `main` by the user, then the slice branch from the clean `main`.
- Branch slice/SOL-85-global-error-handling created from 09586ee (docs commit pushed; fresh `git fetch`).
- TC-1 RED: `ErrorHandlingIntegrationTests` -> 403 with an empty body (no advice: the validation exception goes to
  the `/error` dispatch, which the production chain denies). Unit RED: `ProblemDetailFactoryTests` does not compile
  (no `shared.error` package, no factory).
- TC-1 GREEN: `shared.error` (`ErrorCode`, `CommonErrorCode` with `VALIDATION_ERROR`, named interface `error`),
  `shared.internal.error.ProblemDetailFactory`, `GlobalExceptionHandler.handleMethodArgumentNotValid`. Package
  checkpoint green: 44 tests, 0 failed.
- pmd attempt 1/3 FAIL: pmdMain `ImplicitFunctionalInterface` on `ErrorCode` -> suppression approved (D-163);
  pmdTest `TestClassWithoutTestCases` on `TestErrorsConfiguration` (name starts with `Test`) -> renamed
  `FailingEndpointsConfiguration` (via `sed -i` — anti-pattern, L-30; `spotlessApply` right after). PMD green,
  slice tests green.
- Open for TC-2: Bean Validation messages follow the request/JVM locale; D-159 requires English — checked there.
- TC-2 RED: `limit=0` -> Spring's default problem (`detail` "Validation failure", no `errorCode` / `errors[]`). The
  test also pinned the `errors[].message` values: TC-1 turned red because Bean Validation messages were Russian (JVM
  locale `ru`, request locale without `Accept-Language`). D-164 approved: `spring.web.locale: en` +
  `spring.web.locale-resolver: fixed` in `application.yml` (property names checked in the Boot 4.1.1 metadata). TC-2
  sends `Accept-Language: ru` and maps `@RequestParam("limit")` to a Java parameter `pageSize`.
- TC-2 GREEN: `handleHandlerMethodValidationException` (field = `@RequestParam` / `@PathVariable` name, else the Java
  name; `FieldError`s of bean parameters keep their field). No separate unit test: the mapping needs Spring's
  exception objects and is covered by the acceptance test. Package checkpoint green: 45 tests, 0 failed; PMD green.
- TC-3 RED: all three -> Spring's default problem without `errorCode`; its details leak internals ("Failed to convert
  'limit' with value: 'abc'", "Required parameter 'limit' is not present.").
- TC-3 GREEN: `handleExceptionInternal` maps status 400 to `MALFORMED_REQUEST` with the D-162 detail; other statuses
  still go to the base class until TC-4. Package checkpoint green: 48 tests, 0 failed.
- pmdTest attempt 1/3 FAIL (TC-3): `AvoidDuplicateLiterals` ("/params" x4) -> constants `BODY` / `PARAMS`, done with
  Edit `replace_all`. PMD green, slice tests green.
- TC-4 check before coding: `ResponseEntityExceptionHandler` also yields 406, 413, 503 and any
  `ErrorResponseException` status, which D-151 does not cover -> asked; D-165 approved (code by status, other 4xx
  `MALFORMED_REQUEST`, 5xx `INTERNAL_ERROR`, no new codes).
- TC-4 RED: 404 / 405 / 415 -> Spring's default problems ("No static resource test/errors/missing.", "Method 'DELETE'
  is not supported.", "Content-Type 'text/plain;charset=UTF-8' is not supported."); `Allow: GET` was already present.
  Unit RED: `ProblemDetailFactoryTests.statusSelectsTheCommonCodeAndDetail` (12 statuses) does not compile.
- TC-4 GREEN: `ProblemDetailFactory.forStatus` + `defaultDetail` (D-162, D-165); `handleExceptionInternal` maps every
  remaining base-class exception through it (thrown detail kept only for 409 / 429); `CommonErrorCode` complete.
  Package checkpoint green: 63 tests, 0 failed; PMD green.
- TC-5: `ApiException` and `RateLimitedException` created first (API only, no advice mapping) so the acceptance test
  fails for the real reason. A stub without reading the wait failed Error Prone (`UnusedVariable`), so the first
  version of the wait rounded down (`Duration::toSeconds`).
- TC-5 RED: acceptance — all four -> 403 with an empty body (unhandled exception -> `/error` -> `denyAll`); unit
  `RateLimitedExceptionTests` — wait rounded down (1 ms -> 0 s, 1500 ms -> 1 s), no `retryAfterSeconds` property.
- TC-5 GREEN: `@ExceptionHandler(ApiException.class)` (status, code, detail, properties; `Retry-After` for
  `RateLimitedException`), wait rounded up to whole seconds. Package checkpoint green: 74 tests, 0 failed.
- pmd attempt 1/3 FAIL (TC-5): pmdMain `AvoidFieldNameMatchingMethodName` on `ApiException` -> accessors renamed to
  JavaBean getters (`getStatus`, `getErrorCode`, `getDetail`, `getProperties`; `RateLimitedException
  .getRetryAfterSeconds`), the usual style of exception accessors (`getMessage`); pmdTest `AvoidDuplicateLiterals`
  -> constant `RATE_LIMIT_DETAIL`. PMD green, slice tests green.
- TC-6 RED: `IllegalStateException` -> 403 with an empty body (`/error` -> `denyAll`); the stack trace was only in
  Tomcat's log, without our ERROR line.
- TC-6 GREEN: `@ExceptionHandler(Exception.class)` logs `ERROR "Unexpected error on {method} {path}"` with the stack
  trace and answers `forStatus(500)`; 5xx from the base class (D-165) log the same way (no test: no framework 5xx is
  reachable from the test controller). Log line carries `[traceId-spanId]` equal to the response `traceId`. Package
  checkpoint green: 75 tests, 0 failed; PMD green.
- Note for TC-7: the catch-all also catches Spring Security's `AccessDeniedException` / `AuthenticationException`
  thrown inside MVC (method security) and would turn them into 500 — TC-7 adds a test and passes them on.
- TC-7 check before coding: anonymous requests to a `denyAll` path go to the `AuthenticationEntryPoint`, not the
  `AccessDeniedHandler`; today's 403 comes from the default `Http403ForbiddenEntryPoint`. Asked; D-166 approved:
  anonymous -> 401 `UNAUTHORIZED`, 403 for authenticated callers (test chain with HTTP Basic + in-memory user). TC-7
  and the acceptance criterion "denied request on the production chain -> 403" are amended accordingly; the SOL-86
  main-port tests move to 401.
- TC-7 check before coding (spring-web 7.0.9): only `JacksonJsonHttpMessageConverter(JsonMapper.Builder)` registers
  `ProblemDetailJacksonMixin`; the security handlers write through such a converter built from `jsonMapper.rebuild()`.
- TC-7 RED: `/api/v1/anything` -> 403 empty (default `Http403ForbiddenEntryPoint`); player on `/test/errors/admin-only`
  -> 403 empty; `AccessDeniedException` in the controller -> 500 `INTERNAL_ERROR` (swallowed by the TC-6 catch-all);
  SOL-86 `ActuatorEndpointsIntegrationTests` deny tests (now expecting 401 problem) -> 403. Unit RED:
  `ProblemResponseWriterTests` does not compile.
- TC-7 GREEN: `ProblemResponseWriter` (public, `shared.internal.error`), `ProblemAuthenticationEntryPoint` /
  `ProblemAccessDeniedHandler` (`shared.internal.security`) wired into `applicationSecurityFilterChain` and the test
  chain; the advice passes `AccessDeniedException` / `AuthenticationException` on (no "Failure in @ExceptionHandler"
  warning in the log). Consequence of D-166 outside the spec wording: non-exposed paths on the management port
  (`/actuator/env`) are rejected by the application chain, so they also answer 401 problem+json (was 403 empty) —
  reported as a deviation. Package checkpoint green: 79 tests, 0 failed.
- pmdMain attempt 1/3 FAIL (TC-7): `CloseResource` on `ServletServerHttpResponse` -> try-with-resources. PMD green,
  affected tests green.
- TC-8 check before coding: D-161 (`ErrorController` in `shared.internal.error`) conflicts with the D-116 ArchUnit
  rule (controllers in `..internal.web..`). Asked; D-167 approved: `shared.internal.web.ProblemErrorController`.
- TC-8 RED: a test `FilterRegistrationBean` throwing for `/test/errors/filter-failure` -> 401 `UNAUTHORIZED` with
  `instance` `/error` (the ERROR dispatch is denied by the application chain since TC-7).
- TC-8 GREEN: `shared.internal.web.ProblemErrorController` (D-167) at `${spring.web.error.path:${error.path:/error}}`
  (the placeholder of Boot 4.1.1 `BasicErrorController`; `server.error.path` is deprecated): status from
  `jakarta.servlet.error.status_code`, `instance` = original request URI, 5xx logged at ERROR with the cause;
  `applicationSecurityFilterChain` permits `DispatcherType.ERROR`. `ProblemDetailFactory` / `forStatus` public.
  Added regression: a direct `GET /error` (REQUEST dispatch) stays 401. Package checkpoint green: 81 tests, 0 failed.
- pmdMain attempt 1/3 FAIL (TC-8): `GuardLogStatement` (method calls as log arguments) -> values in locals first. PMD
  green, slice tests green.
- TC-9 RED: `Tracer.NOOP.currentSpan()` returns a no-op span, not `null` -> `"traceId": ""` in the problem.
  GREEN: a span with an empty trace ID counts as no span. Package checkpoint green: 82 tests, 0 failed; PMD green.
- TC-10 (verify) started: docs/PROJECT.md §6 "Errors" updated (code table, `type` / `title` / `traceId` / `errors[]`
  rules, links to D-150..D-166; example `title` corrected to the reason phrase "Bad Request"). `/simplify` running:
  four read-only review agents (reuse, simplification, efficiency, altitude) on the slice diff. `git add -N .` was
  used to include new files in the diff (intent-to-add entries in the index only).
- /simplify (4 read-only review agents: reuse, simplification, efficiency, altitude). Applied: `ApiException
  .getHeaders()` hook (empty by default) — `RateLimitedException` sets `Retry-After` there and the handler copies
  headers generically, no `instanceof`; the wait is rounded once in the constructor; `ApiException` records no stack
  trace (expected client error, never logged); one `UnexpectedErrorLog.record` for all 5xx logging (advice, base-class
  5xx, `ErrorController`); `ProblemDetailFactory.forCode` replaces the nullable `defaultDetail` + `requireNonNull` at
  the call site, trace ID read once; `ProblemErrorController` takes the cause from Boot's `ErrorAttributes.getError`
  (also finds exceptions MVC stored internally); the entry point / access denied handler became two lambda beans in
  `SecurityConfiguration` (two classes deleted); `parameterName` relies on synthesized `@AliasFor` (`name()` ==
  `value`); tests: `validationProblem` / `expected(..., errors)` helpers, captured output read once, no fake
  one-row parameterized test. Skipped: own test context with its own containers (D-154/D-128 approved); switching
  `ApiException` to Spring's `ErrorResponseException` (D-152 approved design); caching `type` URIs (< 1 µs);
  relying on framework `title` / `instance` defaults (needed explicitly outside MVC); `@RequestHeader` / `@CookieValue`
  names (D-159 covers query/path only); sharing MVC's converter in `ProblemResponseWriter` (Boot 4.1.1 exposes no
  converter bean). `git rm --cached` + delete for the two handler classes (were intent-to-add only).
- pmdMain attempt 1/3 FAIL (simplify): `AvoidFieldNameMatchingMethodName` (`LOG` vs `log`) -> method `record`. PMD
  green; package tests green: 82 tests, 0 failed.
- `./gradlew build`: everything up to spotbugs green (spotless, checkstyle, pmd, tests, jacoco). spotbugs attempt 1/3
  FAIL: `SECSC` (ProblemErrorController is a Spring endpoint, informational, L), `SECSPRCSRFURM` (unrestricted
  `@RequestMapping` on the error path, H), `THROWS` (`passSecurityExceptionOn` throws `RuntimeException`, L),
  `SECCRLFLOG` (client-controlled method/path in the ERROR log line, L).
- spotbugs attempt 2/3 FAIL: `THROWS` fixed (one typed pass-on handler per security exception); `SECCRLFLOG` still
  reported after `replace('\r', '_').replace('\n', '_')` (find-sec-bugs 1.14.0 marks a value safe only with its
  `CR_ENCODED` + `LF_ENCODED` or `URL_ENCODED` taint tags; `replace(char, char)` sets neither).
- spotbugs attempt 3/3 FAIL: `replace("\r", "_").replace("\n", "_")` (String form) not recognised either. Limit
  reached -> escalated to the user: `SECSC`, `SECSPRCSRFURM`, `SECCRLFLOG` remain; each needs an exclusion (D-70) or
  a design change.
- Escalation resolved: D-168 approved (exclude filter `config/spotbugs/exclude.xml` + `excludeFilter` in
  `build.gradle`: `SPRING_ENDPOINT` project-wide, `SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING` for
  `ProblemErrorController`, `CRLF_INJECTION_LOGS` for `UnexpectedErrorLog`). spotbugsMain green. D-47 proof: a
  temporary `SpotbugsFilterProbe` logging the `User-Agent` header -> `SECCRLFLOG` + `SECSHUA` reported, build failed;
  probe deleted. Lesson L-31.
- `./gradlew build` green (spotlessCheck, checkstyle, pmd, spotbugs, all tests incl. Modulith/ArchUnit/Flyway,
  jacoco verification).
- Doc fix: stray CR/LF characters in the journal and L-31 (from Python escapes) made `LESSONS.md` non-text for Git
  (whole-file diff); replaced by literal escapes, line endings normalised. Lesson L-32.
- Diff vs spec and D-<n>: every code change traces to D-150..D-168. Deviations from the spec wording: (1) management
  port non-exposed paths answer 401 problem+json (consequence of D-166; spec said the management port is
  unchanged); (2) `ErrorController` in `shared.internal.web` (D-167, not `shared.internal.error`); (3) the entry point
  / access denied handler are lambda beans in `SecurityConfiguration`, not classes (/simplify); (4) `ApiException`
  exposes JavaBean getters and a `getHeaders()` hook (PMD, /simplify); (5) `config/spotbugs/exclude.xml` +
  `build.gradle` changed (D-168). No production endpoint added (OpenAPI DoD n/a). `/security-review` running
  (read-only sub-agent).
- /security-review (read-only sub-agent): one latent finding (confidence 9/10): binding failures (`@ModelAttribute`,
  wrong type) put Spring's conversion text (Java types, echoed input) into `errors[].message`. Checked safe: ERROR
  dispatch permit / direct `/error`, security exceptions pass-on (fails closed), fixed details, CRLF, `Retry-After`,
  `URI.create(instance)`, entry point / handler bodies, locale, SpotBugs exclusions. D-169 approved: fixed message
  "Invalid value" for binding failures. RED: `bindingFailureKeepsTheFieldButNotTheConversionText` showed
  "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; For input string: "abc"";
  GREEN after the `isBindingFailure()` check. `./gradlew build` green, 83 tests.

## Report
- Done: every main-port error is `application/problem+json` with `type` / `title` / `status` / `detail` / `instance` /
  `errorCode` / `traceId` (+ `errors[]`) from MVC, the security chains and the `/error` dispatch; `shared.error` API
  (`ErrorCode`, `CommonErrorCode`, `ApiException`, `RateLimitedException`) for modules; fixed English locale.
- Sensors: `./gradlew build` green (spotless, checkstyle, pmd, spotbugs with the D-168 filter, 83 tests incl.
  Modulith/ArchUnit/Flyway, jacoco). Fix attempts: pmd 1/3 at TC-1, TC-3, TC-5, TC-7, TC-8 and after /simplify (each
  fixed on the first retry); spotbugs 3/3 -> escalated, resolved by D-168.
- Decisions added during the build: D-163..D-169.
- Deviations from PROJECT.md / spec wording: see the diff-vs-spec journal line (management port 401, `ErrorController`
  package, lambda beans, getters + `getHeaders()`, SpotBugs filter). PROJECT.md §6 updated.

## Retro
- Went wrong: `sed -i` on a Java file (L-30); two SpotBugs attempts guessed instead of reading the detector (L-31);
  Python escapes wrote control characters into docs (L-32).
- Worked: pinning `errors[].message` in the acceptance test surfaced the JVM-locale bug early (TC-2); checking the
  Boot / Spring jars before coding (problem mixin, error path property, entry point vs handler) turned three would-be
  surprises into questions; the read-only security review found a real latent leak.
- Loop changes: none proposed beyond the lessons.
- Close: user confirmed the guard hook prompt appeared for the `sed -i` (L-30 updated). Commit e5d8ff3 merged as PR #7
  (6d54557 on `origin/main`, fresh `git fetch`). The close-out docs edit prepared for the slice branch was not in the
  merge and was gone from the working tree; re-applied on `main` as a docs commit.
- Linear replicated: SOL-85 already Done (GitHub integration on merge); description = approved spec with the
  amendments marked; report comment added. No new Backlog issues.
