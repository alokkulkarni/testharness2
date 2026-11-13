---
type: screen
title: Confirm dialogs (delete and clear completed)
summary: Two native modal dialogs ask for confirmation before a to-do is deleted or all Completed to-dos are cleared, with Cancel as the default focus and the clear dialog naming the number of to-dos.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/todos/ConfirmDialog.tsx
  - web/src/todos/TodoListPage.tsx
  - docs/specs/CHG-0001/sdd.md
---

# Confirm dialogs

Component: `web/src/todos/ConfirmDialog.tsx` in the [web SPA](../components/web-spa.md), built on native `<dialog>` with `showModal()`.

**Delete a to-do** ([REQ-TODO-006](../requirements/req-todo-006.md)): names the to-do. Cancel changes nothing; confirm deletes permanently.

**Clear all completed** ([REQ-TODO-007](../requirements/req-todo-007.md)): the SPA re-fetches the list before opening, then uses the count `N`:
- For N of 2 or more: "Delete all N completed to-dos? This deletes all your completed to-dos, including any not shown because of the current page, filter or search. This cannot be undone."
- For N = 1 (analyst default text, [sdd.md section 12.1 item 5](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)): "Delete 1 completed to-do? This deletes your only completed to-do, even if it is not shown because of the current page, filter or search. This cannot be undone."
- For N = 0 no dialog opens, the control is disabled, and a live region says there are no completed to-dos to clear.

**Focus** ([sdd.md section 9](../../specs/CHG-0001/sdd.md#9-accessibility-nfr-001)): initial focus on Cancel; Escape cancels; focus returns to the invoking control, or to the list heading if the row was deleted ([NFR-001](../requirements/nfr-001.md) AC-2).

Used from [to-do list](todo-list.md) and [to-do detail](todo-detail.md).

Tests: [web component tests](../tests/web-component-tests.md) (`list.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`keyboard.spec.ts`), [accessibility tests](../tests/accessibility-tests.md).
