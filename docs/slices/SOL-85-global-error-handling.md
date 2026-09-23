# SOL-85 — Global error handling: @RestControllerAdvice + ProblemDetail
Linear: https://linear.app/solarianofc/issue/SOL-85/global-error-handling-restcontrolleradvice-problemdetail
Status: build | Phase: 0
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
D-6, D-47, D-54, D-104, D-120, D-124, D-126, D-128, D-150..D-162

## Test cases
Acceptance level: HTTP against the main port of an own context (`ErrorHandlingIntegrationTests`, D-154) with the
test controller and test chain. Negative cases assert `errorCode` / `errors[].field` / headers. Rejection details
not fixed by the spec are pinned at RED and recorded in the journal.

- [ ] TC-1 Body validation: RED — POST raw JSON with an invalid `username` and an invalid `items[0].type` -> not
      problem+json / no `errorCode`; GREEN — factory + advice; asserts 400 `VALIDATION_ERROR`, `type`
      `.../validation-error`, `instance`, `traceId`, `errors[].field` = {`username`, `items[0].type`}.
- [ ] TC-2 Parameter validation: `limit=0` on a `@Min(1)` query parameter -> 400 `VALIDATION_ERROR`,
      `errors[].field` = {`limit`}.
- [ ] TC-3 Malformed requests: broken JSON body, `limit=abc`, missing required parameter -> 400
      `MALFORMED_REQUEST`, no `errors[]`.
- [ ] TC-4 Framework statuses: unknown path under the test prefix -> 404 `NOT_FOUND`; DELETE on a GET endpoint ->
      405 `METHOD_NOT_ALLOWED` + `Allow`; `text/plain` body on a JSON endpoint -> 415 `UNSUPPORTED_MEDIA_TYPE`.
- [ ] TC-5 `ApiException`: 409 `CONFLICT`; a test-enum code (e.g. `SAMPLE_BUSINESS_RULE`) with an extra property;
      429 `RATE_LIMITED` with a duration (`Retry-After` + `retryAfterSeconds`) and without (neither present).
- [ ] TC-6 Unexpected exception: 500 `INTERNAL_ERROR`, generic detail, the secret exception message absent from the
      body; captured log has the `ERROR` line with the stack trace and the response's trace ID.
- [ ] TC-7 Security: `/api/v1/anything` (production chain, `denyAll`) -> 403 `FORBIDDEN` problem+json; a test-chain
      path requiring authentication, no credentials -> 401 `UNAUTHORIZED` problem+json.
- [ ] TC-8 `/error` dispatch: a test servlet filter throws for a marker path -> 500 `INTERNAL_ERROR` problem+json
      (RED: Boot's `BasicErrorController` JSON or 403 from `denyAll` on the error dispatch).
- [ ] TC-9 No span: unit test of the factory with no current span -> no `traceId` property.
- [ ] TC-10 Verify: `/simplify`, `./gradlew build` green (incl. Modulith verify with the `shared.error` named
      interface, jacoco), `/security-review`, diff vs spec and D-<n>; CI on the PR green after the user's push.

## Journal (append-only)
- Preconditions: `main` == `origin/main` at 5225853, clean tree (fresh `git fetch`/`status`); Linear SOL-141 Done,
  SOL-85 Backlog. Verified in the Boot 4.1.1 jar: `spring.mvc.problemdetails.enabled` default `false`,
  `ProblemDetailsErrorHandlingConfiguration` backs off when a `ResponseEntityExceptionHandler` bean exists.
- Decisions D-150..D-161 recorded (AskUserQuestion, three batches).
- Spec approved (gate 1) incl. `title` = reason phrase and the detail texts (D-162). Commit plan: one docs commit
  on `main` by the user, then the slice branch from the clean `main`.
