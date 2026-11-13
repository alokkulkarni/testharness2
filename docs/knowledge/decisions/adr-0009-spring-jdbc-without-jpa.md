---
type: decision
title: ADR-0009 Spring JDBC without JPA
summary: Data access uses Spring JdbcClient with package-private repositories, one per module, and no JPA, so every SQL statement is written and filtered by owner explicitly.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0009-spring-jdbc-without-jpa.md
  - api/src/main/java/app/todo/todo/JdbcTodoRepository.java
  - api/src/main/java/app/todo/auth/JdbcUserRepository.java
  - api/pom.xml
---

# ADR-0009 Spring JDBC without JPA

Frozen record: [adr/0009-spring-jdbc-without-jpa.md](../../specs/CHG-0001/adr/0009-spring-jdbc-without-jpa.md).

**Applied in.** `spring-boot-starter-jdbc` only, with no JPA starter in `api/pom.xml`. Repositories: `JdbcTodoRepository`
(`api/src/main/java/app/todo/todo/JdbcTodoRepository.java`), `JdbcUserRepository` and `JdbcSessionRepository`
(`api/src/main/java/app/todo/auth/`). Each statement filters on `user_id`, which is the basis of [REQ-ISO-001](../requirements/req-iso-001.md).
See [to-dos module](../components/todos-module.md).
