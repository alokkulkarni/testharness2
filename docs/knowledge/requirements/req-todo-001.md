---
type: requirement
title: REQ-TODO-001 Create a to-do
summary: A signed-in user creates a not-done to-do with a title, a due date, an optional priority (default Medium) and an optional description, which appears first in their list.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-TODO-001 Create a to-do (M)

Spec: [requirements.md, REQ-TODO-001](../../specs/CHG-0001/requirements.md#req-todo-001-create-a-to-do-m).

**Acceptance criteria in brief.** AC-1 valid create gives a not-done to-do at the top of the list; AC-2 omitted priority
stores Medium; AC-3 description optional; AC-4 still present after reload.

**Implemented by.** [to-dos module](../components/todos-module.md) (`TodoService`, `TodoValidator`), [to-do API](../apis/todo-api.md) `createTodo`,
[web SPA](../components/web-spa.md) form, screen [to-do form](../screens/todo-form.md).

**Rules.** [todo field rules](../rules/todo-field-rules.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoCreateApiTest`), [web component tests](../tests/web-component-tests.md)
(`todo-form.test.tsx`), [e2e tests](../tests/e2e-tests.md).
