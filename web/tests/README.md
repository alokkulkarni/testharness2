# Web test plan: what is and is not tested on the web side (CHG-0001, N08-tests-web)

Run: `pnpm -C web test` (Vitest: unit and component, jsdom), `pnpm -C web e2e` (Playwright; runs three configurations
one after another and fails if any fails: `e2e:main`, `e2e:restart`, `e2e:session-expiry`). `pnpm -C web typecheck`
is part of the gate: it is what enforces the type-level equality in `tests/unit/userMessage.test.ts` (NFR-006 AC-2;
Vitest `run` does not check types).

Conventions: tests are named with their requirement and criterion IDs. The app shell holds one always-present polite
live region with `role="status"` (ux-design section 6), so no test asserts that no status element exists; tests look
at the live regions' text (`tests/helpers/status.ts`; Playwright `getByRole('status').filter({ hasText })`).

## Acceptance criteria not tested on the web side, with reasons

Criteria IDs are those of `docs/specs/CHG-0001/requirements.md`. Backend coverage is the sdd section 14 matrix.

| Criterion | Why it is not a web test |
|---|---|
| REQ-AUTH-003 AC-1, AC-2 | Password hashing and storage in the database file; nothing is observable in the browser |
| REQ-AUTH-003 AC-3 | A password or hash never appearing in API responses or logs; the browser never receives one back. Not checked in the UI (no echo check) |
| REQ-AUTH-004 AC-1 | Calling the to-do API without a session; an API-level check. The web side covers AC-2, AC-3 and AC-4 (sent to log in; log out; expiry) |
| REQ-AUTH-005 AC-1, AC-2 | Absence of an admin operation and one role; there is no UI to exercise |
| REQ-ISO-001 AC-3 | "Clear all completed" deleting only the caller's to-dos needs two users with completed to-dos and is a data check; the web side covers AC-1, AC-2 and AC-4 (`acceptance.spec.ts`) |
| REQ-TODO-001 AC-2 (request sent directly to the API) | The UI always sends a priority; the UI default Medium is tested. The omitted-priority request is an API check |
| REQ-TODO-002 AC-8 (priority sent through the API) | API validation; the web side tests that the control offers only Low, Medium and High |
| REQ-TODO-006 AC-3, after a restart | The web side checks the to-do is gone after a reload (`acceptance.spec.ts`); a restart check needs the store and is the backend's (REQ-DATA-001) |
| REQ-LIST-003 AC-7 (the API refuses a bad page) | API validation; the SPA's own handling (a bad page in the URL is replaced with page 1) is tested |
| REQ-DATA-001 AC-2, AC-3 | The storage engine and first-start file creation; no browser behaviour. AC-1 is the restart e2e |
| REQ-DATA-002 AC-1, AC-2 | Last write wins is a server rule with no UI of its own (no conflict message exists); two-tab edits are not driven in the browser |
| NFR-001 AC-1 beyond what axe finds | axe checks the WCAG rules that can be automated on each screen and state (`a11y.spec.ts`, `a11y.test.tsx`); contrast of the design tokens and screen-reader behaviour are the design audit's and a human's |
| NFR-002 AC-2 | A delivery rule (no deployment to dev or test), not behaviour |
| NFR-004 AC-1, AC-2, AC-3 | Hashing, server-side validation, no stack traces, no committed secrets: backend and repository checks. The web side only checks the SPA never shows internals (NFR-006 AC-2) |
| NFR-005 AC-1 | A review of the stack, no behaviour |
| NFR-007 AC-1, AC-2, AC-3 | Backend module structure |
| Visual snapshots | None added: no component is stable enough yet to be design-critical (web-qa method step 4) |

If this table disagrees with the sdd section 14 matrix, the sdd governs.

## Departures and notes on the review of attempt 1 (critic findings)

- F-7: the `expectTypeOf` check cannot run under `vitest run`; it is enforced by `tsc` through `pnpm -C web typecheck`
  (tsconfig includes `tests/`), which is therefore part of the red and green proof. It is not moved to a `.test-d.ts`
  file because Vitest's typecheck mode would also report every missing-module type error as a failure.
- F-10: the SPA may trim the title and identifier or leave it to the server (sdd section 5); the tests compare the trimmed value.
- F-24: test-only dependencies beyond the ADR-0001 list (jsdom, @testing-library/jest-dom, @vitejs/plugin-react,
  eslint with typescript-eslint and @eslint/js, @types/node) are test infrastructure. Recording them, and the
  TypeScript line, in the ADR-0001 version pins is the react-engineer's task.
