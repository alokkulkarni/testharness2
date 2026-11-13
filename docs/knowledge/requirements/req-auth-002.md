---
type: requirement
title: REQ-AUTH-002 Log in
summary: A registered user logs in with identifier and password, gets an 8-hour session that survives a reload, and sees the same generic refusal for a wrong password and an unknown identifier.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-AUTH-002 Log in (M)

Spec: [requirements.md, REQ-AUTH-002](../../specs/CHG-0001/requirements.md#req-auth-002-login-m) (AC-1 to AC-5, and the Q36 note).

**Acceptance criteria in brief.** AC-1 correct credentials show the user's own list; AC-2 wrong password gives the generic
message and no session; AC-3 unknown identifier gives the same message (non-enumeration at login only); AC-4 reload keeps the
user signed in for 8 hours; AC-5 letter case of the identifier does not matter.

**Implemented by.** [identity module](../components/identity-module.md) (`AuthService`, `SessionService`), [identity API](../apis/identity-api.md)
`logIn` and `getSession`, [web SPA](../components/web-spa.md) (`SessionProvider`, `LoginPage`), screen [log in](../screens/log-in.md).

**Rules.** [session lifetime](../rules/session-lifetime.md), [identifier rules](../rules/identifier-rules.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`LoginSessionApiTest`), [web component tests](../tests/web-component-tests.md) (`auth.test.tsx`),
[e2e tests](../tests/e2e-tests.md) (`auth.spec.ts`, `session-expiry.spec.ts`).

**Decisions.** [ADR-0004](../decisions/adr-0004-server-side-session-cookie.md), [ADR-0008](../decisions/adr-0008-absolute-session-lifetime.md).
