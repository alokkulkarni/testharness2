---
type: decision
title: ADR-0005 Argon2id password hashing
summary: Passwords are stored only as salted Argon2id hashes, computed over the whole password, with no pre-hash and no 72-byte limit.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0005-argon2id-password-hashing.md
  - api/src/main/java/app/todo/auth/PasswordHasher.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0005 Argon2id password hashing

Frozen record: [adr/0005-argon2id-password-hashing.md](../../specs/CHG-0001/adr/0005-argon2id-password-hashing.md).

**Applied in.** `PasswordHasher` (`api/src/main/java/app/todo/auth/PasswordHasher.java`) with BouncyCastle as the Argon2
provider ([api/pom.xml](../../../api/pom.xml), `bcprov-jdk18on` 1.85). Parameters in [sdd.md section 12](../../specs/CHG-0001/sdd.md#12-design-decisions-settling-the-requirements-critics-findings):
m=19 MiB, t=2, p=1. Serves [REQ-AUTH-003](../requirements/req-auth-003.md) and [password rules](../rules/password-rules.md).
