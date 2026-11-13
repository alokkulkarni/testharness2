---
type: test-suite
title: Web unit tests (Vitest)
summary: Vitest unit tests check the whitespace table, the user-message mapping for every failure, and the routing helpers, in web/tests/unit.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/tests/unit/whitespace.test.ts
  - web/tests/unit/userMessage.test.ts
  - web/tests/unit/routing-helpers.test.ts
  - web/vitest.config.ts
---

# Web unit tests

Level: unit (Vitest), files in `web/tests/unit/`:

- `whitespace.test.ts`: the same 25-code-point table as the API `WhitespaceTest` ([whitespace trimming](../rules/whitespace-trimming.md); [REQ-AUTH-001](../requirements/req-auth-001.md) AC-8).
- `userMessage.test.ts`: `userMessageFor` for each failure row, with no internals in any message ([NFR-006](../requirements/nfr-006.md)).
- `routing-helpers.test.ts`: `safeReturnTo`, `isTodoId` and route helpers ([return-to rule](../rules/return-to-and-not-found.md); [REQ-NAV-001](../requirements/req-nav-001.md)).

Component-level tests are in [web component tests](web-component-tests.md). Code under test: [web SPA](../components/web-spa.md).
