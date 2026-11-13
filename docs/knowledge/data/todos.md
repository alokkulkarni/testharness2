---
type: data
title: todos table
summary: The todos table stores one row per to-do owned by one user, with a random public id, trimmed title and description, a required date-only due date, a priority, a done flag and UTC timestamps.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/resources/db/migration/V1__init.sql
  - docs/specs/CHG-0001/sdd.md
---

# `todos`

Owner: [to-dos module](../components/todos-module.md). Schema: [schema overview](schema.md).

| Column | Type | Rule |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | internal; tie-break for ordering |
| `public_id` | TEXT NOT NULL UNIQUE | random UUID v4, lower case; the API's `id` |
| `user_id` | INTEGER NOT NULL FK to `users` | owner, `ON DELETE CASCADE` |
| `title` | TEXT NOT NULL | trimmed, 1 to 200 code points ([todo-field-rules](../rules/todo-field-rules.md)) |
| `title_search` | TEXT NOT NULL | `title` lower-cased with `Locale.ROOT` |
| `description` | TEXT NULL | up to 2000 code points; NULL means no description |
| `description_search` | TEXT NULL | `description` lower-cased with `Locale.ROOT` |
| `due_date` | TEXT NOT NULL | `YYYY-MM-DD`, date only |
| `priority` | TEXT NOT NULL | CHECK `LOW`, `MEDIUM` or `HIGH` |
| `done` | INTEGER NOT NULL | CHECK 0 (Active) or 1 (Completed) |
| `created_at`, `updated_at` | TEXT NOT NULL | UTC, millisecond precision |

Indexes: `ix_todos_user_created (user_id, created_at DESC, id DESC)` and
`ix_todos_user_done_created (user_id, done, created_at DESC, id DESC)`, which serve the list order and the filters
(performance target: [NFR-003](../requirements/nfr-003.md)).

Related requirements: [REQ-TODO-001](../requirements/req-todo-001.md) to [REQ-TODO-007](../requirements/req-todo-007.md),
[REQ-DATA-001](../requirements/req-data-001.md).
