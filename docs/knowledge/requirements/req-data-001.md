---
type: requirement
title: REQ-DATA-001 Persistence
summary: All accounts and to-dos, with their status, due date, priority and timestamps, survive an application restart in a SQLite file that is created on first start.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-DATA-001 Persistence (M)

Spec: [requirements.md, REQ-DATA-001](../../specs/CHG-0001/requirements.md#req-data-001-persistence-m).

**Acceptance criteria in brief.** AC-1 data unchanged after stop and start; AC-2 data held in a SQLite file; AC-3 the file and its
directory are created on first start with no manual setup.

**Implemented by.** [schema](../data/schema.md) (Flyway `V1__init.sql`), [composition root](../components/composition-root.md)
(`DatabaseConfig` creates the directory), [ADR-0003](../decisions/adr-0003-sqlite-persistence.md), [ADR-0014](../decisions/adr-0014-flyway-migrations.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`PersistenceTest`), [e2e tests](../tests/e2e-tests.md) (`restart.spec.ts`).
