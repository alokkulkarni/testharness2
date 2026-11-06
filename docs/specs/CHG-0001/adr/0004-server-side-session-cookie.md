# ADR-0004 Server-side sessions with an opaque HttpOnly cookie

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-AUTH-002 AC-4, REQ-AUTH-004, REQ-ISO-001, NFR-004.
The session lifetime is decided in ADR-0008 and the cross-site request defence in ADR-0007 (split out in round 2).

## Context

Q14 allows "a session or token login". Logout must make the session unusable at once (REQ-AUTH-004 AC-3) and an
expired session must be refused (AC-4). The owner of every to-do operation must come from the authenticated
session (REQ-ISO-001). The app runs on `localhost` over plain HTTP (Q10).

## Decision

- Login creates a row in `sessions` holding the **SHA-256 of a 256-bit random token** (from `SecureRandom`,
  base64url-encoded in the cookie), the user, `created_at` and `expires_at` (ADR-0008). The database never holds a
  usable token.
- Cookie `todo_session`: `HttpOnly`, `SameSite=Strict`, `Path=/`, `Max-Age=28800`; `Secure` when
  `TODO_COOKIE_SECURE=true` (default `false`, because the local run is plain HTTP).
- Logout deletes the row and clears the cookie (always 204).
- Spring Security runs **stateless** (no `HttpSession`, no `JSESSIONID`); a `SessionCookieAuthenticationFilter`
  resolves the cookie to the user on each request. A missing, unknown or expired session leaves the request
  unauthenticated; on a protected path Spring Security then calls `ProblemAuthenticationEntryPoint`, which answers
  401 `UNAUTHENTICATED` through `ProblemWriter` (ADR-0011).

## Consequences

- Logout and expiry take effect immediately, with no token revocation list.
- Sessions survive an API restart (they are in SQLite); a user who reloads after a restart stays signed in until
  the session expires.
- The API cannot tell a visitor who never signed in from one whose cookie the browser has already dropped; both
  get 401. The SPA makes that distinction from its own state (sdd.md §4).
- Hosting the app anywhere but `localhost` requires `TODO_COOKIE_SECURE=true` and HTTPS (recorded as a risk).

## Alternatives

- JWT access tokens: cannot be revoked at logout without a server-side deny list, which removes their benefit.
- Spring Session JDBC: ships no SQLite schema; the custom table is smaller.
- Storing the raw token in the database: a copy of the database file would hand out live sessions.
