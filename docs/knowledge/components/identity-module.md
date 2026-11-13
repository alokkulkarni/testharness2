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

**Behaviour.**
- Sign-up trims and validates the identifier, hashes the password and inserts the user; a duplicate (case-insensitive,
  via `identifier_key`) gives 409 `IDENTIFIER_TAKEN`. No session starts at sign-up. Rules: [identifier-rules](../rules/identifier-rules.md),
  [password-rules](../rules/password-rules.md). Requirements: [REQ-AUTH-001](../requirements/req-auth-001.md).
- Login looks the user up by lower-cased key, and a missing identifier is verified against a dummy hash before the same
  401 `INVALID_CREDENTIALS` is returned as for a wrong password (`api/src/main/java/app/todo/auth/AuthService.java`).
  Requirement: [REQ-AUTH-002](../requirements/req-auth-002.md).
- Sessions last 8 hours absolute from login, and activity does not extend them ([session-lifetime](../rules/session-lifetime.md)).
- Logout deletes the session row and clears the cookie (REQ-AUTH-004 AC-3).
- `getSession` reads the identifier by id (`UserRepository.findById`) and returns the `expiresAt` stored in the authentication details.

**Interfaces.** Endpoints `/api/v1/auth/signup`, `/api/v1/auth/login`, `/api/v1/auth/logout`, `/api/v1/auth/session`.

**Tests.** [API integration tests](../tests/api-integration-tests.md), [architecture tests](../tests/architecture-tests.md),
[contract tests](../tests/contract-tests.md).

**Decisions.** [ADR-0004](../decisions/adr-0004-server-side-session-cookie.md), [ADR-0005](../decisions/adr-0005-argon2id-password-hashing.md),
[ADR-0008](../decisions/adr-0008-absolute-session-lifetime.md), [ADR-0009](../decisions/adr-0009-spring-jdbc-without-jpa.md).
