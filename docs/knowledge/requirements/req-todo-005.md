---
type: requirement
title: REQ-TODO-005 Mark done and not done
summary: A user toggles a to-do between Active and Completed, the change persists with an advanced updated time, and the Active view drops a newly completed to-do without a page reload.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-TODO-005 Mark done and not done (M)

Spec: [requirements.md, REQ-TODO-005](../../specs/CHG-0001/requirements.md#req-todo-005-mark-done-and-not-done-m).

**Acceptance criteria in brief.** AC-1 Active to Completed; AC-2 Completed to Active; AC-3 the list re-queries at once under the Active filter.

**Implemented by.** [to-dos module](../components/todos-module.md) (`updateDone`), [to-do API](../apis/todo-api.md) `setTodoDone`,
[web SPA](../components/web-spa.md) `TodoRow` and `TodoListPage`, screen [to-do list](../screens/todo-list.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoUpdateApiTest`), [e2e tests](../tests/e2e-tests.md) (`acceptance.spec.ts`).
