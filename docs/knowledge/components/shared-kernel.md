---
type: component
title: Shared kernel (api common package)
summary: The app.todo.common package holds the error model, the client-header filter, request ids, whitespace and clock support that both API modules use, and it depends on no module.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/common/ProblemWriter.java
  - api/src/main/java/app/todo/common/ProblemDetailsAdvice.java
  - api/src/main/java/app/todo/common/ClientHeaderFilter.java
  - api/src/main/java/app/todo/common/RequestIdFilter.java
  - api/src/main/java/app/todo/common/Whitespace.java
  - api/src/main/java/app/todo/common/ClockConfig.java
  - docs/specs/CHG-0001/sdd.md
---

# Shared kernel (`app.todo.common`)

**Responsibility.** Everything both modules share, and nothing module-specific. It is not a module in NFR-007 AC-3 terms.

**Classes** (`api/src/main/java/app/todo/common/`):
- Error model: `ProblemWriter` (the only code that builds an error body), `ProblemCode` (closed enum), `ProblemDetailsAdvice`,
  `ProblemAuthenticationEntryPoint`, `ProblemRequestRejectedHandler`, `ProblemErrorController`, `ApiException`,
  `ValidationException`, `NotFoundException`, `FieldError`. See [common problem model](../apis/common-problem-model.md).
- Request handling: `ClientHeaderFilter` (`X-Todo-Client` header, 403), `RequestIdFilter` (request id and per-request log line),
  `BodyReader`.
- Values and time: `Whitespace` (the 25 `White_Space` code points, [whitespace-trimming](../rules/whitespace-trimming.md)),
  `UtcTimestamps`, `ClockConfig` (injected `Clock`, [ADR-0013](../decisions/adr-0013-injected-clock-and-e2e-test-clock.md)).

**Rules.** `common` uses no module and not `config` (architecture rule, [architecture tests](../tests/architecture-tests.md)).
`ProblemDetailsAdvice` maps any `ApiException` generically, so it names no module's exception type
([ADR-0011](../decisions/adr-0011-problem-details-error-model.md), [ADR-0018](../decisions/adr-0018-modular-monolith-module-boundaries.md)).

**Requirements served.** NFR-004 AC-2 (no internals in responses), REQ-TODO-002 AC-1 (trim), NFR-006 (on the SPA side,
see [web SPA](web-spa.md)).

**Tests.** [API integration tests](../tests/api-integration-tests.md) (error sources, Problem codes, filter exception)
and the architecture suite.
