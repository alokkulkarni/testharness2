---
type: decision
title: ADR-0018 Modular monolith with module boundaries
summary: The API is one deployable split into an identity module, a to-dos module, a shared kernel and a composition root, with the boundaries checked by ArchUnit and a contract inventory test.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0018-modular-monolith-module-boundaries.md
  - api/src/test/java/app/todo/architecture/ArchitectureRules.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0018 Modular monolith with module boundaries

Frozen record: [adr/0018-modular-monolith-module-boundaries.md](../../specs/CHG-0001/adr/0018-modular-monolith-module-boundaries.md).

**Applied in.** Packages `app.todo.auth` (identity, publishes `app.todo.auth.api.CurrentUser`), `app.todo.todo` (to-dos),
`app.todo.common` (shared kernel) and `app.todo.config` (composition root). Rules are enforced by
`api/src/test/java/app/todo/architecture/ArchitectureRules.java` and `ArchitectureTest.java`, with a non-vacuity
fixture (`ArchitectureNonVacuityTest.java`). See [identity module](../components/identity-module.md),
[to-dos module](../components/todos-module.md), [shared kernel](../components/shared-kernel.md),
[composition root](../components/composition-root.md), [architecture tests](../tests/architecture-tests.md).
Serves [NFR-007](../requirements/nfr-007.md).
