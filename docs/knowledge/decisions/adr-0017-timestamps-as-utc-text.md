---
type: decision
title: ADR-0017 Timestamps as UTC text
summary: Timestamps are stored as fixed-format UTC text with millisecond precision, so text order equals time order, and due dates are stored as date-only text with no time zone.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0017-timestamps-as-utc-text.md
  - api/src/main/java/app/todo/common/UtcTimestamps.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0017 Timestamps as UTC text

Frozen record: [adr/0017-timestamps-as-utc-text.md](../../specs/CHG-0001/adr/0017-timestamps-as-utc-text.md).

**Applied in.** `UtcTimestamps` (`api/src/main/java/app/todo/common/UtcTimestamps.java`); `created_at` and `updated_at`
columns in [users](../data/users.md), [sessions](../data/sessions.md) and [todos](../data/todos.md). Due dates are
`YYYY-MM-DD` text, parsed strictly ([todo-field-rules](../rules/todo-field-rules.md)). Serves [REQ-TODO-004](../requirements/req-todo-004.md) and [REQ-TODO-002](../requirements/req-todo-002.md) AC-7.
