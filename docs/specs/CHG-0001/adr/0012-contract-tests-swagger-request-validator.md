# ADR-0012 Contract tests with openapi-request-validator (formerly swagger-request-validator) and an endpoint inventory

Status: proposed (CHG-0001, N03-spec); amended for Q56 and NFR-007 AC-3. Requirements: NFR-004, REQ-AUTH-005 AC-1,
NFR-007 AC-3, and every requirement whose interface is in `contracts/identity-api-v1.yaml` or
`contracts/todo-api-v1.yaml`. Split out of ADR-0002 (critic finding F-6, N03-spec round 1). The file name keeps the
validator's former name. The to-dos module's contract keeps the first freeze's file name `todo-api-v1.yaml`: that
contract file is kept rather than removed, because a re-freeze replaces files and removes none, so the to-dos module
inherits its name; there is no wire change (Source: clarifications Q63).

## Context

The API contract is three OpenAPI 3.1 files (Source: clarifications Q56): `contracts/identity-api-v1.yaml` (identity
module), `contracts/todo-api-v1.yaml` (to-dos module) and `contracts/common-problem-v1.yaml` (components only:
`Problem`, `FieldError`, the shared error responses, the `X-Todo-Client` header and the session cookie), linked by
relative `$ref`s. Together they are the source of truth for the API. Drift between them and the implementation must
fail the build, no operation may exist that the contract does not list (REQ-AUTH-005 AC-1: no user-listing or
cross-user operation), and every operation must belong to exactly one module (NFR-007 AC-3).

The validator chosen in the first freeze was renamed in its v3.0.0. Per the project README
(github.com/atlassian/swagger-request-validator), read 2026-11-06 (summarised, not verbatim): before v3.0.0 the
project was known as swagger-request-validator; its new artifacts are `com.atlassian.oai:openapi-request-validator-*`;
its v3 Spring WebMVC adapter is compatible with Spring 7+ and Spring Boot 4+ and requires JDK 17+. Per its
docs/FEATURES.md, read the same day (summarised, not verbatim), its OpenAPI 3.1 support is partial.

## Decision

- Test-only dependencies **com.atlassian.oai:openapi-request-validator-core** (3.0.0, resolved in the tool spikes below) and its MockMvc
  adapter (Apache-2.0 and maintained per the first freeze's ADR for the 2.x line; unverified, not rechecked for
  v3), pinned by the java-engineer. The adapter's artifact id,
  `openapi-request-validator-mockmvc`, follows the README's v3 naming and was not verified by the spikes; the
  java-engineer confirms on Maven Central that this v3 MockMvc adapter exists when pinning. If it does not exist, the
  java-engineer stops and reports it as a spec question; no other validator line or artifact is used without a
  re-freeze.
- The tests read the frozen contract files where they are (`docs/specs/CHG-0001/contracts/`), never a copy: by
  file path, or as a test classpath location if the java-engineer adds that directory as a test resource directory.
  Both were verified (below).
- Every MockMvc integration request and response is validated with an `OpenApiInteractionValidator` built from the
  module file that defines the request's path (`identity-api-v1.yaml` for `/api/v1/auth/**`, `todo-api-v1.yaml`
  for `/api/v1/todos/**`); each module file pulls `common-problem-v1.yaml` in through its `$ref`s.
- An endpoint-inventory test (default profile) loads both module files and fails the build unless: the union of
  their path-and-method pairs equals the routes Spring MVC has mapped; every path-and-method pair appears in exactly
  one of the two files; and the handler method of each operation is declared by a controller in that file's module
  package (`identity-api-v1.yaml` → `app.todo.auth`, `todo-api-v1.yaml` → `app.todo.todo`) (NFR-007 AC-3). The
  mapped routes are compared excluding, by name, only `/error` (all methods of `ProblemErrorController`, `common`;
  sdd.md §6); any other mapped route missing from both module files fails the test, and the java-engineer stops and
  reports it as a spec question.
