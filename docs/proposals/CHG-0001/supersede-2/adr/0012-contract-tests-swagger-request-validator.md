# ADR-0012 Contract tests with openapi-request-validator (formerly swagger-request-validator) and an endpoint inventory

> Draft note for applying supersede-2 (remove when applying): copy to `docs/specs/CHG-0001/adr/0012-contract-tests-swagger-request-validator.md` (file name kept); changed for Q56 and NFR-007 AC-3 (three contract files, renamed validator); see the note in `sdd.md`.

Status: proposed (CHG-0001, N03-spec); amended for Q56 and NFR-007 AC-3. Requirements: NFR-004, REQ-AUTH-005 AC-1,
NFR-007 AC-3, and every requirement whose interface is in `contracts/identity-api-v1.yaml` or
`contracts/todos-api-v1.yaml`. Split out of ADR-0002 (critic finding F-6, N03-spec round 1). The file name keeps the
validator's former name.

## Context

The API contract is three OpenAPI 3.1 files (Source: clarifications Q56): `contracts/identity-api-v1.yaml` (identity
module), `contracts/todos-api-v1.yaml` (to-dos module) and `contracts/common-problem-v1.yaml` (components only:
`Problem`, `FieldError`, the shared error responses, the `X-Todo-Client` header and the session cookie), linked by
relative `$ref`s. Together they are the source of truth for the API. Drift between them and the implementation must
fail the build, no operation may exist that the contract does not list (REQ-AUTH-005 AC-1: no user-listing or
cross-user operation), and every operation must belong to exactly one module (NFR-007 AC-3).

The validator chosen in the first freeze was renamed in its v3.0.0: "Prior to v3.0.0 this project was known as
swagger-request-validator" (README, github.com/atlassian/swagger-request-validator). Its v3 Spring WebMVC adapter is
"Compatible with Spring 7+, Spring Boot 4+ ... Requires JDK17+", and its OpenAPI 3.1 support is "partial"
(docs/FEATURES.md).

## Decision

- Test-only dependencies **com.atlassian.oai:openapi-request-validator-core** (3.0.0 verified) and its MockMvc
  adapter (Apache-2.0, maintained), pinned by the java-engineer. The adapter's artifact id,
  `openapi-request-validator-mockmvc`, follows the README's v3 naming and was not verified by the spike; the
  java-engineer confirms it when pinning, on the Spring Boot major of ADR-0002.
- The tests read the frozen contract files where they are (`docs/specs/CHG-0001/contracts/`), never a copy: by
  file path, or as a test classpath location if the java-engineer adds that directory as a test resource directory.
  Both were verified (below).
- Every MockMvc integration request and response is validated with an `OpenApiInteractionValidator` built from the
  module file that defines the request's path (`identity-api-v1.yaml` for `/api/v1/auth/**`, `todos-api-v1.yaml`
  for `/api/v1/todos/**`); each module file pulls `common-problem-v1.yaml` in through its `$ref`s.
- An endpoint-inventory test (default profile) loads both module files and fails the build unless: the union of
  their path-and-method pairs equals the routes Spring MVC has mapped; every path-and-method pair appears in exactly
  one of the two files; and the handler method of each operation is declared by a controller in that file's module
  package (`identity-api-v1.yaml` → `app.todo.auth`, `todos-api-v1.yaml` → `app.todo.todo`) (NFR-007 AC-3).
- Error responses that have no contract operation (405, unknown route, `REQUEST_REJECTED`, forwarded 413 and 500)
  are checked against the `Problem` schema directly (ADR-0011): `common-problem-v1.yaml` is parsed with
  `io.swagger.parser.OpenAPIParser.readLocation(path, null, options)` (resolve on), and the body is validated with
  the validator's public `com.atlassian.oai.validator.schema.SchemaValidator(OpenAPI, MessageResolver)` and its
  `validate(String json, Schema schema, String keyPrefix)`.
- The validator does not enforce the cookie `apiKey` security scheme, so the 401 behaviour is not checked by the
  contract level: the REQ-AUTH-004 integration tests (every to-do operation without, with an expired and with a
  logged-out session → 401 `UNAUTHENTICATED`) remain its check.

Source of the tool facts above: java-engineer spike of 2026-11-06 (JDK 21 release, Maven 3.10, JUnit 5.10.2,
`openapi-request-validator-core:3.0.0`): an OpenAPI 3.1 module file with `$ref`s to a sibling components-only file
(parameter, response, schema, security scheme) loads through `OpenApiInteractionValidator.createForSpecificationUrl`
from an absolute file path, a `file:` URL and a classpath location; a required `$ref` header is enforced; a `$ref`
`NotFound` response accepts a valid `Problem` body and rejects a bad enum value; loading logs one harmless warning,
"Unknown keyword components"; a missing `todo_session` cookie gives no validation error, whether the scheme is local
or a `$ref`; direct validation against `Problem` works with `SchemaValidator` from either the common file or a
module file.

## Consequences

- The contract is exercised by the same tests that check behaviour; no separate contract suite to maintain.
- The validator supports OpenAPI 3.1 only as far as its current release does; constructs the validator cannot read
  are kept out of the contract (the contract uses plain 3.1 schemas and relative `$ref`s only).
- Validator v3 needs Spring 7 / Spring Boot 4 (sdd.md §13). If the java-engineer pins an earlier Spring Boot major,
  the 2.x line under its former coordinates (`swagger-request-validator-mockmvc`) is used instead; the decision is
  otherwise unchanged.
- The inventory test also guards the module split: a controller serving an operation of the other module's file
  fails the build.

## Alternatives

- Spring Cloud Contract: consumer-driven stubs, more than a single-team API needs.
- Schemathesis: property-based and thorough, but needs a Python runtime in the build.
- Hand-written response assertions only: no check that the contract and code agree.
- Validating against one bundled file produced by `redocly bundle`: works (react-engineer spike), but adds a build
  step and a generated artefact, while the validator already resolves the cross-file `$ref`s.
