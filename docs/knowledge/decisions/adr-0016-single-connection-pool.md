---
type: decision
title: ADR-0016 Single connection pool
summary: The application holds one SQLite connection in one Hikari pool, created only by the composition root, which serialises requests for a single local user.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0016-single-connection-pool.md
  - api/src/main/java/app/todo/config/DatabaseConfig.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0016 Single connection pool

Frozen record: [adr/0016-single-connection-pool.md](../../specs/CHG-0001/adr/0016-single-connection-pool.md).

**Applied in.** `DatabaseConfig` is the only place that creates a `DataSource` or `HikariConfig`
(`api/src/main/java/app/todo/config/DatabaseConfig.java`). The risk and its guard (NFR-003 test) are in
[sdd.md section 13](../../specs/CHG-0001/sdd.md#13-risks). Serves [NFR-007](../requirements/nfr-007.md) AC-1 and [NFR-003](../requirements/nfr-003.md).
See [composition root](../components/composition-root.md).
