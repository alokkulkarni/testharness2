---
type: requirement
title: REQ-TODO-007 Clear all completed
summary: A user deletes all of their Completed to-dos, across pages and filters, after a confirmation that states the count, with the control disabled when there are none.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-TODO-007 Clear all completed (M)

Spec: [requirements.md, REQ-TODO-007](../../specs/CHG-0001/requirements.md#req-todo-007-clear-all-completed-m).

**Acceptance criteria in brief.** AC-1 confirmation states the count, the scope (not only shown items) and permanence, with a
singular sentence for one item; AC-2 all Completed to-dos are deleted and Active ones kept; AC-3 cancel deletes nothing;
AC-4 control disabled when none are Completed.

**Implemented by.** [to-dos module](../components/todos-module.md) (`deleteCompleted`), [to-do API](../apis/todo-api.md) `clearCompletedTodos`
(DELETE `/api/v1/todos/completed`), [web SPA](../components/web-spa.md) `TodoListPage`, screen [confirm dialogs](../screens/confirm-dialogs.md).

**Rules.** The exact sentences are in [confirm dialogs](../screens/confirm-dialogs.md) and `sdd.md` section 8, with the singular text an
analyst default ([sdd.md section 12.1 item 5](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoDeleteAndClearApiTest`), [web component tests](../tests/web-component-tests.md)
(`list.test.tsx`), [e2e tests](../tests/e2e-tests.md).
