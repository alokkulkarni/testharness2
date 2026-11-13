---
type: requirement
title: REQ-NAV-001 Home route and not-found page
summary: The root address goes to the to-do list, an undefined address shows a signed-in user an accessible not-found page with a link home, and signed-out visitors are sent to log in.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-NAV-001 Home route and not-found page (M)

Spec: [requirements.md, REQ-NAV-001](../../specs/CHG-0001/requirements.md#req-nav-001-home-route-and-not-found-page-m).

**Acceptance criteria in brief.** AC-1 `/` goes to `/todos`; AC-2 signed-out `/` leads to log in and then the list; AC-3 an
undefined address shows the not-found page to a signed-in user; AC-4 `/todos/<id>` with an unknown id shows the to-do
not-found state; AC-5 the link home works; AC-6 the animation is decorative, plays once and stops within 5 seconds, with no
flashing; AC-7 keyboard operable with visible focus; AC-8 signed-out visitors to an undefined address go to log in.

**Implemented by.** [web SPA](../components/web-spa.md) (`AppRoutes.tsx`, `RequireAuth.tsx`, `NotFoundPage.tsx`, `todos/todoId.ts`),
screen [not-found page](../screens/not-found.md).

**Rules.** [return-to and not-found](../rules/return-to-and-not-found.md).

**Verified by.** [web component tests](../tests/web-component-tests.md) (`navigation.test.tsx`), [web unit tests](../tests/web-unit-tests.md)
(`routing-helpers.test.ts`), [e2e tests](../tests/e2e-tests.md) (`navigation.spec.ts`), [accessibility tests](../tests/accessibility-tests.md) (`a11y.spec.ts`).
The AC-6 check covers the animation's timing and reduced motion only. The web-qa flash review that [sdd.md section 14](../../specs/CHG-0001/sdd.md#14-verification-and-traceability)
names for AC-6 is not recorded in the change's evidence ([NFR-001](nfr-001.md), open gap).
