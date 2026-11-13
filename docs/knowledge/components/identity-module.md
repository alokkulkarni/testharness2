---
type: component
title: Identity module (api auth package)
summary: The Spring Boot identity module (package app.todo.auth) handles sign-up, login, logout, the session cookie and password hashing, and publishes one type, CurrentUser, to the to-dos module.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/auth/AuthController.java
  - api/src/main/java/app/todo/auth/AuthService.java
  - api/src/main/java/app/todo/auth/SessionService.java
  - api/src/main/java/app/todo/auth/SessionCookieAuthenticationFilter.java
  - api/src/main/java/app/todo/auth/PasswordHasher.java
  - api/src/main/java/app/todo/auth/JdbcUserRepository.java
  - api/src/main/java/app/todo/auth/JdbcSessionRepository.java
  - api/src/main/java/app/todo/auth/api/CurrentUser.java
  - docs/specs/CHG-0001/sdd.md
---

# Identity module (`app.todo.auth`)

**Responsibility.** Accounts, login, the server-side session and its cookie, and the identifier and password rules.
Contract: [identity API](../apis/identity-api.md). Tables it owns: [users](../data/users.md) and [sessions](../data/sessions.md).

**Classes** (from `api/src/main/java/app/todo/auth/`): `AuthController`, `AuthService`, `SessionService`,
`SessionCookieAuthenticationFilter`, `SessionDetails`, `PasswordRules`, `PasswordHasher` (Argon2id),
`IdentifierRules`, `IdentifierTakenException`, `InvalidCredentialsException`, `UserRepository` / `JdbcUserRepository`,
`SessionRepository` / `JdbcSessionRepository`.

**Published seam.** `app.todo.auth.api.CurrentUser(long userId)` is the only type this module publishes
(`api/src/main/java/app/todo/auth/api/CurrentUser.java`). The filter makes each valid session a request whose
principal is a `CurrentUser`; the to-dos module reads only that number ([todos-module](todos-module.md)). This is the
seam behind REQ-ISO-001 and NFR-007 AC-2 ([ADR-0018](../decisions/adr-0018-modular-monolith-module-boundaries.md)).

**Behaviour** (all as-built claims below cite the code they describe):
- Sign-up trims and validates the identifier, hashes the password and inserts the user (`AuthService.java:L31-L63`).
  The insert is `ON CONFLICT (identifier_key) DO NOTHING` (`JdbcUserRepository.java:L25-L27`), so a duplicate, matched
  case-insensitively, makes `signUp` throw `IdentifierTakenException` (`AuthService.java:L59-L60`), which the API answers
  as 409 `IDENTIFIER_TAKEN`. No session starts at sign-up: `signUp` returns the identifier without calling `SessionService`
  (`AuthService.java:L58-L62`). Rules: [identifier-rules](../rules/identifier-rules.md),
  [password-rules](../rules/password-rules.md). Requirements: [REQ-AUTH-001](../requirements/req-auth-001.md).
- Login looks the user up by identifier key (`AuthService.java:L88`). A password over 128 characters is refused without
  hashing (`AuthService.java:L84-L87`). A missing identifier is verified against a dummy hash (`AuthService.java:L89-L92`)
  before the same `InvalidCredentialsException` (401 `INVALID_CREDENTIALS`) is thrown as for a wrong password
  (`AuthService.java:L93-L95`). A correct login issues a session (`AuthService.java:L96-L97`).
  Requirement: [REQ-AUTH-002](../requirements/req-auth-002.md).
- Sessions last 8 hours absolute from login: `SessionService.LIFETIME` is 8 hours (`SessionService.java:L23`) and `issue`
  sets `expiresAt` to now plus that lifetime (`SessionService.java:L39-L42`). Activity does not extend them: `resolve`
  reads the stored `expiresAt`, deletes an expired row, and never writes a new expiry (`SessionService.java:L46-L57`).
  Rule: [session-lifetime](../rules/session-lifetime.md).
- The session token is 32 random bytes; only its SHA-256 hex is stored (`SessionService.java:L35-L43`,
  `SessionService.java:L64-L70`, `JdbcSessionRepository.java:L19-L27`).
- Logout deletes the session row (`AuthService.java:L107-L111`, `SessionService.java:L60-L62`) and the controller answers
  204 with a cookie of Max-Age zero (`AuthController.java:L49-L53`) (REQ-AUTH-004 AC-3).
- `getSession` reads the identifier by user id (`AuthService.java:L101-L105`, `UserRepository.findById`) and returns the
  `expiresAt` from the authentication details (`AuthController.java:L55-L59`).

**Interfaces.** Endpoints `/api/v1/auth/signup`, `/api/v1/auth/login`, `/api/v1/auth/logout`, `/api/v1/auth/session`
(`AuthController.java:L34-L59`).

**Tests.** [API integration tests](../tests/api-integration-tests.md), [architecture tests](../tests/architecture-tests.md),
[contract tests](../tests/contract-tests.md).

**Decisions.** [ADR-0004](../decisions/adr-0004-server-side-session-cookie.md), [ADR-0005](../decisions/adr-0005-argon2id-password-hashing.md),
[ADR-0008](../decisions/adr-0008-absolute-session-lifetime.md), [ADR-0009](../decisions/adr-0009-spring-jdbc-without-jpa.md).
