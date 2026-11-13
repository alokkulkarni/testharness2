---
type: test-suite
title: End-to-end tests (Playwright)
summary: Playwright specs drive the real API and SPA through the main, restart and session-expiry configurations, covering the acceptance journey, auth, navigation, states, keyboard use, network locality and performance, on chromium and webkit.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/playwright.config.ts
  - web/e2e/playwright.session-expiry.config.ts
  - web/e2e/playwright.restart.config.ts
  - web/e2e/acceptance.spec.ts
  - web/e2e/auth.spec.ts
  - web/e2e/support/launch.ts
  - .harness/changes/CHG-0001/verdicts/N12-validate.validator.json
---

# End-to-end tests

Level: e2e (SDD section 14). Three configurations:

| Configuration | Starts | Specs |
|---|---|---|
| main (`web/playwright.config.ts`) | API and SPA with the section 11 commands, temporary database | `acceptance.spec.ts`, `auth.spec.ts`, `navigation.spec.ts`, `states.spec.ts`, `keyboard.spec.ts`, `network.spec.ts`, `performance.spec.ts`, `a11y.spec.ts` |
| restart (`playwright.restart.config.ts`) | API that can be restarted | `restart.spec.ts` ([REQ-ACC-001](../requirements/req-acc-001.md) AC-3, [REQ-DATA-001](../requirements/req-data-001.md)) |
| session-expiry (`playwright.session-expiry.config.ts`) | API under `spring-boot:test-run` with the test-only clock | `session-expiry.spec.ts` ([REQ-AUTH-002](../requirements/req-auth-002.md) AC-4, [REQ-AUTH-004](../requirements/req-auth-004.md) AC-4) |

Support: `web/e2e/support/` (`api-process.ts`, `app.ts`, `launch.ts`), `global-setup.ts`, `global-setup.session-expiry.ts`.

**Projects.** chromium and webkit are the required projects (`.harness/config.yaml`, `tests.requiredProjects`, lines 53-54). The Playwright
configs also list firefox, but it could not launch on the build host, so it is not required. The N10 plan
(`docs/specs/CHG-0001/plan/N10-build-react.md`, lines 83 and 110) attaches the three-browser full runs
(`N10-build-react.e2e-*-full.xml`), in which every Firefox case fails at launch, and records the chromium and webkit runs
(`*-r3.xml`) as the evidence of record. The Firefox divergence is listed in [N12 finding F-9](../../../.harness/changes/CHG-0001/verdicts/N12-validate.validator.json).

## Performance spec

`web/e2e/performance.spec.ts` seeds one user with 500 to-dos and runs list, filter, search, page, create, edit, complete and delete
20 times each, expecting p95 at most 1 second ([NFR-003](../requirements/nfr-003.md)).

Related: [web component tests](web-component-tests.md), [accessibility tests](accessibility-tests.md), [web SPA](../components/web-spa.md), [API](../apis/todo-api.md).
