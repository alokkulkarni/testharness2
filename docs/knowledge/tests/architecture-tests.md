---
type: test-suite
title: Architecture tests (ArchUnit)
summary: ArchUnit rules fail the build on a module dependency cycle, on use of unpublished module types, on DataSource creation outside the composition root, or on a main class outside the listed packages, and a non-vacuity fixture proves each rule can fail.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/test/java/app/todo/architecture/ArchitectureRules.java
  - api/src/test/java/app/todo/architecture/ArchitectureTest.java
  - api/src/test/java/app/todo/architecture/ArchitectureNonVacuityTest.java
  - api/pom.xml
---

# Architecture tests

Level: architecture ([ADR-0018](../decisions/adr-0018-modular-monolith-module-boundaries.md)), ArchUnit 1.4.1 (`api/pom.xml`).

- Rules in `ArchitectureRules.java`: no cycles between `auth`, `todo`, `common` and `config`; `todo` uses only `todo`, `common` and `auth.api`;
  `auth` and `common` never use `todo`; `common` uses no module and not `config`; only `config` creates a `DataSource` or `HikariConfig`;
  every main class is in a listed package.
- `ArchitectureNonVacuityTest` runs the same rules on a fixture root `archfixture` (in the test source set), each with one deliberate violation, and expects each to fail
  (the fixtures are under `api/src/test/java/archfixture/`).
- The test source set `app.todo.testsupport` and `app.todo.TestTodoApplication` are excluded.

Requirements: [NFR-007](../requirements/nfr-007.md) AC-2. Components checked: [identity module](../components/identity-module.md),
[to-dos module](../components/todos-module.md), [shared kernel](../components/shared-kernel.md), [composition root](../components/composition-root.md).
