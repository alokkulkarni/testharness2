---
type: requirement
title: REQ-TODO-003 View and order the list
summary: The list shows each user's to-dos newest first with title, done status, due date and priority, a first-use empty state, and each to-do's detail with description and timestamps.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-TODO-003 View and order the list (M)

Spec: [requirements.md, REQ-TODO-003](../../specs/CHG-0001/requirements.md#req-todo-003-view-and-order-the-list-m).

**Acceptance criteria in brief.** AC-1 newest first with the four fields shown; AC-2 first-use empty state with an
"Add a to-do" link; AC-3 detail shows description and created and updated timestamps.

**Implemented by.** [to-dos module](../components/todos-module.md) (`JdbcTodoRepository` order `created_at DESC, id DESC`),
[web SPA](../components/web-spa.md), screens [to-do list](../screens/todo-list.md) and [to-do detail](../screens/todo-detail.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoListApiTest`), [web component tests](../tests/web-component-tests.md)
(`list.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`states.spec.ts`).
