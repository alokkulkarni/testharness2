---
type: screen
title: To-do list screen (/todos)
summary: The /todos page shows the user's to-dos with a filter, a search box, a clear-completed control, row actions and pagination, with separate first-use, no-match and error states.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/todos/TodoListPage.tsx
  - web/src/todos/FilterGroup.tsx
  - web/src/todos/SearchForm.tsx
  - web/src/todos/Pagination.tsx
  - web/src/todos/TodoRow.tsx
  - docs/specs/CHG-0001/sdd.md
---

# To-do list screen (`/todos?status=&q=&page=`)

Component: `web/src/todos/TodoListPage.tsx` with `FilterGroup`, `SearchForm`, `Pagination` and `TodoRow`, in the [web SPA](../components/web-spa.md).

- **Rows**: title, done checkbox, due date and priority as text ([REQ-TODO-003](../requirements/req-todo-003.md) AC-1).
- **Filter** (radio group): All, Active, Completed. Changing it, or searching, resets to page 1 ([REQ-LIST-001](../requirements/req-list-001.md), [REQ-LIST-003](../requirements/req-list-003.md) AC-5).
- **Search**: submitted with Enter or the button; whitespace-only is no search ([REQ-LIST-002](../requirements/req-list-002.md)).
- **Pagination**: Previous, numbered links, Next; hidden with one page ([REQ-LIST-003](../requirements/req-list-003.md)).
- **States**: first use (no to-dos at all) shows the "Add a to-do" link; any other empty result shows "No matching to-dos"
  ([sdd.md section 8](../../specs/CHG-0001/sdd.md#8-spa-behaviour)).
- **Done toggle**: the list re-queries at once, so an Active view drops a newly completed row ([REQ-TODO-005](../requirements/req-todo-005.md) AC-3).
- **Clear completed**: disabled when no to-do is Completed; otherwise opens the [confirm dialog](confirm-dialogs.md).
- **Not-found on a row** (404 on toggle or delete): the list re-queries and shows "That to-do no longer exists, and the list has been refreshed." (Q48; [sdd.md section 6.1](../../specs/CHG-0001/sdd.md#61-spa-error-presentation-nfr-006)).

Requirements: [REQ-TODO-003](../requirements/req-todo-003.md), [REQ-TODO-005](../requirements/req-todo-005.md), [REQ-TODO-006](../requirements/req-todo-006.md),
[REQ-TODO-007](../requirements/req-todo-007.md), [REQ-LIST-001](../requirements/req-list-001.md) to [REQ-LIST-003](../requirements/req-list-003.md).

Tests: [web component tests](../tests/web-component-tests.md) (`list.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`states.spec.ts`, `acceptance.spec.ts`, `keyboard.spec.ts`).
