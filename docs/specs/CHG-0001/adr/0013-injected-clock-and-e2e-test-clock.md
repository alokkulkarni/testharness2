# ADR-0013 Injected `Clock`, with a test-only offset clock for end-to-end tests

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-AUTH-002 AC-4, REQ-AUTH-004 AC-4, REQ-TODO-001 AC-1,
REQ-TODO-004 AC-1, REQ-TODO-005 AC-1, AC-2. Split out of ADR-0002 and extended for critic finding F-5 (N03-spec
round 1).

## Context

Session expiry (8 hours, ADR-0008) and timestamps that must strictly advance are time-dependent. Unit and
integration tests can inject a fixed clock, but the end-to-end test of REQ-AUTH-004 AC-4 runs the real API process
and cannot wait 8 hours. Any way to move a running API's clock must never exist in the normal local run.

## Decision

- Production: `ClockConfig` (package `common`) defines one `java.time.Clock` bean, `Clock.systemUTC()`. Every class
  that reads time takes the `Clock` by constructor injection; nothing calls `Instant.now()` or similar directly.
- Unit and integration tests pass a fixed or manually advanced `Clock`.
- End-to-end session-expiry test only:
  - `OffsetFileClock` (a `Clock` subclass) and `E2eClockConfiguration` live in the **test source set**
    (`api/src/test/java/app/todo/testsupport/`), so they are absent from `spring-boot:run` and from the packaged
    application. `E2eClockConfiguration` is also `@Profile("e2e")` and registers `OffsetFileClock` as the
    `@Primary` `Clock`.
  - `OffsetFileClock` returns the system UTC time plus an offset read on each call from the file named by property
    `todo.e2e.clock-offset-file` (environment `TODO_E2E_CLOCK_OFFSET_FILE`). The file holds an ISO-8601 duration
    such as `PT8H1M`; an absent or empty file means no offset; unreadable or invalid content throws, so the test
    fails visibly.
  - `api/src/test/java/app/todo/TestTodoApplication` starts the production application with
    `E2eClockConfiguration` added. It is started only by the session-expiry Playwright configuration, with
    `./mvnw -f api/pom.xml spring-boot:test-run` and `SPRING_PROFILES_ACTIVE=e2e`; at start it logs a warning that
    the test clock is active.
  - The test clock has no HTTP endpoint; it is controlled only by writing the local offset file.
  - Neither the profile nor the variable appears in `.env.example`, sdd.md §11 or the README run steps.

## Consequences

- Session expiry is tested end to end through the real SPA path (a signed-in user's request gets 401) without
  waiting and without any test hook in the shipped application.
- The session-expiry e2e test needs its own API process (started with `spring-boot:test-run`), separate from the
  main e2e run that uses the documented §11 commands.
- An integration test in the default profile asserts that the `Clock` bean is the system UTC clock and that no
  `OffsetFileClock` bean exists.

## Alternatives

- A settable clock behind an HTTP endpoint in an `e2e` profile of the production code: works, but ships test code
  and an endpoint that a mis-set profile would expose.
- Editing the session row's `expires_at` in the temporary database from the Playwright test: no clock code at all,
  but it needs a Node SQLite client dependency and writes to the database behind the API's single connection.
- Waiting 8 hours, or a configurable session lifetime: impractical, or a production setting that exists only for
  tests.
