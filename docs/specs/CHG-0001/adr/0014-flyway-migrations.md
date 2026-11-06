# ADR-0014 Schema managed by Flyway migrations run at start-up

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-DATA-001 AC-1, AC-3. Split out of ADR-0003 (critic finding
F-6, N03-spec round 1).

## Context

The schema must exist on first start with no manual setup (REQ-DATA-001 AC-3) and must never be re-created or
truncated on restart (AC-1). Later releases will change the schema.

## Decision

- **Flyway** (Apache-2.0) runs migrations from `api/src/main/resources/db/migration` at start-up; this change adds
  `V1__init.sql` (sdd.md §3).
- If the pinned Flyway major ships SQLite support as a separate module, the java-engineer adds that module too.
- Applied migrations are never edited; later changes add new versioned files.

## Consequences

- A fresh file gets the full schema; an existing file gets only the migrations it lacks.
- Flyway's history table (`flyway_schema_history`) is added to the database file.
- One more runtime dependency.

## Alternatives

- `spring.sql.init` with `schema.sql`: no migration history for later releases, and it must be made idempotent by
  hand.
- Liquibase: equally capable; XML/YAML changelogs are heavier than plain SQL files for three tables.
