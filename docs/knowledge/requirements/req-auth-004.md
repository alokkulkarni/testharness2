---
type: requirement
title: REQ-AUTH-004 Unauthenticated access and logout
summary: Every to-do operation is refused without a valid session, logout ends the session, and an ended session sends the user to log in with a session-ended message.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-AUTH-004 Unauthenticated access and logout (M)

Spec: [requirements.md, REQ-AUTH-004](../../specs/CHG-0001/requirements.md#req-auth-004-unauthenticated-access-and-logout-m).

**Acceptance criteria in brief.** AC-1 to-do API calls without a session are refused (401) and change nothing; AC-2 a
signed-out visitor on a to-do page is sent to log in; AC-3 logout ends the session; AC-4 an expired session asks the user
to log in again.

**Implemented by.** [identity module](../components/identity-module.md) (`SessionCookieAuthenticationFilter`, `SecurityConfig` in the
[composition root](../components/composition-root.md)), [web SPA](../components/web-spa.md) (`RequireAuth`, `SessionProvider`),
screens [log in](../screens/log-in.md) and [to-do list](../screens/todo-list.md).

**Rules.** [session lifetime](../rules/session-lifetime.md), [return-to and not-found](../rules/return-to-and-not-found.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`AuthenticationRequiredApiTest`, `LoginSessionApiTest`,
`ModuleWiringTest`), [web component tests](../tests/web-component-tests.md) (`auth.test.tsx`), [e2e tests](../tests/e2e-tests.md)
(`auth.spec.ts`, `session-expiry.spec.ts`, which uses the test-only clock).

**Decisions.** [ADR-0004](../decisions/adr-0004-server-side-session-cookie.md), [ADR-0007](../decisions/adr-0007-client-header-cross-site-request-defence.md),
[ADR-0013](../decisions/adr-0013-injected-clock-and-e2e-test-clock.md).
