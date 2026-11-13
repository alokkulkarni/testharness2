---
type: test-suite
title: Contract tests (OpenAPI request validator)
summary: Integration requests and responses are validated against the frozen identity and to-do OpenAPI files, and an endpoint inventory test checks that every mapped route is in exactly one module contract.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/test/java/app/todo/testsupport/ContractFilesTest.java
  - api/src/test/java/app/todo/config/EndpointInventoryTest.java
  - api/src/test/java/app/todo/testsupport/Contracts.java
  - api/pom.xml
  - docs/specs/CHG-0001/adr/0012-contract-tests-swagger-request-validator.md
---

# Contract tests

Level: contract (SDD section 14, [ADR-0012](../decisions/adr-0012-contract-tests-swagger-request-validator.md)).

- Every integration request and response is validated by `openapi-request-validator` 3.0.0 against the module file that defines its path
  (`identity-api-v1.yaml` or `todo-api-v1.yaml`); each resolves [common-problem-v1.yaml](../apis/common-problem-model.md) through `$ref`.
- Error bodies outside any operation (405, `REQUEST_REJECTED`) are checked against `Problem` directly.
- `EndpointInventoryTest` requires the union of the two module files to equal Spring's mapped routes, excluding `/error`, with each path in exactly one file
  and its controller in that file's module package. This is the test behind [REQ-AUTH-005](../requirements/req-auth-005.md) AC-1 and [NFR-007](../requirements/nfr-007.md) AC-3.
- `ContractFilesTest` checks the frozen contract files themselves.

Known limit: the validator does not enforce the cookie security scheme, so 401 is covered by the integration tests. The `listTodos` `q` whitelist is a recorded finding ([overview](../system/overview.md#known-divergences-as-recorded-at-n12-and-n13)).

Contracts: [identity API](../apis/identity-api.md), [to-do API](../apis/todo-api.md). Also the `lint:contracts` script (`redocly lint` on the module roots) runs in the gates; see [web SPA](../components/web-spa.md).
