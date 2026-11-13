---
type: component
title: Web SPA (React and TypeScript)
summary: The web/ single-page app, built with React, TypeScript and Vite, renders the sign-up, login, to-do list, to-do form, to-do detail and not-found screens and mirrors the API's validation.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/app/AppRoutes.tsx
  - web/src/app/AppErrorBoundary.tsx
  - web/src/auth/SessionProvider.tsx
  - web/src/auth/RequireAuth.tsx
  - web/src/auth/returnTo.ts
  - web/src/api/client.ts
  - web/src/errors/userMessage.ts
  - web/src/validation/rules.ts
  - web/src/todos/TodoListPage.tsx
  - docs/specs/CHG-0001/sdd.md
---

# Web SPA (`web/`)

**Responsibility.** Screens, routing, client-side validation (a mirror only; the API is authoritative), error
presentation and accessibility. Stack: React, TypeScript, Vite ([ADR-0001](../decisions/adr-0001-react-typescript-spa.md)).

**Modules** (SDD section 2 and the file list):
- `src/api/`: `client.ts` (fetch wrapper, `X-Todo-Client: web` on unsafe methods, 401 reporting), `identity.ts` and
  `todos.ts` (types generated from the two module contracts; the SPA imports `Problem` from `todos.ts` only).
- `src/auth/`: `SessionProvider.tsx` (signed-in, signed-out, unknown), `RequireAuth.tsx`, `LoginPage.tsx`,
  `SignupPage.tsx`, `returnTo.ts` (`safeReturnTo`). See [log in](../screens/log-in.md) and [sign up](../screens/sign-up.md).
- `src/todos/`: `TodoListPage.tsx`, `FilterGroup.tsx`, `SearchForm.tsx`, `Pagination.tsx`, `TodoRow.tsx`,
  `TodoFormPage.tsx`, `TodoDetailPage.tsx`, `ConfirmDialog.tsx`, `todoId.ts`, `format.ts`. See [to-do list](../screens/todo-list.md),
  [to-do form](../screens/todo-form.md), [to-do detail](../screens/todo-detail.md), [confirm dialogs](../screens/confirm-dialogs.md).
- `src/app/`: `AppRoutes.tsx` (route table), `AppShell.tsx`, `NotFoundPage.tsx`, `AppErrorBoundary.tsx`, `usePageFocus.ts`.
  See [not-found page](../screens/not-found.md).
- `src/errors/userMessage.ts`: every user-facing failure goes through `userMessageFor` ([NFR-006](../requirements/nfr-006.md)).
- `src/validation/`: `whitespace.ts`, `rules.ts` ([whitespace-trimming](../rules/whitespace-trimming.md), [todo-field-rules](../rules/todo-field-rules.md)).

**Behaviour that matters for the bundle.**
- Routes: `/` goes to `/todos`; `/signup`, `/login` are public; `/todos`, `/todos/new`, `/todos/:id`,
  `/todos/:id/edit` and the catch-all sit behind `RequireAuth` (REQ-NAV-001).
- The session probe calls `getSession` once; 401 means signed-out and is silent (REQ-AUTH-002 AC-4, REQ-AUTH-004 AC-2).
- A 401 while signed in ends the session with the message "Your session has ended. Please log in again." (REQ-AUTH-004 AC-4).
- `returnTo` is honoured only for the app's own to-do routes ([returnTo rule](../rules/return-to-and-not-found.md)).
- `AppErrorBoundary` shows a full-page message for errors thrown during render; it resets when the pathname changes.
- Accessibility rules (focus, live regions, `aria-describedby`) are in SDD section 9 ([NFR-001](../requirements/nfr-001.md)).

**Interfaces.** Calls the [identity API](../apis/identity-api.md) and the [to-do API](../apis/todo-api.md) over HTTP.

**Tests.** [web unit tests](../tests/web-unit-tests.md), [web component tests](../tests/web-component-tests.md),
[end-to-end tests](../tests/e2e-tests.md), [accessibility tests](../tests/accessibility-tests.md).
