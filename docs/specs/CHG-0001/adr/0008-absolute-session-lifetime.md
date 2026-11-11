# ADR-0008 Session lifetime of 8 hours, absolute from login

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-AUTH-002 AC-4, REQ-AUTH-004 AC-4. Split out of ADR-0004
(critic finding F-6, N03-spec round 1).

## Context

The session lifetime is 8 hours (Q33, REQ-AUTH-002 AC-4); an expired session is refused and the UI asks the user
to log in again (REQ-AUTH-004 AC-4). The requirements do not say whether the 8 hours run from login or from the
last activity (requirements critic finding 2).

## Decision

- At login the session row gets `expires_at = created_at + 8 hours`, read from the injected `Clock` (ADR-0013).
- Activity never extends it: no request updates `expires_at`.
- The cookie's `Max-Age` is 28800 seconds, matching the server-side expiry.
- A request whose session has `expires_at <= now` is refused with 401 `UNAUTHENTICATED` and the row is deleted;
  expired rows of all users are also deleted at each login.

## Consequences

- A user active at hour 7:59 is asked to log in again at hour 8; this is the plain reading of "session lifetime is
  8 hours" and is listed for the approver to confirm at freeze (sdd.md §12.1).
- Expiry is a single comparison, testable with a fixed or offset clock.
- Sessions survive an API restart (they are in SQLite) until the 8 hours end.

## Alternatives

- Sliding (idle) 8-hour expiry: not what "lifetime" suggests, and it lets a session live indefinitely.
- Sliding expiry with an absolute cap: two limits to explain and test for no requirement that asks for it.
