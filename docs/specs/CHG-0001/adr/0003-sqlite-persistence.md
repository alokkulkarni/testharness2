# ADR-0003 SQLite file database through sqlite-jdbc, with Flyway migrations

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-DATA-001, REQ-DATA-002, REQ-AUTH-001 AC-6, REQ-LIST-002,
NFR-003, NFR-005; constraint C-1.

## Context

The requester chose SQLite (Q13). Data must survive restarts (REQ-DATA-001 AC-1), the file must be created on first
start with no manual setup (AC-3), identifiers are unique regardless of case (REQ-AUTH-001 AC-6), and search is
case-insensitive with `%`, `_` and quotes matched literally (REQ-LIST-002 AC-1, AC-6). SQLite's built-in `LIKE`,
`lower()` and `NOCASE` fold ASCII letters only, and SQLite allows one writer at a time.

## Decision

- Driver **org.xerial:sqlite-jdbc** (Apache-2.0). The file path comes from `TODO_DB_PATH` (default
  `./data/todo.db`, relative to the API's working directory); the API creates the parent directory at start-up and
  the driver creates the file on first connection.
- Schema created and evolved by **Flyway** (Apache-2.0) migrations in `api/src/main/resources/db/migration`
  (`V1__init.sql`), run at start-up; if the pinned Flyway major ships SQLite support as a separate module, that
  module is added too. Restarting never re-creates or truncates tables.
- Connection settings: `journal_mode=WAL`, `foreign_keys=ON`, `busy_timeout=5000`, and a HikariCP pool of
  **one connection**, so writes are serialised in the application and `SQLITE_BUSY` cannot occur between pool
  connections.
- Case-insensitive identifier uniqueness through a derived column `identifier_key` (the trimmed identifier lowered
  with `Locale.ROOT`) under a `UNIQUE` constraint. Identifiers are ASCII-only by the validation rules, so lowering is
  unambiguous.
- Case-insensitive search through derived columns `title_search` and `description_search` (the value lowered in
  Java with `Locale.ROOT`), matched with `instr(column, :term) > 0` on the equally lowered search text. `instr` is a
  literal substring test, so `%`, `_`, `\` and quotes need no escaping, and folding covers non-ASCII letters.
- Timestamps stored as ISO-8601 UTC text with exactly three fractional digits (`2026-11-06T10:15:30.123Z`), so text
  order equals time order; due dates stored as `YYYY-MM-DD` text.

## Consequences

- Zero setup for a local run; the database is one file a developer can open with the `sqlite3` tool (REQ-DATA-001 AC-2).
- Derived columns must be written on every insert and update of the source field; repository tests cover this.
- One connection limits throughput; with one local user and up to 500 to-dos (NFR-003) this is well within budget.
- Java's lower-casing is a simple case mapping, not full case folding (for example "ß" does not match "SS");
  accepted and recorded as a risk.

## Alternatives

- H2 file mode: recommended in Q13 but the requester chose SQLite.
- `COLLATE NOCASE` and `LIKE ... ESCAPE`: ASCII-only folding and escaping rules that are easy to get wrong.
- SQLite FTS5: word-based matching, not "contains"; unneeded at 500 rows.
- `spring.sql.init` with `schema.sql`: no migration history for later releases.
- A larger connection pool: concurrent writers would contend for SQLite's single write lock.
