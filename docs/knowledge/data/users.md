---
type: data
title: users table
summary: The users table holds one row per account, with the trimmed identifier in its original case, a lower-cased unique key and an Argon2id password hash, and no role column.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/resources/db/migration/V1__init.sql
  - docs/specs/CHG-0001/sdd.md
---

# `users`

Owner: [identity module](../components/identity-module.md). Schema: [schema overview](schema.md).

| Column | Type | Rule |
|---|---|---|
| `id` | INTEGER PK | internal only, never returned |
| `identifier` | TEXT NOT NULL | trimmed, original case; shown back as `User.identifier` |
| `identifier_key` | TEXT NOT NULL UNIQUE | `identifier` lower-cased with `Locale.ROOT`; enforces case-insensitive uniqueness ([REQ-AUTH-001](../requirements/req-auth-001.md) AC-6) |
| `password_hash` | TEXT NOT NULL | Argon2id encoded string ([ADR-0005](../decisions/adr-0005-argon2id-password-hashing.md)) |
| `created_at` | TEXT NOT NULL | UTC timestamp |

No role or permission column: every account has the same permissions ([REQ-AUTH-005](../requirements/req-auth-005.md) AC-2).

Related: [password rules](../rules/password-rules.md), [identifier rules](../rules/identifier-rules.md),
[REQ-AUTH-003](../requirements/req-auth-003.md) (no password or hash in storage in plain form, and no hash in any response).
