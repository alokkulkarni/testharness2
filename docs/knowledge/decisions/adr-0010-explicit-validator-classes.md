---
type: decision
title: ADR-0010 Explicit validator classes
summary: Input rules live in plain validator classes (for example TodoValidator and IdentifierRules) on the server, with mirrored rules in the SPA, rather than in framework annotations.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0010-explicit-validator-classes.md
  - api/src/main/java/app/todo/todo/TodoValidator.java
  - api/src/main/java/app/todo/auth/IdentifierRules.java
  - api/src/main/java/app/todo/auth/PasswordRules.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0010 Explicit validator classes

Frozen record: [adr/0010-explicit-validator-classes.md](../../specs/CHG-0001/adr/0010-explicit-validator-classes.md).

**Applied in.** Server rules: `TodoValidator` (`api/src/main/java/app/todo/todo/TodoValidator.java`), `IdentifierRules`
and `PasswordRules` (`api/src/main/java/app/todo/auth/`). SPA mirror: `web/src/validation/rules.ts`. The server is
authoritative ([sdd.md section 5](../../specs/CHG-0001/sdd.md#5-validation-server-authoritative-spa-mirrors-nfr-004-ac-2-adr-0010)).
See [rules](../rules/index.md) for the rules themselves.
