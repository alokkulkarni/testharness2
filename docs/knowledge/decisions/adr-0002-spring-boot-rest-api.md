---
type: decision
title: ADR-0002 Spring Boot REST API
summary: The HTTP API is a Java 21 Spring Boot application, pinned to the Spring Boot 4.1.2 release in api/pom.xml.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0002-spring-boot-rest-api.md
  - api/pom.xml
---

# ADR-0002 Spring Boot REST API

Frozen record: [adr/0002-spring-boot-rest-api.md](../../specs/CHG-0001/adr/0002-spring-boot-rest-api.md).

**Applied in.** One Maven module with Spring Boot parent `4.1.2` and Java 21 (`api/pom.xml`, lines 11-24). The version
pin follows [sdd.md section 12.1 item 21](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)
(clarifications Q60). Serves [NFR-005](../requirements/nfr-005.md) and [NFR-007](../requirements/nfr-007.md) AC-1.
