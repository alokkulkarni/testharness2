---
type: decision
title: ADR-0003 SQLite persistence
summary: All accounts, sessions and to-dos are stored in one embedded SQLite file whose path comes from TODO_DB_PATH.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0003-sqlite-persistence.md
  - api/pom.xml
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0003 SQLite persistence

Frozen record: [adr/0003-sqlite-persistence.md](../../specs/CHG-0001/adr/0003-sqlite-persistence.md).

**Applied in.** The `sqlite-jdbc` driver (`api/pom.xml`, version 3.50.3.0). The file is `TODO_DB_PATH`, default
`./data/todo.db` ([sdd.md section 11](../../specs/CHG-0001/sdd.md#11-local-run-nfr-002-req-data-001-ac-3)).
See [schema](../data/schema.md). Serves [REQ-DATA-001](../requirements/req-data-001.md) and [NFR-005](../requirements/nfr-005.md).
