---
type: requirement
title: REQ-DATA-002 Concurrent edits, last write wins
summary: Two edits to the same to-do are both accepted with the later write stored and no conflict error, and edits to different to-dos are stored independently.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-DATA-002 Concurrent edits: last write wins (M)

Spec: [requirements.md, REQ-DATA-002](../../specs/CHG-0001/requirements.md#req-data-002-concurrent-edits-last-write-wins-m).

**Acceptance criteria in brief.** AC-1 later write stored, no conflict error; AC-2 edits to different to-dos do not lose data.

**Implemented by.** [to-dos module](../components/todos-module.md) (whole-content `updateContent`, separate `updateDone`),
[ADR-0006](../decisions/adr-0006-last-write-wins-whole-content-replace.md), [ADR-0016](../decisions/adr-0016-single-connection-pool.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`ConcurrentEditsApiTest`).
