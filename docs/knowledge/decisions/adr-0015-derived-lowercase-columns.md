---
type: decision
title: ADR-0015 Derived lower-case columns for case-insensitive matching
summary: Lower-cased copies of identifiers, titles and descriptions are stored beside the originals, so uniqueness and search are case-insensitive with a plain literal substring test.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0015-derived-lowercase-columns.md
  - api/src/main/resources/db/migration/V1__init.sql
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0015 Derived lower-case columns

Frozen record: [adr/0015-derived-lowercase-columns.md](../../specs/CHG-0001/adr/0015-derived-lowercase-columns.md).

**Applied in.** `users.identifier_key` (UNIQUE) and `todos.title_search` / `todos.description_search`
([users](../data/users.md), [todos](../data/todos.md)). The search is `instr(...) > 0`, which treats `%`, `_` and quotes literally
([list-search-and-paging](../rules/list-search-and-paging.md)). Lower-casing uses `Locale.ROOT`; full case folding is not
supported (sdd section 13). Serves [REQ-AUTH-001](../requirements/req-auth-001.md) AC-6 and [REQ-LIST-002](../requirements/req-list-002.md).
