---
type: requirement
title: REQ-ACC-001 Overall success scenario
summary: The end-to-end scenario covers add, see, search, filter, edit, complete and delete for one user, isolation from a second user, and persistence across an API restart.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-ACC-001 Overall success scenario (M)

Spec: [requirements.md, REQ-ACC-001](../../specs/CHG-0001/requirements.md#req-acc-001-overall-success-scenario-m).

**Acceptance criteria in brief.** AC-1 the full user journey works; AC-2 a second user cannot see or change the first user's
to-dos; AC-3 to-dos survive an API restart.

**Verified by.** [e2e tests](../tests/e2e-tests.md): `acceptance.spec.ts` (AC-1, AC-2) and `restart.spec.ts`
with `playwright.restart.config.ts` (AC-3, which restarts the API process). At API level, [API integration tests](../tests/api-integration-tests.md):
`AcceptanceScenarioApiTest` (AC-1), `TodoIsolationApiTest` (AC-2) and `PersistenceTest` (persistence across a restart, REQ-DATA-001).

**Built from.** [web SPA](../components/web-spa.md), [identity module](../components/identity-module.md), [to-dos module](../components/todos-module.md),
[requirements](req-todo-001.md) through [REQ-LIST-003](req-list-003.md).
