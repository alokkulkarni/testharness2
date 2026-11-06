# ADR-0018 Modular monolith: identity and to-dos modules with boundaries enforced by ArchUnit

> Draft note for applying supersede-2 (remove when applying): copy to `docs/specs/CHG-0001/adr/0018-modular-monolith-module-boundaries.md` (new file, for Q51, Q56, Q57, Q58); see the note in `sdd.md`.

Status: proposed (CHG-0001, N03-spec, supersede-2). Requirements: NFR-007 AC-1..AC-3, REQ-ISO-001, REQ-AUTH-004
AC-1, REQ-AUTH-002 AC-4; constraint C-2. Amends ADR-0002.

## Context

The first freeze has one Spring Boot API whose packages by feature (ADR-0002) carry no enforced boundary. The
requester asked for components split by the functionality they serve and chose a modular monolith: one Spring Boot
deployable split into strict modules with enforced boundaries, one process and one SQLite file, not separate
services (Source: clarifications Q51; C-2). Q57 accepted NFR-007: one application, process, SQLite file and
connection pool (AC-1); a test fails on a use of another module's unpublished type or on a dependency cycle (AC-2);
an identity module and a to-dos module, each contract operation in exactly one (AC-3). Q56 split the contract into
`identity-api-v1.yaml`, `todos-api-v1.yaml` and the shared `common-problem-v1.yaml`. Q58 chose ArchUnit to enforce
the boundaries.

The frozen package layout crossed these lines: `user` held only `UserRepository`; `common` held exceptions only
`auth` throws; `ClientHeaderFilter` (in `auth`) also guards the to-do routes; `TodoController` needs the caller's
internal user id with no named seam; and `getSession` returns `Session.user.identifier` although `sessions` holds
only `user_id` and `UserRepository` had no lookup by id.

## Decision

