---
stage: plan-node
changeId: CHG-0001
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0001
    agent: orchestrator
    agentRunId: run-plan-md-1
upstream:
  - path: docs/specs/CHG-0001/requirements.md
    sha256: 266b5b6d3d319d635b6fbe7e01d1750d017d704a0a24dada7243c8b6264241bb
  - path: docs/specs/CHG-0001/sdd.md
    sha256: 13262e8e00cf325ca29d7a40011bd6e88949bcd0c854029e8f79f3e9eee41d4b
---

# N10-build-react — react-engineer

Make the react tests pass in an isolated worktree.

## Files

- `web/index.html`
- `web/vite.config.ts`
- `web/scripts/generate-tokens.mjs`
- `web/src/main.tsx`
- `web/src/styles/tokens.css`
- `web/src/styles/app.css`
- `web/src/api/client.ts`
- `web/src/validation/whitespace.ts`
- `web/src/validation/rules.ts`
- `web/src/errors/userMessage.ts`
- `web/src/auth/returnTo.ts`
- `web/src/auth/SessionProvider.tsx`
- `web/src/auth/RequireAuth.tsx`
- `web/src/auth/LoginPage.tsx`
- `web/src/auth/SignupPage.tsx`
- `web/src/todos/todoId.ts`
- `web/src/todos/format.ts`
- `web/src/todos/TodoListPage.tsx`
- `web/src/todos/FilterGroup.tsx`
- `web/src/todos/SearchForm.tsx`
- `web/src/todos/Pagination.tsx`
- `web/src/todos/TodoRow.tsx`
- `web/src/todos/TodoDetailPage.tsx`
- `web/src/todos/TodoFormPage.tsx`
- `web/src/todos/ConfirmDialog.tsx`
- `web/src/app/AppRoutes.tsx`
- `web/src/app/AppShell.tsx`
- `web/src/app/AppErrorBoundary.tsx`
- `web/src/app/NotFoundPage.tsx`
- `web/src/app/ui.tsx`
- `web/src/app/usePageFocus.ts`

## Order

1. Pure modules (whitespace, returnTo, todoId, API types, userMessage) against `tests/unit`.
2. API client, SessionProvider, RequireAuth, app shell, routes, error boundary, not-found page, log in and sign up against `tests/component/auth` and `navigation`.
3. List screen with filter, search, pagination, row toggle, delete and clear-completed dialogs against `list` and `errors`.
4. To-do form (create, edit) and detail page against `todo-form`.
5. Styles from the design tokens, `index.html`, `vite.config.ts` (dev proxy), then axe component tests, typecheck, lint.
6. One `pnpm -C web e2e` run against the real API (after N09 merged); attach the unit/component JUnit report and the e2e reports as evidence.

## Risks

- F-26: `throwingResponse` is a 2xx with no Content-Type whose body read rejects; the client reads every 2xx JSON body regardless of Content-Type and maps a body-read rejection to `{ kind: 'unexpected' }` (a `fetch` rejection is `network`), so the N-2 tests pass within sdd §6.1.
- Focus and no-remount rules (filter change keeps the `<h1>` element and the radio focus; pagination moves focus to the `<h1>` at once) depend on never keying the list page by location.
- Native `<dialog>` under jsdom (a test-setup shim provides `showModal`); the dialog is mounted only while open.
- A 401 while signed in is reported to `SessionProvider` and the call site then shows no error of its own.
- Firefox for Playwright could not run on the requester's host; it is reported, never installed (see Accepted divergences).
- F-24 (version pins, ADR-0001): TypeScript stays on 6.0.3 because the pinned `typescript-eslint` 8.72.1 declares the peer range `typescript >=4.8.4 <6.1.0`, which excludes TypeScript 7. The test-only dev dependencies beyond the ADR-0001 list are jsdom (Vitest DOM environment), `@testing-library/jest-dom` (matchers), `@vitejs/plugin-react` (JSX transform for Vite and Vitest), `eslint`, `@eslint/js` and `typescript-eslint` (the `lint` script) and `@types/node` (Node types for the e2e support code); they were added by the tests node (N08), not by this node, and are test or tooling only.
- Choices inside the spec that a reviewer may question: AppErrorBoundary also remembers a caught error outside the render (module variable, cleared on pathname change or unmount), because React renders a tree that threw a second time and discards the boundary state if the retry does not throw; without it errors.test.tsx "following the error page's link to home" never shows the error page. The rule "only a pathname change clears it" is kept.
- The list screen starts its request in a layout effect and renders the response with flushSync, so the rows are on screen in the task that receives the data.
- The list screen, when it is the page the document loaded on, leaves focus at the start of the document (skip link first Tab stop, NFR-001 AC-2); every other screen focuses its heading on arrival.
- A signed-in visitor on /login is sent on by PublicRoute to the valid returnTo or /todos, which is also how a successful log in navigates.
- e2e run against the real API (fix(web)): TodoRow shows the requested checked state while the toggle request is in flight (reverted on failure), FilterGroup shows the chosen radio at once (the address still drives status), ConfirmDialog wraps Tab and Shift+Tab inside the modal (NFR-001 AC-2). All three are under Files; no spec rule changed.
- web/src/styles/tokens.css is generated from docs/specs/CHG-0001/design/ux/tokens.json by web/scripts/generate-tokens.mjs (DD-1).

## Proof

