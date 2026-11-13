---
type: api
title: Identity API (v1)
summary: The identity module's HTTP operations, signUp, logIn, logOut and getSession under /api/v1/auth, defined in identity-api-v1.yaml.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/contracts/identity-api-v1.yaml
  - api/src/main/java/app/todo/auth/AuthController.java
  - api/src/main/java/app/todo/auth/SessionService.java
---

# Identity API (v1)

**Frozen contract.** [identity-api-v1.yaml](../../specs/CHG-0001/contracts/identity-api-v1.yaml) (OpenAPI 3.1; the
authoritative source for every schema and limit; this page does not repeat them).

**Implementation.** `AuthController` in the [identity module](../components/identity-module.md)
(`api/src/main/java/app/todo/auth/AuthController.java`: `signUp` L34-L38, `logIn` L40-L47, `logOut` L49-L53, `getSession` L55-L59).

| Operation | Method and path | Auth | Requirements | Notes |
|---|---|---|---|---|
| `signUp` | POST `/api/v1/auth/signup` | none | [REQ-AUTH-001](../requirements/req-auth-001.md) | 201 creates the account, no session; 409 `IDENTIFIER_TAKEN` |
| `logIn` | POST `/api/v1/auth/login` | none | [REQ-AUTH-002](../requirements/req-auth-002.md) | sets the session cookie; wrong password and unknown identifier give the same 401 `INVALID_CREDENTIALS` |
| `logOut` | POST `/api/v1/auth/logout` | none | [REQ-AUTH-004](../requirements/req-auth-004.md) | deletes the session row, clears the cookie, 204 |
| `getSession` | GET `/api/v1/auth/session` | session | [REQ-AUTH-002](../requirements/req-auth-002.md) AC-4 | returns the identifier as stored at sign-up and `expiresAt`; 401 `UNAUTHENTICATED` without a session |

**Cookie.** Session cookie set by `AuthController.cookie` (`AuthController.java:L61-L69`): HttpOnly (L63), SameSite=Strict (L65),
Path=/ (L66), Max-Age from `SessionService.LIFETIME`, 8 hours (`SessionService.java:L23`), so 28800 seconds. `Secure` is set only
when `todo.cookie.secure` is true, and that defaults to false (`AuthController.java:L29`, `L64`). The cookie name comes from
`SessionCookieAuthenticationFilter.COOKIE_NAME`. The attribute tests are in `api/src/test/java/app/todo/auth/LoginSessionApiTest.java`
(a test, cited for the tested behaviour only; the claims above cite the code).
[ADR-0004](../decisions/adr-0004-server-side-session-cookie.md).

**Errors.** Every error is `application/problem+json` as in the [common problem model](common-problem-model.md).

**Tests.** [contract tests](../tests/contract-tests.md), [API integration tests](../tests/api-integration-tests.md).