- **Modules** (packages under `app.todo`; names kept from the first freeze, sdd.md §12.1):
  - `auth` — the **identity module** (NFR-007 AC-3): sign-up, login, logout, session; contract
    `identity-api-v1.yaml`; tables `users`, `sessions`. Holds `AuthController`, `AuthService`, `SessionService`,
    `SessionRepository`, `UserRepository` (folded in from the former `user` package, with a new `findById`),
    `SessionCookieAuthenticationFilter`, `PasswordRules`, `IdentifierRules`, `IdentifierTakenException`,
    `InvalidCredentialsException`.
  - `todo` — the **to-dos module**: the to-do and list operations; contract `todos-api-v1.yaml`; table `todos`.
  - `common` — the **shared kernel**, not a module of AC-3: the error model (ADR-0011), `ClientHeaderFilter`
    (ADR-0007; it guards both modules' unsafe routes), `RequestIdFilter`, `ClockConfig` (ADR-0013), `Whitespace`.
  - `config` — the **composition root**, not a module: `SecurityConfig` (names both modules' paths, wires the
    filters) and `DatabaseConfig` (the only `DataSource`: HikariCP pool of one connection, pragmas, Flyway;
    ADR-0016, ADR-0014). The root package `app.todo` holds only the application class and is part of the
    composition root.
- **Published interface.** `auth` publishes one package to other modules, `app.todo.auth.api`, holding only
  `CurrentUser(long userId)`. `SessionCookieAuthenticationFilter` sets a `CurrentUser` as the Spring Security
  principal; `TodoController` reads it with `@AuthenticationPrincipal` and passes `userId` to `TodoService` and
  `TodoRepository`. The owner comes only from the session (REQ-ISO-001, REQ-AUTH-004 AC-1); the SQL
  `… WHERE public_id = ? AND user_id = ?` is unchanged. `todo` publishes nothing to other modules. Every public type
  of `common` is published to all.
- **Allowed dependencies** (no cycles): `todo` → `todo`, `common`, `auth.api`; `auth` → `auth`, `common`;
  `common` → no module and not `config`; `config` and the root package → any. Libraries (JDK, Spring, Jackson,
  HikariCP, Flyway, sqlite-jdbc) are outside the rules.
- **Repositories are package-private** in their module (`UserRepository`, `SessionRepository` in `auth`;
  `TodoRepository` in `todo`), so the compiler already stops another module from reaching a table's repository;
  ArchUnit enforces the rest.
- **Shared schema.** The physical foreign key `todos.user_id → users(id) ON DELETE CASCADE` is kept as an accepted
  coupling of the shared schema (one SQLite file, C-2). `todo` treats `userId` as an opaque number from
  `CurrentUser`; its SQL never names `users` or `sessions`. One `V1__init.sql` is kept with table owners (ADR-0014).
- **`getSession`** (REQ-AUTH-002 AC-4): the identity module reads the identifier with
  `UserRepository.findById(userId)` inside `auth`; no other module is involved (sdd.md §2, §4).
- **Enforcement (Q58): ArchUnit**, test-only dependency `com.tngtech.archunit:archunit-junit5` (Apache-2.0,
  maintained), pinned by the java-engineer. An architecture test over the main classes only (test classes such as
  `app.todo.TestTodoApplication` and `app.todo.testsupport.*` are excluded) checks: no cycles between `auth`, `todo`,
  `common`, `config`; `todo` uses only `todo`, `common`, `auth.api`; `auth` and `common` never use `todo`; `common`
  uses no module and not `config`; only `config` creates a `DataSource` or `HikariConfig`; every main class lies in
  `auth`, `auth.api`, `todo`, `common`, `config` or the root package. The rules are built for a
  given root package, so a non-vacuity test runs the same rules on a fixture package in the test source set that
  holds a deliberate violation of each rule and expects each to fail (sdd.md §14).
- **Logging context.** The per-request log's internal user id (sdd.md §10) is read from the logging context, where
  `SessionCookieAuthenticationFilter` puts it, so `common` needs no `auth` type.

Still valid unchanged: ADR-0003 (one SQLite file through sqlite-jdbc), ADR-0016 (one connection; with one
`DataSource` in `config` it is also NFR-007 AC-1's one pool), NFR-002 (local run, one `./mvnw spring-boot:run`) and
NFR-005 (React + TypeScript, Java/Spring Boot, HTTP, SQLite). ADR-0007 names `ClientHeaderFilter` without a package,
ADR-0009 already places `UserRepository` and `SessionRepository` in `auth`, ADR-0010 places `TodoQuery` in `todo`
and ADR-0013 places `ClockConfig` in `common`, so none of them changes.

## Consequences

- A forbidden dependency or a cycle fails `./mvnw test` (NFR-007 AC-2); the running application is unchanged (no
  runtime library, no new bean).
- The rules are written by hand: a new package must be added to them, and a rule that matches nothing would pass
  silently; the package-location rule guards the first risk and the non-vacuity test the second (sdd.md §13).
- `ClientHeaderFilter` and the exceptions only `auth` throws move package; behaviour is unchanged.
- Nothing in the database stops one module's SQL from naming another module's table; table ownership is enforced
  by where the repositories live and by review of `TodoRepository`'s SQL (sdd.md §14), not by the database.

## Alternatives

- **Spring Modulith** (`ApplicationModules.verify()`): checks cycles and access to module internals out of the box
  and documents modules, but its version follows the Spring Boot line, its test-support classes must be excluded
  from module detection, and it is a framework for two modules and a small kernel. Declined by the requester in Q58.
- **Separate services**, each its own process: declined by the requester in Q51 (C-2); it would also break NFR-007
  AC-1 (one process, one SQLite file, one pool).
- **A Maven module per API module** (`api-identity`, `api-todos`, `api-common`): the compiler would enforce the
  boundaries, but NFR-007 AC-1 asks for one application, ADR-0002 keeps one Maven module for a simple build, and
  `api/pom.xml` would become a parent with three children for three tables.
- **Logical foreign-key-free schema** (no `todos.user_id → users` constraint): removes the coupling, but loses the
  cascade and the integrity check SQLite gives for free in one file.
