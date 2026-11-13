---
type: api
title: Common problem model (RFC 9457 errors)
summary: Every API error is an application/problem+json body with a closed code enum and per-field errors, as defined by common-problem-v1.yaml and written by one component.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/contracts/common-problem-v1.yaml
  - api/src/main/java/app/todo/common/ProblemWriter.java
  - api/src/main/java/app/todo/common/ProblemCode.java
  - api/src/main/java/app/todo/common/ProblemDetailsAdvice.java
---

# Common problem model

**Frozen contract.** [common-problem-v1.yaml](../../specs/CHG-0001/contracts/common-problem-v1.yaml): components only
(`Problem`, `FieldError`, shared error responses, the `X-Todo-Client` header and the session cookie).

**Implementation.** One writer, `ProblemWriter` (`api/src/main/java/app/todo/common/ProblemWriter.java`), called by
`ProblemDetailsAdvice`, `ProblemAuthenticationEntryPoint`, `ClientHeaderFilter`, `ProblemRequestRejectedHandler` and
`ProblemErrorController`. `ProblemCode` is one closed enum equal to the contract's `code` enum
([ADR-0011](../decisions/adr-0011-problem-details-error-model.md)).

| Cause | Status | `code` |
|---|---|---|
| Field validation failed | 400 | `VALIDATION_FAILED` |
| Body not parseable JSON, or missing | 400 | `MALFORMED_REQUEST` |
| Firewall or other framework 4xx | that 4xx | `REQUEST_REJECTED` |
| No valid session | 401 | `UNAUTHENTICATED` |
| Wrong login | 401 | `INVALID_CREDENTIALS` |
| `X-Todo-Client` missing or wrong | 403 | `CLIENT_HEADER_MISSING` |
| No such to-do, unknown route | 404 | `NOT_FOUND` |
| Method not defined | 405 | `METHOD_NOT_ALLOWED` |
| Wrong or missing `Content-Type` | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| Identifier taken | 409 | `IDENTIFIER_TAKEN` |
| Anything else | 500 | `INTERNAL_ERROR` (fixed sentence; stack trace logged, never returned) |

The table is the SDD section 6 table ([sdd.md](../../specs/CHG-0001/sdd.md#6-error-model-adr-0011)).

**Rule.** Field messages are shown as sent by the SPA, so they must be plain language and say how to fix the field
(NFR-006 AC-1, [NFR-004](../requirements/nfr-004.md) AC-2).

**Tests.** [API integration tests](../tests/api-integration-tests.md) (error sources, `ProblemCode` equals the contract enum),
[contract tests](../tests/contract-tests.md).
