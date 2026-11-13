---
type: api
title: To-do API (v1)
summary: The to-dos module's HTTP operations under /api/v1/todos, covering list, create, get, update, done toggle, delete and clear-completed, defined in todo-api-v1.yaml.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/contracts/todo-api-v1.yaml
  - api/src/main/java/app/todo/todo/TodoController.java
  - api/src/main/java/app/todo/todo/TodoService.java
---

# To-do API (v1)

**Frozen contract.** [todo-api-v1.yaml](../../specs/CHG-0001/contracts/todo-api-v1.yaml) (OpenAPI 3.1; authoritative).

**Implementation.** `TodoController` in the [to-dos module](../components/todos-module.md)
(`api/src/main/java/app/todo/todo/TodoController.java`). Every operation requires a session (401 `UNAUTHENTICATED` otherwise).

| Operation | Method and path | Requirements | Notes |
|---|---|---|---|
| `listTodos` | GET `/api/v1/todos` | [REQ-LIST-001](../requirements/req-list-001.md), [REQ-LIST-002](../requirements/req-list-002.md), [REQ-LIST-003](../requirements/req-list-003.md) | query `status`, `q`, `page`; fixed page size 20; returns `totals` ignoring filter, search and page |
| `createTodo` | POST `/api/v1/todos` | [REQ-TODO-001](../requirements/req-todo-001.md) | priority defaults to MEDIUM |
| `getTodo` | GET `/api/v1/todos/{todoId}` | [REQ-TODO-003](../requirements/req-todo-003.md), [REQ-ISO-001](../requirements/req-iso-001.md) | 404 `NOT_FOUND` for missing, malformed or another user's id |
| `updateTodo` | PUT `/api/v1/todos/{todoId}` | [REQ-TODO-004](../requirements/req-todo-004.md), [REQ-DATA-002](../requirements/req-data-002.md) | replaces title, description, due date and priority |
| `setTodoDone` | PUT `/api/v1/todos/{todoId}/done` | [REQ-TODO-005](../requirements/req-todo-005.md) | sets `done` only |
| `deleteTodo` | DELETE `/api/v1/todos/{todoId}` | [REQ-TODO-006](../requirements/req-todo-006.md) | permanent delete |
| `clearCompletedTodos` | DELETE `/api/v1/todos/completed` | [REQ-TODO-007](../requirements/req-todo-007.md) | returns `deletedCount`, 200 with 0 when none |

**Isolation.** The owner is never read from the request; bodies have no owner field and unknown fields are refused
(`UNKNOWN_FIELD`). See [REQ-ISO-001](../requirements/req-iso-001.md).

**Validation.** [todo-field-rules](../rules/todo-field-rules.md), [list-search-and-paging](../rules/list-search-and-paging.md).

**Tests.** [contract tests](../tests/contract-tests.md), [API integration tests](../tests/api-integration-tests.md).
