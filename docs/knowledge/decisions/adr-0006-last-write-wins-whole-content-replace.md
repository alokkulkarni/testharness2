---
type: decision
title: ADR-0006 Last write wins with whole-content replace
summary: To-do edits have no version check; a content update replaces every content field, and the done toggle is a separate operation that sets only done.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0006-last-write-wins-whole-content-replace.md
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0006 Last write wins with whole-content replace

Frozen record: [adr/0006-last-write-wins-whole-content-replace.md](../../specs/CHG-0001/adr/0006-last-write-wins-whole-content-replace.md).

**Applied in.** `updateTodo` (`PUT /api/v1/todos/{todoId}`) and `setTodoDone` in the [to-dos module](../components/todos-module.md);
the SPA sends the whole form. Serves [REQ-DATA-002](../requirements/req-data-002.md) (no conflict error; edits to different
to-dos are independent) and [REQ-TODO-004](../requirements/req-todo-004.md).
