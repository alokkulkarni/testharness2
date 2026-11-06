# ADR-0007 Cross-site request defence with a required `X-Todo-Client` header

Status: proposed (CHG-0001, N03-spec). Requirements: NFR-004, REQ-AUTH-004 AC-1, REQ-ISO-001. Split out of
ADR-0004 (critic finding F-6, N03-spec round 1).

## Context

Authentication is a cookie (ADR-0004), so a browser attaches it to any request to the API, including one started
by another page. The app runs on `localhost` over plain HTTP (Q10). On `localhost`, pages served from other ports
are the same *site*, so `SameSite=Strict` alone does not stop another local page from sending a cross-origin
request with the cookie. The API is JSON-only and is called only by the SPA, which shares its origin through the
Vite proxy (ADR-0001).

## Decision

- Every `POST`, `PUT` and `DELETE` request must carry the header `X-Todo-Client` with the exact value `web`. A
  browser sends such a custom header cross-origin only after a CORS preflight; the API configures **no CORS**
  (it never sends `Access-Control-Allow-*` headers), so the preflight fails and the request is never sent.
- `ClientHeaderFilter` checks the header. On operations that need a session it runs after
  `SessionCookieAuthenticationFilter`, so an unauthenticated call is always reported as 401 (REQ-AUTH-004 AC-1);
  a missing or different header is then 403 `CLIENT_HEADER_MISSING`, written through `ProblemWriter` (ADR-0011).
- Spring Security's token CSRF protection is disabled in favour of this header check.
- The SPA's fetch wrapper (`web/src/api/client.ts`) adds the header to every unsafe request.

## Consequences

- No token bootstrap call is needed before sign-up or login.
- Direct API callers (curl, integration tests) must send the header; the contract declares it as a required
  parameter on every unsafe operation.
- The defence relies on browsers enforcing CORS preflight for custom headers, which every current browser does.
- If the API is ever served to other origins on purpose, this decision must be revisited together with CORS.

## Alternatives

- Spring Security's synchroniser or double-submit CSRF token: works, but needs a token bootstrap call before
  sign-up and login; the custom header gives the same protection for a JSON-only API with no CORS.
- `SameSite=Strict` alone: does not separate different ports on `localhost` (same site).
- Checking `Origin` / `Sec-Fetch-Site`: viable, but the expected origin differs between the Vite dev server and a
  direct API call, so it needs an allow-list setting; the header check needs none.