- Error responses that have no contract operation (405, unknown route, `REQUEST_REJECTED`, forwarded 413 and 500)
  are checked against the `Problem` schema directly (ADR-0011): `common-problem-v1.yaml` is parsed with
  `io.swagger.parser.OpenAPIParser.readLocation(path, null, options)` (resolve on), and the body is validated with
  the validator's public `com.atlassian.oai.validator.schema.SchemaValidator(OpenAPI, MessageResolver)` and its
  `validate(String json, Schema schema, String keyPrefix)`.
- The validator does not enforce the cookie `apiKey` security scheme, so the 401 behaviour is not checked by the
  contract level: the REQ-AUTH-004 integration tests (every to-do operation without, with an expired and with a
  logged-out session → 401 `UNAUTHENTICATED`) remain its check.
- Contract lint: `redocly lint` with `extends: [recommended]` (`@redocly/cli`, ADR-0001) is run on the two module
  roots only, `identity-api-v1.yaml` and `todo-api-v1.yaml`; `common-problem-v1.yaml` is linted through their
  `$ref`s. It is not linted on its own: having no operations and no servers, it fails `no-empty-servers` and warns
  `no-unused-components`. The lint runs as the `web/package.json` script `lint:contracts`
  (`pnpm -C web lint:contracts`, `redocly lint` on the two module roots), part of the contract level of sdd.md §14
  and run in the gates.

Source of the tool facts above: tool spikes of 2026-11-06, recorded in
`.harness/changes/CHG-0001/tool-spikes.md`. In short (java-engineer spikes A and
B, JDK Temurin 23.0.2 with `release` 21, Maven 3.10.0, JUnit 5.10.2, `openapi-request-validator-core:3.0.0`): an
OpenAPI 3.1 module file with `$ref`s to a sibling components-only file (parameter `ClientHeader`, response
`NotFound` → `Problem`, security scheme `sessionCookie`) loads through
`OpenApiInteractionValidator.createForSpecificationUrl` from an absolute file path, a `file:` URL and a classpath
location; a missing `$ref` header is reported; the `$ref` `NotFound` response accepts a valid `Problem` body and
rejects a bad enum value; the first load logs one warning, "Unknown keyword components", and still succeeds; a
missing `todo_session` cookie gives no validation error, whether the scheme is local or a `$ref`; direct validation
against `Problem` works with `SchemaValidator` from either the common file or a module file. Lint results:
react-engineer check of the three contract files (`@redocly/cli` 2.76.0), §5 of the same file.

## Consequences

- The contract is exercised by the same tests that check behaviour; no separate contract suite to maintain.
- The validator supports OpenAPI 3.1 only as far as its current release does; constructs the validator cannot read
  are kept out of the contract (the contract uses plain 3.1 schemas and relative `$ref`s only).
- The v3 adapter is documented as compatible with Spring 7+ / Spring Boot 4+ (tool spikes of 2026-11-06 §1, above).
  The backend targets the latest Spring Boot 4.x release, pinned exactly in `api/pom.xml` by the java-engineer
  (Source: clarifications Q60; ADR-0002, sdd.md §12.1 item 21, §13), which meets it.
- `MethodNotAllowed` and `RequestRejected` are defined only in `common-problem-v1.yaml` and referenced by no
  operation (as in the first freeze's file), so neither the lint through the module roots nor an operation's
  validation covers them; the direct `SchemaValidator` checks against `Problem` above (405, `REQUEST_REJECTED`) are
  their check.
- The inventory test also guards the module split: a controller serving an operation of the other module's file
  fails the build.

## Alternatives

- Spring Cloud Contract: consumer-driven stubs, more than a single-team API needs.
- Schemathesis: property-based and thorough, but needs a Python runtime in the build.
- Hand-written response assertions only: no check that the contract and code agree.
- Validating against one bundled file per module produced by `redocly bundle`: works (react-engineer spike,
  tool spikes of 2026-11-06 above), but adds a build step and a generated artefact, while the validator already resolves the
  cross-file `$ref`s.
