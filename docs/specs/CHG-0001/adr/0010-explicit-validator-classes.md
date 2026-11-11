# ADR-0010 Explicit validator classes instead of Bean Validation annotations

Status: proposed (CHG-0001, N03-spec). Requirements: NFR-004 AC-2, REQ-TODO-002, REQ-AUTH-001 AC-3, AC-7..AC-11,
REQ-LIST-002 AC-7, REQ-LIST-003 AC-7. Split out of ADR-0002 (critic finding F-6, N03-spec round 1).

## Context

The server is the authority for input rules (NFR-004 AC-2). The rules trim with a defined whitespace set before
measuring, count Unicode code points, split identifiers into email and username rules, and must report every field
error of a request together, each naming its field (REQ-TODO-002 AC-10). The SPA mirrors the same rules (sdd.md §5).

## Decision

- Validation lives in plain classes: `IdentifierRules`, `PasswordRules` (package `auth`), `TodoValidator` and the
  `page` / `q` handling in `TodoQuery` (package `todo`), all using `app.todo.common.Whitespace` for trimming
  (sdd.md §5).
- Each validator returns the complete list of field errors (`field`, `code`, `message`) for a request; a non-empty
  list becomes one 400 `VALIDATION_FAILED` Problem (ADR-0011).
- Request bodies are bound to Java records with Jackson `FAIL_ON_UNKNOWN_PROPERTIES` on; an unknown property becomes
  the field error `UNKNOWN_FIELD`, and a JSON value of the wrong type becomes `INVALID_TYPE`.
- No `jakarta.validation` annotations on request records.

## Consequences

- Every rule is a unit-testable method with explicit boundary tests (sdd.md §14).
- The rules are written twice (Java and TypeScript); the same boundary table drives both unit-test suites.

## Alternatives

- Bean Validation annotations: cannot express trim-then-length in code points or the email/username split without
  custom constraints, which would be as much code as plain validators, and error order and grouping are harder to
  control.
- A JSON Schema validator driven by the contract: covers types and raw lengths, but not trimming, code-point counts
  after trimming or the email rule.
