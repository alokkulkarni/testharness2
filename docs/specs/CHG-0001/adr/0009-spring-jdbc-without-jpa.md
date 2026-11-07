# ADR-0009 Hand-written SQL through Spring `JdbcClient`, no JPA

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-ISO-001, REQ-AUTH-005, NFR-004 AC-2, NFR-005. Split out of
ADR-0002 (critic finding F-6, N03-spec round 1).

## Context

Storage is SQLite (Q13, ADR-0003). Hibernate has no first-party SQLite dialect (only the community dialects module),
and the data model is three small tables. Every to-do statement must filter on the owner in the same statement as
the id (REQ-ISO-001 AC-2), which is easiest to review when the SQL is visible.

## Decision

- Repositories (`UserRepository`, `SessionRepository` in `auth`, `TodoRepository`) hold hand-written SQL run through
  Spring's `JdbcClient` (from `spring-boot-starter-jdbc`), with **bound parameters only**; no SQL is built by
  string concatenation of request values.
- **No JPA/Hibernate** and no Spring Data repositories.
- Every `TodoRepository` method takes the owner's user id as a required parameter (sdd.md §7).

## Consequences

- SQL is visible and testable; the owner filter is written into every statement.
- No ORM conveniences (dirty checking, lazy loading, generated queries); acceptable for three tables.
- Row mapping is hand-written and covered by repository integration tests.

## Alternatives

- Spring Data JPA + Hibernate community SQLite dialect: more moving parts, dialect maintained outside Hibernate core.
- Spring Data JDBC: derived queries hide the owner filter; custom queries would be needed anyway.
- jOOQ: type-safe SQL, but code generation and a further dependency for a small schema.
