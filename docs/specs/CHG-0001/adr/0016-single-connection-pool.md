# ADR-0016 One SQLite connection, WAL journal and enforced foreign keys

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-DATA-001 AC-1, REQ-DATA-002 AC-2, NFR-003. Split out of
ADR-0003 (critic finding F-6, N03-spec round 1).

## Context

SQLite allows one writer at a time; a second connection that tries to write while another holds the write lock
gets `SQLITE_BUSY`. Concurrent edits to different to-dos must both be stored (REQ-DATA-002 AC-2). Foreign keys are
off by default in SQLite. One local user with up to 500 to-dos must see results within 1 second (NFR-003).

## Decision

- HikariCP pool of exactly **one connection**, so database access is serialised in the application and
  `SQLITE_BUSY` cannot occur between pool connections.
- Pragmas on the connection: `journal_mode=WAL`, `foreign_keys=ON`, `busy_timeout=5000`.

## Consequences

- No lost or refused writes from lock contention; concurrent requests queue for the connection.
- One connection limits throughput; with one local user and up to 500 to-dos this is well within budget, and the
  NFR-003 performance test guards it.
- A slow request delays every other request; no request holds the connection for more than one transaction.

## Alternatives

- A larger connection pool: concurrent writers would contend for SQLite's single write lock and need retry logic.
- Rollback journal (default): readers block during writes; WAL lets the read transaction of a list see a consistent
  snapshot.
