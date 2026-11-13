---
type: test-suite
title: Web component tests (Vitest and React Testing Library)
summary: Component tests render the SPA's screens against a fake API and a stubbed session provider, covering the sign-up, log-in, list, form, navigation, error and accessibility behaviour.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/tests/component/auth.test.tsx
  - web/tests/component/list.test.tsx
  - web/tests/component/todo-form.test.tsx
  - web/tests/component/navigation.test.tsx
  - web/tests/component/errors.test.tsx
  - web/tests/component/a11y.test.tsx
  - web/tests/helpers/fakeApi.ts
  - web/tests/helpers/renderApp.tsx
---

# Web component tests

Level: component (Vitest, React Testing Library). Helpers: `web/tests/helpers/fakeApi.ts`, `renderApp.tsx`, `status.ts`, `lazy.ts`.

| File | Covers |
|---|---|
| `auth.test.tsx` | [sign-up](../screens/sign-up.md) and [log-in](../screens/log-in.md): [REQ-AUTH-001](../requirements/req-auth-001.md), [REQ-AUTH-002](../requirements/req-auth-002.md), [REQ-AUTH-004](../requirements/req-auth-004.md) (session-ended message, probe behaviour) |
| `list.test.tsx` | [to-do list](../screens/todo-list.md) and [confirm dialogs](../screens/confirm-dialogs.md): [REQ-TODO-003](../requirements/req-todo-003.md), [REQ-TODO-005](../requirements/req-todo-005.md) and [REQ-DATA-002](../requirements/req-data-002.md) (done-toggle re-query cases), [REQ-TODO-006](../requirements/req-todo-006.md), [REQ-TODO-007](../requirements/req-todo-007.md), [REQ-LIST-001](../requirements/req-list-001.md) to [REQ-LIST-003](../requirements/req-list-003.md) |
| `todo-form.test.tsx` | [to-do form](../screens/todo-form.md): [REQ-TODO-001](../requirements/req-todo-001.md), [REQ-TODO-002](../requirements/req-todo-002.md) |
| `navigation.test.tsx` | [not-found page](../screens/not-found.md) and [return-to rule](../rules/return-to-and-not-found.md): [REQ-NAV-001](../requirements/req-nav-001.md) |
| `errors.test.tsx` | [NFR-006](../requirements/nfr-006.md): messages, the error boundary, background failures |
| `a11y.test.tsx` | [NFR-001](../requirements/nfr-001.md): focus and `aria-describedby` on the screens |

Code under test: [web SPA](../components/web-spa.md). Related: [web unit tests](web-unit-tests.md), [e2e tests](e2e-tests.md).
