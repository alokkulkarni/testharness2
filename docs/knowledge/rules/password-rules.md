---
type: rule
title: Password rules
summary: A password is 8 to 128 code points, is not trimmed or normalised, and is stored only as an Argon2id hash of the whole password.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/auth/PasswordRules.java
  - api/src/main/java/app/todo/auth/PasswordHasher.java
  - docs/specs/CHG-0001/sdd.md
---

# Password rules

- **Length**: 8 to 128 characters, counted as Unicode code points ([sdd.md section 5](../../specs/CHG-0001/sdd.md#5-validation-server-authoritative-spa-mirrors-nfr-004-ac-2-adr-0010)).
  The minimum 8 comes from the requester (Q33); the maximum 128 is an analyst default ([REQ-AUTH-001](../requirements/req-auth-001.md) AC-5, AC-11).
- **Not trimmed**: spaces at either end count.
- **Login**: a password over 128 code points gets the same 401 without hashing; no other rule applies at login.
- **Storage**: Argon2id with the whole password hashed, no pre-hash, so the 72-byte limit of bcrypt does not apply
  ([ADR-0005](../decisions/adr-0005-argon2id-password-hashing.md); [REQ-AUTH-003](../requirements/req-auth-003.md)).

Implemented in `api/src/main/java/app/todo/auth/PasswordRules.java` and `PasswordHasher.java`, stored in [users](../data/users.md).
