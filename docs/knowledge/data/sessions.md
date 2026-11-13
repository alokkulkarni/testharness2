---
type: data
title: sessions table
summary: The sessions table keeps one row per login, keyed by the SHA-256 hash of the cookie token, with an absolute expiry time and a cascading foreign key to users.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/resources/db/migration/V1__init.sql
  - docs/specs/CHG-0001/sdd.md
---

# `sessions`

Owner: [identity module](../components/identity-module.md). Schema: [schema overview](schema.md).

| Column | Type | Rule |
|---|---|---|
| `token_hash` | TEXT PK | SHA-256 hex of the cookie token; the token itself is never stored |
| `user_id` | INTEGER NOT NULL | FK to `users` `ON DELETE CASCADE` |
| `created_at` | TEXT NOT NULL | UTC timestamp |
| `expires_at` | TEXT NOT NULL | 8 hours after login, absolute ([session lifetime](../rules/session-lifetime.md)) |

Index: `ix_sessions_expires_at (expires_at)`.

Each request hashes the cookie, loads the row, and treats a missing or expired row as unauthenticated; an expired row is
deleted ([sdd.md section 4](../../specs/CHG-0001/sdd.md#4-auth-and-session-adr-0004-adr-0005-adr-0007-adr-0008)).
Requirements: [REQ-AUTH-002](../requirements/req-auth-002.md) AC-4, [REQ-AUTH-004](../requirements/req-auth-004.md) AC-4.
