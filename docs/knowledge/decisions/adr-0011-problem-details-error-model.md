---
type: decision
title: ADR-0011 Problem Details error model
summary: Every API error is an RFC 9457 application/problem+json body with a closed code enum, written by one component, so no stack trace or SQL reaches a client.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0011-problem-details-error-model.md
  - api/src/main/java/app/todo/common/ProblemWriter.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0011 Problem Details error model

Frozen record: [adr/0011-problem-details-error-model.md](../../specs/CHG-0001/adr/0011-problem-details-error-model.md).

**Applied in.** [common problem model](../apis/common-problem-model.md); `ProblemWriter` and `ProblemCode` in the
[shared kernel](../components/shared-kernel.md). The SPA maps codes to messages in `web/src/errors/userMessage.ts`
([NFR-006](../requirements/nfr-006.md)). Serves [NFR-004](../requirements/nfr-004.md) AC-2.
