# ADR-0002 Spring Boot REST API on Java 21

Status: proposed (CHG-0001, N03-spec); amended by ADR-0018 (supersede-2: module packages and enforced boundaries) and for
Q60 (supersede-2: the Spring Boot line).
Requirements: NFR-005, NFR-002, NFR-007; constraints C-1, C-2. In round 2 the persistence, validation, error-model,
contract-test and clock decisions were split out into ADR-0009 to ADR-0013.

## Context

The requester fixed the API as Java/Spring Boot (Q3, C-1) and chose the latest Spring Boot 4.x release (Q60). The
repository has no API code, so the Java version, build tool, module layout and starters are open. The API runs locally next to the SPA (NFR-002). The requester chose
a modular monolith, not separate services (Q51, C-2, NFR-007).

## Decision

- A single Spring Boot application in `api/` (one Maven module, with the Maven wrapper committed), **Java 21 LTS**,
  on the latest Spring Boot 4.x release, pinned exactly in `api/pom.xml` by the java-engineer (Source: clarifications
  Q60; the contract validator's v3 adapter needs Spring Boot 4+, ADR-0012).
- Servlet stack (Spring MVC on embedded Tomcat), not the reactive stack.
- Starters: `spring-boot-starter-web` (REST, Jackson), `spring-boot-starter-jdbc` (ADR-0009),
  `spring-boot-starter-security` (filter chain, security headers, `Argon2PasswordEncoder`, see ADR-0005),
  `spring-boot-starter-test`.
- Root package `app.todo`, split into modules by the functionality they serve (ADR-0018, sdd.md §2): `auth` (the
  identity module), `todo` (the to-dos module), the shared kernel `common`, and the composition root `config`.
- Static resource handling is off (`spring.web.resources.add-mappings=false`): the API serves no files.

## Consequences

- `./mvnw spring-boot:run` is the only command a developer needs for the API (NFR-002).
- One Maven module keeps the build simple; the module boundaries are packages, enforced by an ArchUnit test in the
  build (ADR-0018, NFR-007 AC-2).
- Blocking I/O with one SQLite connection (ADR-0016) is simple and ample for a local single user.

## Alternatives

- Spring WebFlux: non-blocking I/O brings nothing with a blocking JDBC driver and one local user.
- Multi-module Maven build (api, domain, persistence): more structure than three tables need; ADR-0018 also weighs a
  Maven module per API module and declines it.
- Gradle: equally capable; Maven was chosen for the plainer, declarative build file.
