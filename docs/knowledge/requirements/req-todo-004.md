---
type: requirement
title: REQ-TODO-004 Edit a to-do
summary: A user edits title, description, due date and priority of their own to-do; valid saves persist and advance the updated time, invalid saves change nothing, and a missing to-do gives not-found.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-TODO-004 Edit a to-do (M)

Spec: [requirements.md, REQ-TODO-004](../../specs/CHG-0001/requirements.md#req-todo-004-edit-a-to-do-m).

**Acceptance criteria in brief.** AC-1 valid edit persists, `updated_at` advances, `created_at` unchanged; AC-2 invalid
edit leaves the stored to-do unchanged; AC-3 missing to-do gives not-found and creates nothing; AC-4 description can be
cleared, due date cannot.

**Implemented by.** [to-dos module](../components/todos-module.md) (`updateContent`, strictly advancing `updated_at`),
[to-do API](../apis/todo-api.md) `updateTodo`, screen [to-do form](../screens/todo-form.md) in edit mode.

**Rules.** [todo field rules](../rules/todo-field-rules.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`TodoUpdateApiTest`), [e2e tests](../tests/e2e-tests.md).

**Decisions.** [ADR-0006](../decisions/adr-0006-last-write-wins-whole-content-replace.md), [ADR-0013](../decisions/adr-0013-injected-clock-and-e2e-test-clock.md).
