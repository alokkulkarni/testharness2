---
type: requirement
title: REQ-TODO-006 Delete a to-do
summary: Deleting asks for confirmation, removes the to-do permanently, returns not-found for it afterwards, and falls back to the last valid page when the last to-do on a page is deleted.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-TODO-006 Delete a to-do (M)

Spec: [requirements.md, REQ-TODO-006](../../specs/CHG-0001/requirements.md#req-todo-006-delete-a-to-do-m).

**Acceptance criteria in brief.** AC-1 confirmation before delete; AC-2 cancel changes nothing; AC-3 confirm deletes permanently;
AC-4 update or delete of a deleted to-do gives not-found; AC-5 the last valid page is shown after deleting the last item on a page.

**Implemented by.** [to-dos module](../components/todos-module.md) (`delete`), [to-do API](../apis/todo-api.md) `deleteTodo`,
[web SPA](../components/web-spa.md) `ConfirmDialog`, screens [confirm dialogs](../screens/confirm-dialogs.md) and [to-do list](../screens/todo-list.md).

**Rules.** [list, search and paging](../rules/list-search-and-paging.md) (page clamp).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoDeleteAndClearApiTest`), [web component tests](../tests/web-component-tests.md)
(`list.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`states.spec.ts`, `acceptance.spec.ts`).
