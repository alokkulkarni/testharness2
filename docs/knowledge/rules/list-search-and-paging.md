---
type: rule
title: List filter, search and paging semantics
summary: The to-do list filters on done status, matches a literal lower-cased substring of title or description, orders newest first with a total order, pages 20 at a time, and clamps a page past the last to the last valid page.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/todo/TodoValidator.java
  - api/src/main/java/app/todo/todo/JdbcTodoRepository.java
  - docs/specs/CHG-0001/sdd.md
---

# List filter, search and paging

- **Filter**: `all` (default), `active` (`done = 0`), `completed` (`done = 1`). Other values are refused. The SPA removes an unknown `status` from the URL ([REQ-LIST-001](../requirements/req-list-001.md)).
- **Search**: `q` is trimmed; empty means no restriction. Otherwise it is lower-cased with `Locale.ROOT` and matched with `instr(...) > 0` against title and description, so `%`, `_`, `\` and quotes are literal. It is ANDed with the filter ([REQ-LIST-002](../requirements/req-list-002.md)).
- **Order**: `created_at DESC, id DESC`, a total order, so pages never repeat or skip ([REQ-TODO-003](../requirements/req-todo-003.md), [REQ-LIST-003](../requirements/req-list-003.md) AC-2).
- **Paging**: fixed size 20; `totalPages = ceil(totalItems / 20)`. The `page` value must match `^0*[1-9][0-9]*$`; zero, negative and non-numeric values are refused with 400 on `page`.
- **Past the last page**: the API clamps to `min(requested, totalPages)`, or to 1 when there are no matches, and returns 200 with the effective page. Values too large for a `long` count as past the last page ([REQ-LIST-003](../requirements/req-list-003.md) AC-6).
- **Totals**: `all`, `active` and `completed` counts ignore filter, search and page, and are read in the same transaction as the page.

Implemented in `TodoValidator.java` and `JdbcTodoRepository.java` ([to-dos module](../components/todos-module.md)); the SPA side is in [to-do list](../screens/todo-list.md).
