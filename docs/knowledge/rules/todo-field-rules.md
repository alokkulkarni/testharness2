---
type: rule
title: To-do field rules
summary: A to-do title is 1 to 200 characters after trimming, a description is at most 2000 characters, the due date is a required strict YYYY-MM-DD date, and priority is LOW, MEDIUM or HIGH with MEDIUM as the create default.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/todo/TodoValidator.java
  - web/src/validation/rules.ts
  - docs/specs/CHG-0001/sdd.md
---

# To-do field rules

| Field | Rule | Source |
|---|---|---|
| Title | trimmed; 1 to 200 code points; blank is refused | [REQ-TODO-002](../requirements/req-todo-002.md) AC-1 to AC-3 |
| Description | 2000 code points or fewer, as sent; absent, null or blank means NULL | REQ-TODO-001 AC-3, REQ-TODO-002 AC-4 |
| Due date | required; `YYYY-MM-DD` parsed with strict resolution (so `2026-02-30` fails with `INVALID_DATE`); year 0001 to 9999; past dates allowed; no time or zone | REQ-TODO-002 AC-5 to AC-9 |
| Priority | `LOW`, `MEDIUM` or `HIGH`, case-sensitive; defaults to `MEDIUM` on create when omitted or null; required on update | REQ-TODO-001 AC-2, REQ-TODO-002 AC-8 |
| Bodies | unknown property gives `UNKNOWN_FIELD`; wrong JSON type gives `INVALID_TYPE`; all field errors reported together | [sdd.md section 5](../../specs/CHG-0001/sdd.md#5-validation-server-authoritative-spa-mirrors-nfr-004-ac-2-adr-0010) |

"Characters" are Unicode code points on both sides. The SPA keeps the due date as a string from `<input type="date">`,
so the date does not shift in any time zone ([REQ-TODO-002](../requirements/req-todo-002.md) AC-7).

Implemented in `api/src/main/java/app/todo/todo/TodoValidator.java` and mirrored in `web/src/validation/rules.ts`
(see [ADR-0010](../decisions/adr-0010-explicit-validator-classes.md)). Stored in [todos](../data/todos.md).
