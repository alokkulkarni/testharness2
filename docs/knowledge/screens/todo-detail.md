---
type: screen
title: To-do detail screen (/todos/:id)
summary: The detail page shows one to-do's title, status, due date, priority, description and created and updated times in the viewer's time zone, with links to edit and delete and a to-do not-found state.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/todos/TodoDetailPage.tsx
  - web/src/todos/todoId.ts
  - docs/specs/CHG-0001/sdd.md
---

# To-do detail screen (`/todos/:id`)

Component: `web/src/todos/TodoDetailPage.tsx` with `web/src/todos/todoId.ts` (`isTodoId`) in the [web SPA](../components/web-spa.md).

- Shows description, created timestamp and updated timestamp ([REQ-TODO-003](../requirements/req-todo-003.md) AC-3). Timestamps use the viewer's local time; due dates stay date-only.
- An id that is not a UUID, or an id the API does not find for this user, shows the to-do not-found state without a second request ([REQ-ISO-001](../requirements/req-iso-001.md) AC-2, [REQ-NAV-001](../requirements/req-nav-001.md) AC-4).
- A delete from this page keeps a "Back to my to-dos" link ([sdd.md section 6.1](../../specs/CHG-0001/sdd.md#61-spa-error-presentation-nfr-006)).

Tests: [web component tests](../tests/web-component-tests.md) (`navigation.test.tsx`), [accessibility tests](../tests/accessibility-tests.md) (the detail page is one of the eight audited screens).
