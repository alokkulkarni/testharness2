---
type: screen
title: Not-found page
summary: For a signed-in user, an undefined address shows an animated, decorative "not found" page with a link back to home that plays once, stops within 5 seconds, and never flashes.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/app/NotFoundPage.tsx
  - web/src/app/AppRoutes.tsx
  - docs/specs/CHG-0001/sdd.md
---

# Not-found page

Component: `web/src/app/NotFoundPage.tsx`, rendered by the catch-all route in `web/src/app/AppRoutes.tsx`, in the [web SPA](../components/web-spa.md).

- Shown only to a signed-in user on an address that matches no other route ([REQ-NAV-001](../requirements/req-nav-001.md) AC-3).
- Content: an `<h1>` saying the page was not found, one sentence, and a link to `/` (home, which goes to `/todos`) ([REQ-NAV-001](../requirements/req-nav-001.md) AC-5).
- The animation is a CSS animation on an `aria-hidden="true"` element. It plays once, ends within 5 seconds (WCAG 2.2.2), and has no flashing (WCAG 2.3.1). The heading, sentence and link are usable from the first render ([REQ-NAV-001](../requirements/req-nav-001.md) AC-6, AC-7).
- The page makes no API call and never redirects.
- A signed-out visitor is sent to log in instead ([return-to rule](../rules/return-to-and-not-found.md)).

Tests: [web component tests](../tests/web-component-tests.md) (`navigation.test.tsx`), [e2e tests](../tests/e2e-tests.md) (`navigation.spec.ts`, which records the animation timing), [accessibility tests](../tests/accessibility-tests.md).
