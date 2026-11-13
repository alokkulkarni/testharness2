---
type: requirement
title: REQ-LIST-001 Filter by status
summary: The list filters to All, Active or Completed to-dos, defaults to All, and shows a no-matches state when the chosen filter matches nothing.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-LIST-001 Filter by status (M)

Spec: [requirements.md, REQ-LIST-001](../../specs/CHG-0001/requirements.md#req-list-001-filter-by-status-m).

**Acceptance criteria in brief.** AC-1 All lists both; AC-2 Active lists not-done; AC-3 Completed lists done; AC-4 no-matches
state, distinct from first use; AC-5 All is the default.

**Implemented by.** [to-dos module](../components/todos-module.md) (`TodoQuery` status, `listTodos` `status`), [to-do API](../apis/todo-api.md),
[web SPA](../components/web-spa.md) `FilterGroup`, screen [to-do list](../screens/todo-list.md).

**Rules.** [list, search and paging](../rules/list-search-and-paging.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoListApiTest`), [web component tests](../tests/web-component-tests.md) (`list.test.tsx`),
[e2e tests](../tests/e2e-tests.md) (`states.spec.ts`).
