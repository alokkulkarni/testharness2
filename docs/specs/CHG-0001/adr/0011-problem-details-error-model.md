# ADR-0011 RFC 9457 problem details for every error, built by one `ProblemWriter`

Status: proposed (CHG-0001, N03-spec). Requirements: NFR-004 AC-2, REQ-TODO-002 AC-10, REQ-AUTH-003 AC-3,
REQ-AUTH-004 AC-1, REQ-ISO-001 AC-2. Split out of ADR-0002 and extended for critic finding F-4 (N03-spec round 1).

## Context

Errors must name the offending field (REQ-TODO-002 AC-10) and never expose stack traces, SQL, passwords or hashes
(NFR-004 AC-2, REQ-AUTH-003 AC-3). In Spring, error responses come from several places: controller exceptions
(handled by a `@RestControllerAdvice`), servlet filters and Spring Security (which run before any controller and
are not seen by the advice), Spring Security's request firewall, and the servlet container's `/error` forward
(Spring Boot's default body there does not match the contract).

## Decision

- Every error response under `/api/**` is `application/problem+json` with the contract's `Problem` schema and a
  `code` from its enum. The status-to-code mapping is in sdd.md §6.
- One component, `app.todo.common.ProblemWriter`, is the only code that builds and writes a Problem body. It writes
  directly to the servlet response, so the body does not depend on the request's `Accept` header. Its callers:
  - `ProblemDetailsAdvice` (`@RestControllerAdvice`): application exceptions and Spring MVC exceptions (unreadable
    body, unsupported media type, method not allowed, no handler or resource found, any other 4xx).
  - `ProblemAuthenticationEntryPoint`: Spring Security's entry point for unauthenticated requests (401).
  - `ClientHeaderFilter`: 403 `CLIENT_HEADER_MISSING` (ADR-0007).
  - `ProblemRequestRejectedHandler`: Spring Security firewall rejections (400 `REQUEST_REJECTED`).
  - `ProblemErrorController`: replaces Spring Boot's `/error` handling for anything the container forwards there;
    the whitelabel page is disabled.
- Unexpected exceptions become 500 `INTERNAL_ERROR` with a fixed sentence; the stack trace is logged server-side
  with the request id and never returned.

## Consequences

- Clients (the SPA and tests) parse one error shape for every status.
- Adding a new error source means routing it through `ProblemWriter`; an integration test sends one request per
  source (sdd.md §14, NFR-004) and checks the body against the `Problem` schema.
- 405 and unknown-route responses have no contract operation, so their test validates the `Problem` schema directly
  rather than through the operation-level contract validator (ADR-0012).

## Alternatives

- Spring's built-in `ProblemDetail` support (`spring.mvc.problemdetails.enabled`): covers Spring MVC exceptions but
  not filters, the security entry point or the firewall, and its body lacks the contract's `code` and `errors`.
- Spring Boot's default `/error` JSON: a different shape from the contract and may include exception details.
- Separate body-building code in each filter: the shapes would drift.
