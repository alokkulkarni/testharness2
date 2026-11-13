---
type: test-suite
title: API integration tests (JUnit and MockMvc)
summary: Spring Boot integration tests in api/src/test cover every API operation, error source, persistence, session behaviour and the password and filter-exception paths against a fresh SQLite file.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/test/java/app/todo/auth/SignUpApiTest.java
  - api/src/test/java/app/todo/todo/TodoListApiTest.java
  - api/src/test/java/app/todo/config/PersistenceTest.java
  - api/src/test/java/app/todo/testsupport/IntegrationTestBase.java
  - .harness/changes/CHG-0001/traceability.json
---

# API integration tests

Level: integration (SDD section 14), Spring Boot with MockMvc against a fresh temporary SQLite file per test class. The filter-exception
test runs on the embedded container at a random port, because MockMvc performs no ERROR dispatch.

**Suites** (`api/src/test/java/app/todo/`):

| Suite | Covers |
|---|---|
| `auth/SignUpApiTest` | [REQ-AUTH-001](../requirements/req-auth-001.md): create, duplicates, field refusals, password and identifier boundaries |
| `auth/LoginSessionApiTest` | [REQ-AUTH-002](../requirements/req-auth-002.md), [REQ-AUTH-004](../requirements/req-auth-004.md) AC-3 and AC-4: login, identical 401 bodies, cookie attributes, 8-hour expiry, logout |
| `auth/AuthenticationRequiredApiTest` | [REQ-AUTH-004](../requirements/req-auth-004.md) AC-1: 401 for no, expired or logged-out sessions, and check order |
| `auth/PasswordStorageTest` | [REQ-AUTH-003](../requirements/req-auth-003.md), [REQ-AUTH-005](../requirements/req-auth-005.md): hashes, salts, no secrets in responses |
| `auth/FilterExceptionTest` | [NFR-004](../requirements/nfr-004.md): a forced exception in a filter gives a generic 500 with one log line and no sentinel values |
| `todo/TodoCreateApiTest`, `todo/TodoValidationApiTest` | [REQ-TODO-001](../requirements/req-todo-001.md), [REQ-TODO-002](../requirements/req-todo-002.md) |
| `todo/TodoUpdateApiTest`, `todo/TodoDeleteAndClearApiTest` | [REQ-TODO-004](../requirements/req-todo-004.md) to [REQ-TODO-007](../requirements/req-todo-007.md) |
| `todo/TodoListApiTest` | [REQ-LIST-001](../requirements/req-list-001.md) to [REQ-LIST-003](../requirements/req-list-003.md) |
| `todo/TodoIsolationApiTest` | [REQ-ISO-001](../requirements/req-iso-001.md), [REQ-AUTH-005](../requirements/req-auth-005.md) |
| `todo/ConcurrentEditsApiTest` | [REQ-DATA-002](../requirements/req-data-002.md) |
| `todo/AcceptanceScenarioApiTest` | [REQ-ACC-001](../requirements/req-acc-001.md) (API part) |
| `todo/HandlerExceptionTest` | [NFR-004](../requirements/nfr-004.md) AC-2 |
| `config/PersistenceTest` | [REQ-DATA-001](../requirements/req-data-001.md) |
| `config/ModuleWiringTest` | [NFR-007](../requirements/nfr-007.md) AC-1; [REQ-AUTH-004](../requirements/req-auth-004.md) AC-4 (system clock in the default profile) |
| `common/ErrorSourcesTest`, `common/ProblemCodeTest`, `common/JsonSuffixMediaTypeTest`, `common/WhitespaceTest` | [common problem model](../apis/common-problem-model.md), [whitespace trimming](../rules/whitespace-trimming.md) |

**Test helpers** (`testsupport/`): `Db`, `ApiClient`, `LogCapture`, `OffsetFileClock`, `E2eClockConfiguration`, `Contracts`.
The `Contracts` helper holds a whitelist for the `listTodos` `q` parameter, recorded as a finding ([overview, known divergences](../system/overview.md#known-divergences-as-recorded-at-n12-and-n13)).

Related: [contract tests](contract-tests.md), [architecture tests](architecture-tests.md). Components tested: [identity module](../components/identity-module.md),
[to-dos module](../components/todos-module.md), [shared kernel](../components/shared-kernel.md).
