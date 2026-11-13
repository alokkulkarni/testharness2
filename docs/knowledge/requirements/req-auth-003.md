---
type: requirement
title: REQ-AUTH-003 Password storage
summary: Passwords are stored only as salted one-way hashes from a strong algorithm, and no password or hash appears in any response or log line.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-AUTH-003 Password storage (M)

Spec: [requirements.md, REQ-AUTH-003](../../specs/CHG-0001/requirements.md#req-auth-003-password-storage-m).

**Acceptance criteria in brief.** AC-1 stored value is a salted one-way hash (Argon2id), never plaintext or reversible;
AC-2 equal passwords give different hashes; AC-3 no password or hash in any API response or log line.

**Implemented by.** [identity module](../components/identity-module.md) (`PasswordHasher`), [users table](../data/users.md),
[shared kernel logging rules](../components/shared-kernel.md#logging-rules), [common problem model](../apis/common-problem-model.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`PasswordStorageTest`, `FilterExceptionTest`).

**Decisions.** [ADR-0005](../decisions/adr-0005-argon2id-password-hashing.md). Rules: [password rules](../rules/password-rules.md).
