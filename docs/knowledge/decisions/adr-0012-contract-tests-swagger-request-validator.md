---
type: decision
title: ADR-0012 Contract tests with the OpenAPI request validator
summary: Every integration request and response is checked against the frozen OpenAPI files with the Atlassian openapi-request-validator v3 line, while cookie security is tested at the application level.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0012-contract-tests-swagger-request-validator.md
  - api/pom.xml
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0012 Contract tests with the OpenAPI request validator

Frozen record: [adr/0012-contract-tests-swagger-request-validator.md](../../specs/CHG-0001/adr/0012-contract-tests-swagger-request-validator.md).

**Naming note.** The ADR filename says "swagger-request-validator". The build uses the renamed library at version 3.0.0:
`com.atlassian.oai:openapi-request-validator-core` and `openapi-request-validator-mockmvc`
(`api/pom.xml`, lines 77-89), as [sdd.md section 12.1 item 21](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)
and the section 13 validator-version risk at sdd.md line 768 record.

**Known limit.** The validator does not enforce the cookie `apiKey` scheme, so 401 is tested at the integration level
(sdd.md line 769, the section 13 cookie-scheme risk). See [contract tests](../tests/contract-tests.md).
