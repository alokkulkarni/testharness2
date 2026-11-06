# ADR-0001 React + TypeScript single-page app built with Vite

Status: proposed (CHG-0001, N03-spec). Requirements: NFR-005, NFR-001, NFR-002, NFR-003, REQ-NAV-001; constraint C-1.

## Context

The requester fixed the UI as React + TypeScript (Q3, C-1). The repository has no UI code, so the build tool,
routing, data access and test tooling are open. The UI must meet WCAG 2.1 AA (NFR-001), run locally with no
hosted service (NFR-002) and show results within 1 second (NFR-003).

## Decision

- A client-rendered SPA in `web/`, built with **Vite** and TypeScript in `strict` mode, using **React** (current
  major) and **React Router** for routes (`/`, `/signup`, `/login`, `/todos`, `/todos/new`, `/todos/:id`,
  `/todos/:id/edit`, and a catch-all `*` for the not-found page, behind the same auth guard as the to-do routes;
  `/` and `*`: Q37, Q46, Q47, REQ-NAV-001). List state
  (`status`, `q`, `page`) lives in the URL query string.
- Data access through one small `fetch` wrapper (`web/src/api/client.ts`); no global state library. API types are
  generated from `contracts/todo-api-v1.yaml` with **openapi-typescript** so the UI cannot drift from the contract.
- Confirmation dialogs use the native HTML `<dialog>` element with `showModal()` (built-in focus containment and
  Escape handling); no UI component library.
- In development the Vite dev server proxies `/api` to the API, so the SPA and API share one origin.
- Tests: **Vitest** + **React Testing Library** for components, **vitest-axe** (axe-core) for component a11y checks,
  **Playwright** with **@axe-core/playwright** for end-to-end and page-level a11y checks.
- pnpm with a committed lockfile; versions are pinned by the react-engineer at build time.

Dependencies introduced (all MIT or Apache-2.0, actively maintained): react, react-dom, react-router (MIT); vite,
typescript (MIT / Apache-2.0); openapi-typescript (MIT); vitest, @testing-library/react, @testing-library/user-event
(MIT); vitest-axe, axe-core, @axe-core/playwright (MIT / MPL-2.0 for axe-core); @playwright/test (Apache-2.0).
axe-core is MPL-2.0, used unmodified as a test-only dependency.

## Consequences

- One origin in development means no CORS configuration and the session cookie works with `SameSite=Strict`.
- Generated types make contract changes a compile error in the UI.
- Native `<dialog>` keeps the dependency set small; focus return to the invoking control is still written by hand.
- No server-side rendering; acceptable for a local, signed-in app.

## Alternatives

- Next.js / Remix: server rendering and a Node server are not needed for a local SPA backed by a Java API.
- Create React App: unmaintained.
- A component library (MUI, Chakra): faster styling, but a large dependency for eight audited screens (SDD §13); accessible native
  elements suffice.
- TanStack Query / Redux: caching and global state are unnecessary; every change re-queries the list
  (REQ-TODO-005 AC-3).
