---
stage: plan-node
changeId: CHG-0001
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0001
    agent: orchestrator
    agentRunId: run-plan-md-1
upstream:
  - path: docs/specs/CHG-0001/requirements.md
    sha256: 266b5b6d3d319d635b6fbe7e01d1750d017d704a0a24dada7243c8b6264241bb
  - path: docs/specs/CHG-0001/sdd.md
    sha256: 13262e8e00cf325ca29d7a40011bd6e88949bcd0c854029e8f79f3e9eee41d4b
---

# N09-build-java — java-engineer

Make the java tests pass in an isolated worktree.

## Files

- `api/pom.xml`
- `api/.gitignore`
- `api/src/main/resources/`
- `api/src/main/java/app/todo/`
- `api/src/test/java/app/todo/TestTodoApplication.java`
- `api/src/test/java/app/todo/testsupport/E2eClockConfiguration.java`
- `api/src/test/java/app/todo/testsupport/OffsetFileClock.java`
- `.harness/changes/CHG-0001/evidence/`

## Order

1. Plan file (this file), committed before any code.
2. `build(api)`: extend `api/pom.xml` with what production code needs (Spring Security, JDBC, Flyway, BouncyCastle,
   sqlite-jdbc at compile scope, `spring-boot-maven-plugin` so `spring-boot:run` and `spring-boot:test-run` resolve),
   `api/.gitignore` (target/, data/); pin the Spring Boot 4.x line (sdd 12.1 item 21).
3. `feat(api)` shared kernel `app.todo.common`: `Whitespace`, `ProblemCode`, `FieldError`, `ApiException` and subtypes,
   `ProblemWriter`, `ProblemDetailsAdvice`, `ProblemErrorController`, `RequestIdFilter`, `ClientHeaderFilter`,
   `ClockConfig`, body reading helper (WhitespaceTest, ProblemCodeTest, ErrorSourcesTest).
4. `feat(api)` composition root `app.todo.config` + `V1__init.sql` + `application.properties`: `DatabaseConfig`
   (one Hikari connection, pragmas, creates the parent directory), Flyway, `SecurityConfig` (stateless chain, entry
   point, firewall handler, filter order session -> authorization -> client header), request-id filter registration
   (ModuleWiringTest, PersistenceTest).
5. `feat(api)` identity module `app.todo.auth` + `auth.api.CurrentUser`: rules, Argon2id, session cookie filter,
   controller, repositories (SignUpApiTest, LoginSessionApiTest, PasswordStorageTest, AuthenticationRequiredApiTest,
   FilterExceptionTest).
6. `feat(api)` to-dos module `app.todo.todo`: validator, query, JDBC repository, service, controller (TodoCreate,
   TodoValidation, TodoUpdate, TodoList, TodoDeleteAndClear, TodoIsolation, ConcurrentEdits, AcceptanceScenario,
   HandlerException, EndpointInventory, Architecture tests).
7. `test(api)` test-source-set support for the e2e session-expiry run (ADR-0013): `TestTodoApplication`,
   `E2eClockConfiguration`, `OffsetFileClock` (new files; no existing test is edited).
8. Full backend suite, attach the green run's JUnit XML as evidence.

## Risks

- Spring Boot 4.x / Spring Security 7 / Jackson 3 APIs differ from 3.x; only verified by compiling and running.
- Filter order (session 401, then header 403, then 404/405/415/400) is built from Spring Security's filter ordering;
  the filters are not servlet-registered beans so they cannot run twice or out of order under MockMvc.
- One pooled connection: nothing may hold it while waiting for another (Argon2id runs outside transactions).
- SQLite: no JDBC read-only flag on transactions; unique-violation handled with `ON CONFLICT DO NOTHING`.
- FilterExceptionTest (F-12): the per-request log line is selected by the text "500"; the request id never contains
  "500" and no other `app.todo` entry for the request is logged.
- Spring Boot pin (F-10): latest 4.x found resolvable on Maven Central is kept; JDK 21 may not be installed.

## Proof

- `api/src/test/` whole suite (`cd api && ./mvnw test`): WhitespaceTest, ProblemCodeTest, ErrorSourcesTest,
  ModuleWiringTest, PersistenceTest, EndpointInventoryTest, SignUpApiTest, LoginSessionApiTest, PasswordStorageTest,
  AuthenticationRequiredApiTest, FilterExceptionTest, Todo*ApiTest, ConcurrentEditsApiTest, AcceptanceScenarioApiTest,
  HandlerExceptionTest, ArchitectureTest, ArchitectureNonVacuityTest, ContractFilesTest (REQ-AUTH-001..005,
  REQ-ISO-001, REQ-TODO-001..007, REQ-LIST-001..003, REQ-DATA-001..002, REQ-ACC-001, NFR-003 (indexes), NFR-004,
  NFR-005, NFR-007); JUnit XML attached as `test-report` evidence; critic-verdict by a fresh critic.

## Departures

- `api/src/test/java/app/todo/TestTodoApplication.java`, `api/src/test/java/app/todo/testsupport/E2eClockConfiguration.java`
  and `api/src/test/java/app/todo/testsupport/OffsetFileClock.java` are new test-source-set classes required by
  ADR-0013 and sdd.md 11 for `spring-boot:test-run`; no existing test file is changed.
- Result: 342 tests, 339 pass, 3 fail, 0 skipped, on JDK 23.0.2 (compiled with `--release 21`; no JDK 21 on the
  machine), Spring Boot 4.1.2. The three failures are not fixable by the implementation and no test was edited:
  `TodoListApiTest.REQ_LIST_002_AC5_empty_search_is_no_restriction` (`GET /api/v1/todos?q=`) and
  `TodoListApiTest.REQ_LIST_002_AC7_whitespace_only_search_is_no_search` (`q=%20`, `q=%20%20%20`; two parameterised
  cases) fail inside the test client's request-side contract check (`openapi-request-validator` v3 MockMvc adapter:
  "Parameter 'q' is required but is missing"), because `todo-api-v1.yaml` `listTodos` parameter `q` lacks
  `allowEmptyValue: true` although sdd.md 7 and REQ-LIST-002 AC-5/AC-7 require an empty or blank `q` to be accepted.
  The API answers those requests 200 with all items. Fix needs a contract change (re-freeze) or a test change.
- `.harness/changes/CHG-0001/evidence/` receives the attached test reports (written by `harness evidence attach`).
