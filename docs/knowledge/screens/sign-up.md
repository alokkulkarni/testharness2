---
type: screen
title: Sign-up screen
summary: The /signup page has one identifier field that takes an email address or a username, a password field, and field-level messages; success returns the user to log in with an account-created message.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/auth/SignupPage.tsx
  - docs/specs/CHG-0001/sdd.md
  - docs/specs/CHG-0001/design/ux/ux-design.md
---

# Sign-up screen (`/signup`)

Component: `web/src/auth/SignupPage.tsx` in the [web SPA](../components/web-spa.md). Public route; a signed-in visitor is sent to `/todos`.

- One identifier field (email or username) and one password field ([REQ-AUTH-001](../requirements/req-auth-001.md) AC-7).
- Server messages appear beside their field; a taken identifier keeps the password typed ([NFR-006](../requirements/nfr-006.md)).
- On success it shows "Account created — log in" on the login page and does not sign in ([sdd.md section 4](../../specs/CHG-0001/sdd.md#4-auth-and-session-adr-0004-adr-0005-adr-0007-adr-0008)).
- Rules: [identifier rules](../rules/identifier-rules.md), [password rules](../rules/password-rules.md).
- Visual and copy detail: the UX design, [ux-design.md](../../specs/CHG-0001/design/ux/ux-design.md).

Tests: [web component tests](../tests/web-component-tests.md) (`auth.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`auth.spec.ts`),
[accessibility tests](../tests/accessibility-tests.md).
