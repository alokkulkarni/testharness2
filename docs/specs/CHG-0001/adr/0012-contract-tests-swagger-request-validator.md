# ADR-0012 Contract tests with swagger-request-validator-mockmvc and an endpoint inventory

Status: proposed (CHG-0001, N03-spec). Requirements: NFR-004, REQ-AUTH-005 AC-1, and every requirement whose
interface is in `contracts/todo-api-v1.yaml`. Split out of ADR-0002 (critic finding F-6, N03-spec round 1).

## Context

`contracts/todo-api-v1.yaml` is the source of truth for the API. Drift between it and the implementation must fail
the build, and no operation may exist that the contract does not list (REQ-AUTH-005 AC-1: no user-listing or
cross-user operation).

## Decision

- Add the test-only dependency **com.atlassian.oai:swagger-request-validator-mockmvc** (Apache-2.0, maintained),
  pinned by the java-engineer. Every MockMvc integration request and response is validated against the contract.
- An endpoint-inventory test compares the routes Spring MVC has mapped (default profile) with the contract's
  path-and-method pairs; any difference fails the build.
- Error responses that have no contract operation (405, unknown route) are checked against the contract's `Problem`
  schema directly (ADR-0011).

## Consequences

- The contract is exercised by the same tests that check behaviour; no separate contract suite to maintain.
- The validator supports OpenAPI 3.1 only as far as its current release does; constructs the validator cannot read
  are kept out of the contract (the contract uses plain 3.1 schemas).

## Alternatives

- Spring Cloud Contract: consumer-driven stubs, more than a single-team API needs.
- Schemathesis: property-based and thorough, but needs a Python runtime in the build.
- Hand-written response assertions only: no check that the contract and code agree.
