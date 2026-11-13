---
type: rule
title: Session lifetime
summary: A session lasts 8 hours from login, is never extended by activity, ends with no warning, and an expired session is refused and its row deleted.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/sdd.md
  - api/src/main/java/app/todo/auth/SessionService.java
---

# Session lifetime

- The session expires at login time plus 8 hours (`expires_at`, [sessions](../data/sessions.md)); the cookie has Max-Age 28800.
- Activity does not extend it; a user active at 7:59 is asked to log in again at 8:00
  ([sdd.md section 12.1 item 2](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)).
- Each request treats a missing, unknown or `expires_at <= now` session as unauthenticated; an expired row is deleted.
- The SPA reports a 401 while signed in as "Your session has ended. Please log in again." ([web SPA](../components/web-spa.md)).
- No warning precedes expiry, and unsaved input is lost; this is accepted under WCAG 2.2.1's essential exception (Q36, [NFR-001](../requirements/nfr-001.md)).

Sources: [REQ-AUTH-002](../requirements/req-auth-002.md) AC-4, [REQ-AUTH-004](../requirements/req-auth-004.md) AC-4, [ADR-0008](../decisions/adr-0008-absolute-session-lifetime.md).
Implemented in `api/src/main/java/app/todo/auth/SessionService.java`.
