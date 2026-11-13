---
type: requirement
title: REQ-TODO-002 To-do field rules and validation
summary: Titles are trimmed to 1 to 200 characters, descriptions are at most 2000, the due date is a required real calendar date, and priority is Low, Medium or High, with the server refusing anything else.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-TODO-002 To-do field rules and validation (M)

Spec: [requirements.md, REQ-TODO-002](../../specs/CHG-0001/requirements.md#req-todo-002-to-do-field-rules-and-validation-m).

**Acceptance criteria in brief.** AC-1 trim the title; AC-2 blank title refused; AC-3 title 200 accepted, 201 refused;
AC-4 description 2000 accepted, 2001 refused; AC-5 due date required; AC-6 invalid calendar date refused; AC-7 date only,
same calendar date in any time zone; AC-8 priority is Low, Medium or High; AC-9 past dates accepted; AC-10 each refusal names its field.

**Implemented by.** [to-dos module](../components/todos-module.md) (`TodoValidator`), [web SPA](../components/web-spa.md) (`validation/rules.ts`),
[common problem model](../apis/common-problem-model.md) (`VALIDATION_FAILED` with `errors[]`).

**Rules.** [todo field rules](../rules/todo-field-rules.md), [whitespace trimming](../rules/whitespace-trimming.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoValidationApiTest`), [web unit tests](../tests/web-unit-tests.md),
[web component tests](../tests/web-component-tests.md) (`todo-form.test.tsx`).

**Decisions.** [ADR-0010](../decisions/adr-0010-explicit-validator-classes.md), [ADR-0017](../decisions/adr-0017-timestamps-as-utc-text.md).
