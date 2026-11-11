# ADR-0003 SQLite file database through sqlite-jdbc

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-DATA-001, NFR-002, NFR-005; constraint C-1. In round 2 the
migration, connection, case-insensitivity and timestamp-format decisions were split out into ADR-0014 to ADR-0017.

## Context

The requester chose SQLite (Q13). Data must survive restarts (REQ-DATA-001 AC-1), be held in a SQLite file
(AC-2), and the file must be created on first start with no manual setup (AC-3). The app runs locally with no
hosted database (NFR-002).

## Decision

- Driver **org.xerial:sqlite-jdbc** (Apache-2.0, maintained), pinned by the java-engineer.
- One database file whose path comes from `TODO_DB_PATH` (default `./data/todo.db`, relative to the API's working
  directory). `DatabaseConfig` creates the parent directory at start-up; the driver creates the file on first
  connection.
- The schema is created and evolved by migrations (ADR-0014); restarting never re-creates or truncates tables.

## Consequences

- Zero setup for a local run; the database is one file a developer can open with the `sqlite3` tool (REQ-DATA-001 AC-2).
- SQLite allows one writer at a time and has no native date-time type; ADR-0016 and ADR-0017 address these.
- SQLite's case folding is ASCII-only; ADR-0015 addresses case-insensitive uniqueness and search.

## Alternatives

- H2 file mode: recommended in Q13 but the requester chose SQLite.
- An in-memory database: loses data on restart (REQ-DATA-001 AC-1).
