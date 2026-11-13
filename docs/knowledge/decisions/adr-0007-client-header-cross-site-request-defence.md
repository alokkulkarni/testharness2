---
type: decision
title: ADR-0007 X-Todo-Client header as cross-site request defence
summary: Unsafe API methods must carry an X-Todo-Client header, which a cross-site form cannot send, and ClientHeaderFilter refuses the request with 403 otherwise.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0007-client-header-cross-site-request-defence.md
  - api/src/main/java/app/todo/common/ClientHeaderFilter.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0007 `X-Todo-Client` header as cross-site request defence

Frozen record: [adr/0007-client-header-cross-site-request-defence.md](../../specs/CHG-0001/adr/0007-client-header-cross-site-request-defence.md).

**Applied in.** `ClientHeaderFilter` in the [shared kernel](../components/shared-kernel.md) returns 403
`CLIENT_HEADER_MISSING` ([common problem model](../apis/common-problem-model.md)). The SPA sends `X-Todo-Client: web` on
unsafe methods (`web/src/api/client.ts`). Order of checks: session, then header, then route ([sdd.md section 4](../../specs/CHG-0001/sdd.md#4-auth-and-session-adr-0004-adr-0005-adr-0007-adr-0008)).
