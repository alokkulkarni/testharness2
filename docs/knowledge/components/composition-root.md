---
type: component
title: Composition root (api config package)
summary: The app.todo.config package wires the modules together, holds the security configuration and the only DataSource, and may depend on every module.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/config/SecurityConfig.java
  - api/src/main/java/app/todo/config/DatabaseConfig.java
  - api/src/main/java/app/todo/config/WebConfig.java
  - api/src/main/java/app/todo/TodoApplication.java
  - docs/specs/CHG-0001/sdd.md
---

# Composition root (`app.todo.config` and the root package)

**Responsibility.** Wiring only. The root package `app.todo` holds the application's main class
(`api/src/main/java/app/todo/TodoApplication.java`) and belongs to this root.

**Classes.**
- `SecurityConfig`: the Spring Security filter chain. It names both modules' paths, and it sets the order of checks
  (session, then client header, then route; see [REQ-AUTH-004](../requirements/req-auth-004.md)).
- `DatabaseConfig`: the only `DataSource` and `HikariConfig`. It creates the parent directory of `TODO_DB_PATH`, runs the
  SQLite pragmas, and keeps one connection ([ADR-0016](../decisions/adr-0016-single-connection-pool.md),
  NFR-007 AC-1).
- `WebConfig`: web MVC configuration. Its contents were not reviewed line by line for this bundle; see the file for detail.

**Rules.** This package may depend on every module; no module may depend on it ([architecture tests](../tests/architecture-tests.md)).

**Database.** The SQLite file is `TODO_DB_PATH`, default `./data/todo.db` (SDD section 11). The Flyway migration
`api/src/main/resources/db/migration/V1__init.sql` creates all three tables ([schema](../data/schema.md),
[ADR-0014](../decisions/adr-0014-flyway-migrations.md)).

**Requirements served.** NFR-007 AC-1, REQ-DATA-001 AC-3 (the database and its directory are created on first start).

**Tests.** [API integration tests](../tests/api-integration-tests.md) (persistence, module wiring, endpoint inventory).

**Known gap.** The run steps and `.env.example` that SDD section 11 describes are not in the repository at HEAD, so
the documented configuration is not visible in the repo ([overview](../system/overview.md#known-divergences-as-recorded-at-n12-and-n13)).
