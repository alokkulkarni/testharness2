---
type: screen
title: To-do create and edit form (/todos/new, /todos/:id/edit)
summary: One form creates or edits a to-do with title, description, due date and a three-option priority, shows field errors beside their fields, and treats an unknown id as not found.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/todos/TodoFormPage.tsx
  - web/src/validation/rules.ts
  - docs/specs/CHG-0001/sdd.md
---

# To-do create and edit form

Component: `web/src/todos/TodoFormPage.tsx` in the [web SPA](../components/web-spa.md). Routes `/todos/new` and `/todos/:id/edit` sit behind `RequireAuth`.

- **Create**: a valid save goes to `/todos`, where the new to-do is first ([REQ-TODO-001](../requirements/req-todo-001.md)). Priority defaults to Medium.
- **Edit**: loads with `getTodo`, saves the whole content, and keeps the stored to-do when the save is refused ([REQ-TODO-004](../requirements/req-todo-004.md)).
- **Fields**: title, description (optional), due date (`<input type="date">`), priority (select of Low, Medium, High).
- **Errors**: `errors[].message` shown beside each field and in a summary that takes focus ([REQ-TODO-002](../requirements/req-todo-002.md) AC-10; [NFR-001](../requirements/nfr-001.md) AC-3).
- **Not found**: a 404, or an id that is not a UUID, shows the to-do not-found message with a "Back to my to-dos" link ([REQ-NAV-001](../requirements/req-nav-001.md) AC-4).

Rules: [todo field rules](../rules/todo-field-rules.md), [whitespace trimming](../rules/whitespace-trimming.md).

Tests: [web component tests](../tests/web-component-tests.md) (`todo-form.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`acceptance.spec.ts`, `keyboard.spec.ts`).
