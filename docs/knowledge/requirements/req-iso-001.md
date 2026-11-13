---
type: requirement
title: REQ-ISO-001 Per-user isolation
summary: A user sees, searches, changes and deletes only their own to-dos, and another user's identifier gets the same not-found as a missing one.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-ISO-001 Per-user isolation (M)

Spec: [requirements.md, REQ-ISO-001](../../specs/CHG-0001/requirements.md#req-iso-001-per-user-isolation-m).

**Acceptance criteria in brief.** AC-1 lists, filters, searches and pages return only the caller's to-dos; AC-2 another
user's to-do gives "not found" for read, update, toggle and delete; AC-3 clear-completed touches only the caller's to-dos;
AC-4 a search that matches only another user's data shows the empty result.

**Implemented by.** [to-dos module](../components/todos-module.md) (owner from `CurrentUser`, owner in every SQL statement),
[identity module](../components/identity-module.md) (the published seam), [to-do API](../apis/todo-api.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoIsolationApiTest`), [e2e tests](../tests/e2e-tests.md)
(`acceptance.spec.ts`).

**Decisions.** [ADR-0009](../decisions/adr-0009-spring-jdbc-without-jpa.md), [ADR-0018](../decisions/adr-0018-modular-monolith-module-boundaries.md).
