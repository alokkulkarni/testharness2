# ADR-0004 Server-side sessions with an opaque HttpOnly cookie, 8 hours absolute

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-AUTH-002 AC-4, REQ-AUTH-004, REQ-ISO-001, NFR-004.

## Context

Q14 allows "a session or token login". Sessions last 8 hours (Q33, REQ-AUTH-002 AC-4); logout must make the session
unusable at once (REQ-AUTH-004 AC-3); expiry must be refused (AC-4). The requirements do not say whether the 8 hours
run from login or from last activity (critic finding 2). The app runs on `localhost` over plain HTTP (Q10). On
`localhost`, pages served from other ports are the same *site*, so `SameSite` alone does not stop another local page
from sending a cross-origin request with the cookie.

## Decision

- Login creates a row in `sessions` holding the **SHA-256 of a 256-bit random token** (from `SecureRandom`,
  base64url-encoded in the cookie), the user, `created_at` and `expires_at = created_at + 8 hours`. The database
  never holds a usable token.
- The 8 hours are **absolute from login**: activity does not extend them. An expired session is refused with 401
  `UNAUTHENTICATED` and its row deleted; expired rows are also deleted at each login.
- Cookie `todo_session`: `HttpOnly`, `SameSite=Strict`, `Path=/`, `Max-Age=28800`; `Secure` when
  `TODO_COOKIE_SECURE=true` (default `false`, because the local run is plain HTTP).
- Logout deletes the row and clears the cookie (always 204).
- Spring Security runs **stateless** (no `HttpSession`, no `JSESSIONID`); a `SessionCookieAuthenticationFilter`
  resolves the cookie to the user on each request.
- **Cross-site request defence**: every POST, PUT and DELETE must carry `X-Todo-Client: web`, which a browser sends
  cross-origin only after a CORS preflight; the API configures no CORS, so the preflight fails. Spring Security's
  token CSRF protection is disabled in favour of this header check. On protected operations the session is
  checked before the header, so an unauthenticated call is always reported as 401 (REQ-AUTH-004 AC-1).

## Consequences

- Logout and expiry take effect immediately, with no token revocation list.
- Sessions survive an API restart (they are in SQLite); a user who reloads after a restart stays signed in until
  the 8 hours end.
- A user active at hour 7:59 is asked to log in again at hour 8 (REQ-AUTH-004 AC-4); accepted as the plain
  reading of "session lifetime is 8 hours".
- Hosting the app anywhere but `localhost` requires `TODO_COOKIE_SECURE=true` and HTTPS (recorded as a risk).

## Alternatives

- JWT access tokens: cannot be revoked at logout without a server-side deny list, which removes their benefit.
- Sliding (idle) 8-hour expiry: not what "lifetime" suggests, and it lets a session live indefinitely.
- Spring Session JDBC: ships no SQLite schema; the custom table is smaller.
- Spring Security's synchroniser/double-submit CSRF token: works, but needs a token bootstrap call before sign-up
  and login; the custom header gives the same protection for a JSON-only API with no CORS.
