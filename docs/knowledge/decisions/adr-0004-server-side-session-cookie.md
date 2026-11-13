---
type: decision
title: ADR-0004 Server-side session in an HTTP-only cookie
summary: Login creates a server-side session row and an HttpOnly cookie, and the API answers 401 both to visitors who never signed in and to those whose session ended.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0004-server-side-session-cookie.md
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0004 Server-side session in an HTTP-only cookie

Frozen record: [adr/0004-server-side-session-cookie.md](../../specs/CHG-0001/adr/0004-server-side-session-cookie.md).

**Applied in.** [sessions table](../data/sessions.md) (hash of the token as key), `SessionCookieAuthenticationFilter` and
`SessionService` in the [identity module](../components/identity-module.md). The SPA tells "never signed in" from "session
ended" by its own state, not by the response ([sdd.md section 4.1](../../specs/CHG-0001/sdd.md#41-spa-session-state-req-auth-001-ac-1-req-auth-002-ac-4-req-auth-004-ac-2-ac-4)).
Serves [REQ-AUTH-002](../requirements/req-auth-002.md) and [REQ-AUTH-004](../requirements/req-auth-004.md).
