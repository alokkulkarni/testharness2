---
type: test-suite
title: Accessibility tests (axe and keyboard scripts)
summary: axe checks against WCAG 2.1 A and AA rules, keyboard-only Playwright scripts, and component-level focus and error-association checks cover the screens listed in NFR-001 and the not-found page.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/e2e/a11y.spec.ts
  - web/e2e/keyboard.spec.ts
  - web/tests/component/a11y.test.tsx
---

# Accessibility tests

Covers [NFR-001](../requirements/nfr-001.md) and [REQ-NAV-001](../requirements/req-nav-001.md) AC-7, and the automated part of AC-6.

- **axe (WCAG 2.1 A/AA)**: `web/e2e/a11y.spec.ts` (`@axe-core/playwright`) and `web/tests/component/a11y.test.tsx` (vitest-axe) on sign-up, log-in, list, form,
  delete and clear-completed dialogs, to-do detail and the not-found page.
- **Keyboard**: `web/e2e/keyboard.spec.ts` operates every action with the keyboard and checks focus moves into and out of dialogs.
- **Manual pass: not recorded (open gap).** SDD section 14 calls for a manual screen-reader and contrast pass on eight screens, recorded by web-qa,
  and REQ-NAV-001 AC-6 calls for a flash review of the not-found animation. Neither is in the CHG-0001 evidence, so this bundle does not count
  them as done ([NFR-001](../requirements/nfr-001.md)).

Screens: [sign-up](../screens/sign-up.md), [log-in](../screens/log-in.md), [to-do list](../screens/todo-list.md), [to-do form](../screens/todo-form.md),
[to-do detail](../screens/todo-detail.md), [confirm dialogs](../screens/confirm-dialogs.md), [not-found page](../screens/not-found.md).
Related: [e2e tests](e2e-tests.md), [web component tests](web-component-tests.md).
