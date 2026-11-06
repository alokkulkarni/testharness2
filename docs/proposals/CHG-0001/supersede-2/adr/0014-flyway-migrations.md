# ADR-0014 Schema managed by Flyway migrations run at start-up

> Draft note for applying supersede-2 (remove when applying): copy to `docs/specs/CHG-0001/adr/0014-flyway-migrations.md`; changed for NFR-007 (table owners, one `V1__init.sql` kept); see the note in `sdd.md`.

Status: proposed (CHG-0001, N03-spec); amended for NFR-007 (ADR-0018). Requirements: REQ-DATA-001 AC-1, AC-3,
NFR-007 AC-1. Split out of ADR-0003 (critic finding F-6, N03-spec round 1).

## Context

The schema must exist on first start with no manual setup (REQ-DATA-001 AC-3) and must never be re-created or
truncated on restart (AC-1). Later releases will change the schema. The API is split into an identity module and a
to-dos module that share one SQLite file (C-2, NFR-007 AC-1).

## Decision

- **Flyway** (Apache-2.0) runs migrations from `api/src/main/resources/db/migration` at start-up; this change adds
  `V1__init.sql` (sdd.md §3).
- If the pinned Flyway major ships SQLite support as a separate module, the java-engineer adds that module too.
- Applied migrations are never edited; later changes add new versioned files.
- One `V1__init.sql` is kept for both modules, because the one database file has one Flyway history. Each table has
  one owning module (sdd.md §3, ADR-0018): `users` and `sessions` are owned by the identity module (`app.todo.auth`),
  `todos` by the to-dos module (`app.todo.todo`), and `flyway_schema_history` by the composition root
  (`app.todo.config`); only a table's owner reads or writes it.

## Consequences

- A fresh file gets the full schema; an existing file gets only the migrations it lacks.
- Flyway's history table (`flyway_schema_history`) is added to the database file.
- One more runtime dependency.
- The foreign key `todos.user_id → users(id)` stays in the shared schema as an accepted coupling between the two
  modules (ADR-0018).

## Alternatives

- `spring.sql.init` with `schema.sql`: no migration history for later releases, and it must be made idempotent by
  hand.
- Liquibase: equally capable; XML/YAML changelogs are heavier than plain SQL files for three tables.
- One migration location per module: two Flyway histories over one file, or one history with interleaved version
  numbers, for three tables; one `V1__init.sql` with table owners is simpler.
