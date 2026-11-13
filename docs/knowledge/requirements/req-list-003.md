---
type: requirement
title: REQ-LIST-003 Pagination
summary: The list shows 20 to-dos per page in a stable newest-first order, with Previous and Next controls, a page past the last clamped to the last valid page, and a 400 for zero, negative or non-numeric pages.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-LIST-003 Pagination (M)

Spec: [requirements.md, REQ-LIST-003](../../specs/CHG-0001/requirements.md#req-list-003-pagination-m).

**Acceptance criteria in brief.** AC-1 20 per page with numbered links; AC-2 no repeats or gaps across pages; AC-3 Previous
and Next disabled at the ends; AC-4 one page with no Next for 20 or fewer; AC-5 filter or search change returns to page 1;
AC-6 page past the last shows the last valid page; AC-7 zero, negative or non-numeric page refused (400); AC-8 fixed page size 20.

**Implemented by.** [to-dos module](../components/todos-module.md) (`JdbcTodoRepository.list`, page clamp in `TodoValidator`/`TodoService`),
[to-do API](../apis/todo-api.md) `listTodos`, [web SPA](../components/web-spa.md) `Pagination`, screen [to-do list](../screens/todo-list.md).

**Rules.** [list, search and paging](../rules/list-search-and-paging.md), [return-to and not-found](../rules/return-to-and-not-found.md) (URL page normalisation).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoListApiTest`), [web component tests](../tests/web-component-tests.md) (`list.test.tsx`),
[e2e tests](../tests/e2e-tests.md) (`states.spec.ts`, `acceptance.spec.ts`), [performance](../tests/e2e-tests.md#performance-spec).
