---
type: decision
title: ADR-0014 Flyway migrations
summary: The schema is created by Flyway at start-up from one versioned migration, V1__init.sql, which is the only place the tables are defined.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0014-flyway-migrations.md
  - api/src/main/resources/db/migration/V1__init.sql
  - api/pom.xml
---

# ADR-0014 Flyway migrations

Frozen record: [adr/0014-flyway-migrations.md](../../specs/CHG-0001/adr/0014-flyway-migrations.md).

**Applied in.** `spring-boot-starter-flyway` (`api/pom.xml`) and `api/src/main/resources/db/migration/V1__init.sql`.
See [schema overview](../data/schema.md). Serves [REQ-DATA-001](../requirements/req-data-001.md) AC-3 (the database is created
on first start with no manual setup).