- `web/tests/unit/whitespace.test.ts`, `routing-helpers.test.ts`, `userMessage.test.ts` (REQ-TODO-002, REQ-AUTH-001, REQ-AUTH-004, REQ-NAV-001, NFR-006).
- `web/tests/component/auth.test.tsx`, `navigation.test.tsx`, `list.test.tsx`, `todo-form.test.tsx`, `errors.test.tsx`, `a11y.test.tsx` (all REQ-* and NFR-001, NFR-006 web criteria).
- `pnpm -C web typecheck` (type-level Problem equality, NFR-006 AC-2), `pnpm -C web lint`.
- Evidence: .harness/changes/CHG-0001/evidence/test-report/N10-build-react.component-junit-r2.xml (Vitest, merged tree); N10-build-react.e2e-main-full.xml, N10-build-react.e2e-restart-full.xml, N10-build-react.e2e-session-expiry-full.xml (Playwright against the real API; Chromium and WebKit, see Accepted divergences for Firefox).
- critic-verdict.

## Departures

- `web/src/api/identity.ts`: ADR-0001 says it is generated by openapi-typescript; neither it nor `@redocly/cli` is a dependency in `web/package.json`, and this node adds no dependency, so the file is hand-written from the contract (same names and shapes, with the top-level `Problem` alias the tests import). Regenerate when the generator is added.
- `web/src/api/todos.ts`: hand-written from the contract for the same reason as `web/src/api/identity.ts` (no generator dependency).
- `web/package.json`: gains the script `tokens` (runs `scripts/generate-tokens.mjs`); no dependency added.
- `web/src/vite-env.d.ts`: a one-line `vite/client` reference so `tsc` accepts the stylesheet imports in `main.tsx`.
- `web/tests/component/todo-form.test.tsx`: fixture defect fixed by web-qa under N10 by requester decision (`withNotes()` row 2 now has due date 2026-11-07 so the list shows `due 6 nov 2026` once; no assertion changed). In future changes this kind of test fix is expected to go through the tests node, the N08 equivalent.
- `web/tests/helpers/renderApp.tsx`: `render()` gets an `onRecoverableError` that ignores only React's "recovered by synchronously rendering the entire root" report whose cause is one of the deliberate test errors (`boom`, `SENTINEL-INTERNALS-7f3a`) and passes every other recoverable error to `reportError`; vitest counted those three recovered, deliberate errors as unhandled (exit 1 with 484/484 passing). Used by errors.test.tsx and a11y.test.tsx. No test, assertion or vitest option changed. Reason: requester decision, web-qa under N10; future changes route this through the tests node.
- `web/playwright.config.ts`: webkit project added by requester decision.
- `web/e2e/playwright.restart.config.ts`: firefox and webkit projects added beside chromium (workers 1 and fullyParallel false already serialise the projects, so the API restart is never used by two browsers at once; no other change). Reason: requester decision, web-qa under N10; future changes route this through the tests node.
- `web/e2e/playwright.session-expiry.config.ts`: firefox and webkit projects added beside chromium (workers 1 and fullyParallel false already serialise the projects, so the shared clock-offset file is never used by two browsers at once; no other change). Reason: requester decision, web-qa under N10; future changes route this through the tests node.
- `web/e2e/restart.spec.ts`: test defect, link matched by exact name; no assertion changed, none skipped. Reason: requester decision, web-qa under N10.
- `web/e2e/performance.spec.ts`: test defect, the row Edit link is matched by `/^Edit /`; no assertion changed, none skipped. Reason: requester decision, web-qa under N10.
- `web/e2e/keyboard.spec.ts`: test defect, `tabTo` limit 150 (20 rows per page at about 4 tab stops plus page chrome plus margin) and Tab through `pressTab`; no assertion changed, none skipped. Reason: requester decision, web-qa under N10.
- `web/e2e/navigation.spec.ts`: test defect, home link Tab through `pressTab`; no assertion changed, none skipped. Reason: requester decision, web-qa under N10.
- `web/e2e/acceptance.spec.ts`: test defect, paging waits for "Showing 21-40 of 45" before reading page 2; no assertion changed, none skipped. Reason: requester decision, web-qa under N10.
- `web/e2e/support/app.ts`: new `pressTab` helper (Alt+Tab in WebKit, Tab elsewhere) used by the keyboard and navigation specs. Reason: requester decision, web-qa under N10; future changes route e2e test fixes through the tests node.
- `web/tests/component/todo-form.test.tsx`: critic attempt-1 F-1 and F-3. `withNotes()` row 2 now has due date 2026-12-20 (no one-day time-zone shift can render `due 6 nov 2026` for the wrong row, so the list assertion catches a UTC-midnight parse bug under Pacific/Pago_Pago; shown by a temporary TZ-naive `formatDate`, reverted and not committed), superseding the 2026-11-07 date above; and the detail-page delete 404 test gains a focus-on-h1 assertion (ux-design section 6, NFR-001 AC-2), red against the current SPA. Reason: requester decision, web-qa under N10; future changes route this through the tests node.
- `web/tests/component/list.test.tsx`: critic attempt-1 F-2. New test (two cases, done=false and done=true) for REQ-TODO-005 and REQ-DATA-002: the toggle PUT succeeds but the re-query returns the other `done` value; the checkbox shows the server value and the next click sends the reverse of it; red against the current SPA. Reason: requester decision, web-qa under N10; future changes route this through the tests node.

## Accepted divergences

- Browser coverage of the e2e suite. The requester's session ran the full three-browser `pnpm -C web e2e` outside the sandbox on change-branch commit 13f00a0. Chromium and WebKit pass every configuration (main 52/52, restart 1/1, session-expiry 2/2 each). Firefox could not run on this macOS host: every test fails at launch with "browserType.launch: Failed to launch the browser process ... Could not find profile folder" (Playwright 1.65.0, firefox-1572, macOS 27.2), also after a forced reinstall. The requester accepted this for this change as an environment limitation, since no acceptance criterion names a browser. The firefox projects stay in the Playwright configs. Follow-up: run the Firefox e2e in CI on Linux.
