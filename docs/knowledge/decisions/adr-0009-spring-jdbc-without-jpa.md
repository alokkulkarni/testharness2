---
type: decision
title: ADR-0009 Spring JDBC without JPA
summary: Data access uses hand-written SQL through Spring JdbcClient with no JPA and package-private JDBC repositories, and every to-do statement is filtered by the owner's user id.
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

**Applied in.** `spring-boot-starter-jdbc` only, with no JPA starter in `api/pom.xml`. The repositories are package-private
classes: `JdbcTodoRepository` (`api/src/main/java/app/todo/todo/JdbcTodoRepository.java:L19`), `JdbcUserRepository`
(`api/src/main/java/app/todo/auth/JdbcUserRepository.java:L12`) and `JdbcSessionRepository` (`api/src/main/java/app/todo/auth/JdbcSessionRepository.java:L11`).

**Owner filter.** Every `TodoRepository` statement carries the owner's `user_id`, as the ADR requires (`JdbcTodoRepository.java:L46`,
`L55`, `L89`, `L104-L109`, `L126`, `L143`, `L154`, `L162`). The identity repositories do not filter on `user_id`: users are looked up
by `identifier_key` (`JdbcUserRepository.java:L38`) and sessions by `token_hash` (`JdbcSessionRepository.java:L31`). The owner filter
is therefore a to-dos rule, and it is the basis of [REQ-ISO-001](../requirements/req-iso-001.md). See [to-dos module](../components/todos-module.md).
