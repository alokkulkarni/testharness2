# ADR-0002 Spring Boot REST API with Spring JDBC and RFC 9457 errors

Status: proposed (CHG-0001, N03-spec). Requirements: NFR-005, NFR-004 AC-2, REQ-TODO-002 AC-10; constraint C-1.

## Context

The requester fixed the API as Java/Spring Boot (Q3, C-1) and storage as SQLite (Q13). Hibernate has no
first-party SQLite dialect (only the community dialects module), and the data model is three small tables.
Errors must name the offending field (REQ-TODO-002 AC-10) and never expose stack traces or SQL (NFR-004 AC-2).

## Decision

- A single Spring Boot application in `api/` (Maven, with the Maven wrapper committed), **Java 21 LTS**, on the
  current OSS-supported Spring Boot major line; the java-engineer pins the exact version in `api/pom.xml`.
- Starters: `spring-boot-starter-web` (REST, Jackson), `spring-boot-starter-jdbc` (Spring `JdbcClient`, HikariCP),
  `spring-boot-starter-security` (filter chain, security headers, `Argon2PasswordEncoder`, see ADR-0005),
  `spring-boot-starter-test`.
- Persistence through hand-written SQL in repository classes using `JdbcClient` with bound parameters only; **no
  JPA/Hibernate**.
- Validation in explicit validator classes (`TodoValidator`, `IdentifierRules`, `PasswordRules`) rather than Bean
  Validation annotations, because the rules trim before measuring, count code points and must report one error per
  field in a fixed format. Request bodies are bound to records with Jackson `FAIL_ON_UNKNOWN_PROPERTIES` on.
- One `@RestControllerAdvice` maps every exception to an `application/problem+json` body per the contract's `Problem`
  schema; unexpected exceptions become a generic 500 and are logged server-side only.
- A `java.time.Clock` bean is injected wherever time is read, so tests control time.
- Contract tests use **swagger-request-validator-mockmvc** (Atlassian, Apache-2.0) to check every MockMvc response
  against `contracts/todo-api-v1.yaml`.

## Consequences

- SQL is visible and testable; the owner filter is written into every statement (sdd.md "Isolation").
- No ORM conveniences (dirty checking, lazy loading); acceptable for three tables.
- Custom validators need their own unit tests (planned in the verification table).

## Alternatives

- Spring Data JPA + Hibernate community SQLite dialect: more moving parts, dialect maintained outside Hibernate core.
- jOOQ: type-safe SQL, but code generation and a further dependency for a small schema.
- Bean Validation annotations: cannot express trim-then-length in code points or the email/username split without
  custom constraints, which would be as much code as plain validators.
