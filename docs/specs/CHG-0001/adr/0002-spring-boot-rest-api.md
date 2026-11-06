# ADR-0002 Spring Boot REST API on Java 21

Status: proposed (CHG-0001, N03-spec). Requirements: NFR-005, NFR-002; constraint C-1. In round 2 the persistence,
validation, error-model, contract-test and clock decisions were split out into ADR-0009 to ADR-0013.

## Context

The requester fixed the API as Java/Spring Boot (Q3, C-1). The repository has no API code, so the Java version,
build tool, module layout and starters are open. The API runs locally next to the SPA (NFR-002).

## Decision

- A single Spring Boot application in `api/` (one Maven module, with the Maven wrapper committed), **Java 21 LTS**,
  on the current OSS-supported Spring Boot major line; the java-engineer pins the exact version in `api/pom.xml`.
- Servlet stack (Spring MVC on embedded Tomcat), not the reactive stack.
- Starters: `spring-boot-starter-web` (REST, Jackson), `spring-boot-starter-jdbc` (ADR-0009),
  `spring-boot-starter-security` (filter chain, security headers, `Argon2PasswordEncoder`, see ADR-0005),
  `spring-boot-starter-test`.
- Root package `app.todo`, packages by feature (`auth`, `user`, `todo`) plus `common` and `config` (sdd.md §2).
- Static resource handling is off (`spring.web.resources.add-mappings=false`): the API serves no files.

## Consequences

- `./mvnw spring-boot:run` is the only command a developer needs for the API (NFR-002).
- One module keeps the build simple; the package boundaries carry the structure.
- Blocking I/O with one SQLite connection (ADR-0016) is simple and ample for a local single user.

## Alternatives

- Spring WebFlux: non-blocking I/O brings nothing with a blocking JDBC driver and one local user.
- Multi-module Maven build (api, domain, persistence): more structure than three tables need.
- Gradle: equally capable; Maven was chosen for the plainer, declarative build file.
