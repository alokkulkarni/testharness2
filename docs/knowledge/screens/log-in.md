---
type: screen
title: Log-in screen
summary: The /login page takes an identifier and password, shows one generic message for a refused login, returns the user to a safe to-do route, and shows the session-ended message only after a session ends while signed in.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/auth/LoginPage.tsx
  - web/src/auth/returnTo.ts
  - docs/specs/CHG-0001/sdd.md
---

# Log-in screen (`/login`)

Component: `web/src/auth/LoginPage.tsx` in the [web SPA](../components/web-spa.md).

- A refused login shows "Invalid identifier or password. Check them and try again." for both a wrong password and an unknown identifier
  ([REQ-AUTH-002](../requirements/req-auth-002.md) AC-2, AC-3; [NFR-006](../requirements/nfr-006.md) AC-4).
- After success the SPA goes to `safeReturnTo(returnTo)` ([return-to rule](../rules/return-to-and-not-found.md)).
- The "Your session has ended" message appears only when the session ended while signed in; it is passed in router state, so a typed `/login` never shows it ([REQ-AUTH-004](../requirements/req-auth-004.md) AC-4).
- The "Account created — log in" message comes from sign-up ([sign-up](sign-up.md)).

Tests: [web component tests](../tests/web-component-tests.md) (`auth.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`auth.spec.ts`, `session-expiry.spec.ts`).
