---
type: component
title: To-dos module (api todo package)
summary: The Spring Boot to-dos module (package app.todo.todo) creates, lists, searches, pages, edits, completes and deletes a caller's to-dos, scoping every statement to the owner taken from the session.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/todo/TodoController.java
  - api/src/main/java/app/todo/todo/TodoService.java
  - api/src/main/java/app/todo/todo/TodoValidator.java
  - api/src/main/java/app/todo/todo/JdbcTodoRepository.java
  - api/src/main/java/app/todo/todo/TodoQuery.java
  - docs/specs/CHG-0001/sdd.md
---

# To-dos module (`app.todo.todo`)

**Responsibility.** To-do records for the signed-in user: validation, list semantics (filter, search, order, paging),
timestamps and isolation. Contract: [to-do API](../apis/todo-api.md). Table it owns: [todos](../data/todos.md).

**Classes** (from `api/src/main/java/app/todo/todo/`): `TodoController`, `TodoService`, `TodoValidator`, `TodoQuery`,
`TodoRepository` / `JdbcTodoRepository`, `Todo`, `TodoContent`, `NewTodo`, `TodoPage`, `TodoPageResult`, `TodoTotals`,
`Priority`.

**Isolation.** The owner comes only from `CurrentUser` (the identity module's published type). Every repository method
takes `userId` and filters on it in the same statement as `public_id`, so another user's id gives the same not-found as
a missing one (REQ-ISO-001, [ADR-0009](../decisions/adr-0009-spring-jdbc-without-jpa.md)). The module's SQL names only `todos`.

**Behaviour.**
- Create defaults priority to `MEDIUM` and sets `created_at` and `updated_at` to now; updates advance `updated_at`
  strictly; `created_at` never changes ([REQ-TODO-001](../requirements/req-todo-001.md), [REQ-TODO-004](../requirements/req-todo-004.md)).
- Update replaces all content fields (last write wins); the done toggle sets only `done` ([ADR-0006](../decisions/adr-0006-last-write-wins-whole-content-replace.md),
  [REQ-DATA-002](../requirements/req-data-002.md)).
- List: filter `all`, `active`, `completed`; search by a literal lower-cased substring of title or description;
  order `created_at DESC, id DESC`; page size 20; a page past the last is clamped to the last page
  ([REQ-LIST-001](../requirements/req-list-001.md), [REQ-LIST-002](../requirements/req-list-002.md), [REQ-LIST-003](../requirements/req-list-003.md)).
  Rules: [list-search-and-paging](../rules/list-search-and-paging.md).
- Clear completed deletes only the caller's completed to-dos and returns `deletedCount` (REQ-TODO-007, REQ-ISO-001 AC-3).
- Validation rules for title, description, due date and priority: [todo-field-rules](../rules/todo-field-rules.md).

**Interfaces.** `/api/v1/todos`, `/api/v1/todos/completed`, `/api/v1/todos/{todoId}`, `/api/v1/todos/{todoId}/done`.

**Tests.** [API integration tests](../tests/api-integration-tests.md), [contract tests](../tests/contract-tests.md),
[architecture tests](../tests/architecture-tests.md).

**Decisions.** [ADR-0006](../decisions/adr-0006-last-write-wins-whole-content-replace.md), [ADR-0009](../decisions/adr-0009-spring-jdbc-without-jpa.md),
[ADR-0013](../decisions/adr-0013-injected-clock-and-e2e-test-clock.md), [ADR-0015](../decisions/adr-0015-derived-lowercase-columns.md),
[ADR-0017](../decisions/adr-0017-timestamps-as-utc-text.md).
