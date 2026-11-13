---
type: requirement
title: REQ-LIST-002 Search
summary: Search matches the text of titles and descriptions case-insensitively as a literal substring, combines with the filter, treats whitespace-only text as no search, and never errors on special characters.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-LIST-002 Search (M)

Spec: [requirements.md, REQ-LIST-002](../../specs/CHG-0001/requirements.md#req-list-002-search-m).

**Acceptance criteria in brief.** AC-1 title match, case-insensitive; AC-2 description match; AC-3 AND with filter;
AC-4 no-matches state; AC-5 empty search is no restriction; AC-6 `%`, `_` and quotes match literally; AC-7 whitespace-only is no search.

**Implemented by.** [to-dos module](../components/todos-module.md) (`instr(title_search, :q) > 0 OR instr(description_search, :q) > 0`),
[web SPA](../components/web-spa.md) `SearchForm`.

**Rules.** [list, search and paging](../rules/list-search-and-paging.md), [whitespace trimming](../rules/whitespace-trimming.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoListApiTest`), [web unit tests](../tests/web-unit-tests.md) (`whitespace.test.ts`),
[e2e tests](../tests/e2e-tests.md) (`acceptance.spec.ts`).

**Decisions.** [ADR-0015](../decisions/adr-0015-derived-lowercase-columns.md).
