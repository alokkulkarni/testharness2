# ADR-0017 Timestamps as fixed-width UTC ISO-8601 text, due dates as `YYYY-MM-DD` text

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-TODO-001 AC-1, REQ-TODO-003 AC-1, REQ-TODO-004 AC-1,
REQ-TODO-002 AC-7, REQ-LIST-003 AC-2. Split out of ADR-0003 (critic finding F-6, N03-spec round 1).

## Context

SQLite has no date-time column type. The list is ordered newest first by creation time with no repeats or gaps
across pages (REQ-TODO-003 AC-1, REQ-LIST-003 AC-2); updated timestamps must strictly advance (REQ-TODO-004 AC-1);
a due date is a calendar date with no time or zone (REQ-TODO-002 AC-7).

## Decision

- `created_at`, `updated_at` (todos), `created_at` (users, sessions) and `expires_at` (sessions) are stored as UTC
  text with exactly three fractional digits: `yyyy-MM-ddTHH:mm:ss.SSSZ` (for example `2026-11-06T10:15:30.123Z`).
  Because every value has the same width and zone, text order equals time order, so `ORDER BY created_at DESC` and
  `expires_at <= :now` compare correctly.
- `due_date` is stored as `YYYY-MM-DD` text and never converted to an instant.
- The API returns the same strings (contract `Todo.createdAt`, `updatedAt`, `dueDate`).

## Consequences

- Rows are readable in the `sqlite3` tool without conversion.
- Every write must format through one formatter; a value with a different width would break ordering, so repository
  tests check the stored format.
- Millisecond precision: two writes within one millisecond are kept apart by the "previous + 1 ms" rule
  (ADR-0006).

## Alternatives

- Integer epoch milliseconds: compact and ordered, but unreadable in the `sqlite3` tool and easy to confuse with
  seconds.
- SQLite `datetime()` text without fractional seconds: second precision cannot order writes within one second.
