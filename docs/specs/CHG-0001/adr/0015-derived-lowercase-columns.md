# ADR-0015 Case-insensitive uniqueness and search through derived lower-cased columns

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-AUTH-001 AC-6, REQ-AUTH-002 AC-5, REQ-LIST-002 AC-1, AC-2,
AC-6. Split out of ADR-0003 (critic finding F-6, N03-spec round 1).

## Context

Identifiers are unique regardless of letter case (REQ-AUTH-001 AC-6) and login ignores case (REQ-AUTH-002 AC-5).
Search is a case-insensitive "contains" over title and description, with `%`, `_` and quotes matched literally
(REQ-LIST-002 AC-1, AC-2, AC-6). SQLite's built-in `LIKE`, `lower()` and `NOCASE` fold ASCII letters only, and
`LIKE` treats `%` and `_` as wildcards.

## Decision

- `users.identifier_key`: the trimmed identifier lowered in Java with `Locale.ROOT`, under a `UNIQUE` constraint;
  sign-up and login look users up by it. Identifiers are ASCII-only by the validation rules (sdd.md §5), so
  lowering is unambiguous.
- `todos.title_search` and `todos.description_search`: the stored value lowered in Java with `Locale.ROOT`, written
  on every insert and update of the source field.
- Search matches `instr(title_search, :q) > 0 OR instr(description_search, :q) > 0` with the search text trimmed
  and lowered the same way. `instr` is a literal substring test, so no character needs escaping.

## Consequences

- Folding covers non-ASCII letters (Java case mapping), not only ASCII.
- Derived columns must be kept in step with their source; repository tests cover every write path.
- Java's lower-casing is a simple case mapping, not full case folding (for example "ß" does not match "SS");
  accepted and recorded as a risk.
- `instr` cannot use an index; at 500 rows per user (NFR-003) a scan of the user's rows is fast.

## Alternatives

- `COLLATE NOCASE` and `LIKE ... ESCAPE`: ASCII-only folding and escaping rules that are easy to get wrong.
- A custom SQLite collation or function registered through the driver: works, but ties correctness to driver
  extension code.
- SQLite FTS5: word-based matching, not "contains"; unneeded at 500 rows.
