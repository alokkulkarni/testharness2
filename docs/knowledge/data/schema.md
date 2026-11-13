---
type: data
title: Database schema overview (SQLite, V1)
summary: One Flyway migration creates the users, sessions and todos tables in a single SQLite file, with table owners and one accepted cross-module foreign key.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/resources/db/migration/V1__init.sql
  - docs/specs/CHG-0001/sdd.md
---

# Database schema (SQLite, V1)

**Migration.** `api/src/main/resources/db/migration/V1__init.sql` creates all tables ([ADR-0014](../decisions/adr-0014-flyway-migrations.md)).
Storage is one SQLite file at `TODO_DB_PATH` ([ADR-0003](../decisions/adr-0003-sqlite-persistence.md); REQ-DATA-001 AC-2).

| Table | Owner module | Concept |
|---|---|---|
| `users` | identity (`auth`) | [users](users.md) |
| `sessions` | identity (`auth`) | [sessions](sessions.md) |
| `todos` | to-dos (`todo`) | [todos](todos.md) |
| `flyway_schema_history` | composition root (`config`) | created by Flyway |

**Conventions.**
- Timestamps are UTC text `yyyy-MM-ddTHH:mm:ss.SSSZ`, so text order is time order ([ADR-0017](../decisions/adr-0017-timestamps-as-utc-text.md)).
- Lower-case copies of identifiers, titles and descriptions drive case-insensitive matching ([ADR-0015](../decisions/adr-0015-derived-lowercase-columns.md)).
- Only a table's owner reads or writes it. The to-dos module's SQL names only `todos`.

**Accepted coupling.** `todos.user_id` references `users(id)` (`ON DELETE CASCADE`). This is the only foreign key
between two modules' tables, kept as an accepted coupling of the shared schema
([sdd.md section 3](../../specs/CHG-0001/sdd.md#3-data-model-flyway-v1__initsql-adr-0014-adr-0015-adr-0017-adr-0018); [ADR-0018](../decisions/adr-0018-modular-monolith-module-boundaries.md)).

**Diagram.** [data model (PNG)](../diagrams/png/todo-data-model.png), [data model (PDF)](../diagrams/pdf/todo-data-model.pdf).
