# ADR-0001 React + TypeScript single-page app built with Vite

Status: proposed (CHG-0001, N03-spec); amended for Q56 (one generated types file per module contract). Requirements:
NFR-005, NFR-001, NFR-002, NFR-003, REQ-NAV-001, NFR-007; constraint C-1.

## Context

The requester fixed the UI as React + TypeScript (Q3, C-1). The repository has no UI code, so the build tool,
routing, data access and test tooling are open. The UI must meet WCAG 2.1 AA (NFR-001), run locally with no
hosted service (NFR-002) and show results within 1 second (NFR-003). The API contract is split into one file per
API module, `contracts/identity-api-v1.yaml` and `contracts/todo-api-v1.yaml`, which share
`contracts/common-problem-v1.yaml` by `$ref` (Source: clarifications Q56; ADR-0018).

## Decision

- A client-rendered SPA in `web/`, built with **Vite** and TypeScript in `strict` mode, using **React** (current
  major) and **React Router** for routes (`/`, `/signup`, `/login`, `/todos`, `/todos/new`, `/todos/:id`,
  `/todos/:id/edit`, and a catch-all `*` for the not-found page, behind the same auth guard as the to-do routes;
  `/` and `*`: Q37, Q46, Q47, REQ-NAV-001). List state
  (`status`, `q`, `page`) lives in the URL query string.
- Data access through one small `fetch` wrapper (`web/src/api/client.ts`); no global state library. API types are
  generated with **openapi-typescript** so the UI cannot drift from the contract: one file per module contract,
  `web/src/api/identity.ts` from `contracts/identity-api-v1.yaml` and `web/src/api/todos.ts` from
  `contracts/todo-api-v1.yaml`, configured in `web/redocly.yaml` (`apis` entries `identity` and `todos`, each with
  its `root` and `x-openapi-ts.output`). The cross-file `$ref`s into `common-problem-v1.yaml` are resolved by the
  generator, so each output carries its own, structurally identical `Problem` type; the SPA imports `Problem` from
  `web/src/api/todos.ts` only (sdd.md §2, §6.1). Source: tool spikes of 2026-11-06, recorded in
  `.harness/changes/CHG-0001/tool-spikes.md`; react-engineer spike with
  openapi-typescript 7.13.0 and @redocly/cli 2.76.0, Node 26.12.0, pnpm 11.17.0: per-module outputs generated,
  cross-file `$ref`s resolved, each output with its own structurally identical `Problem`, security schemes not
  emitted, unreferenced schemas dropped, and a CLI path argument ignored when a `redocly.yaml` exists. `redocly join`
  is not used (it adds tags, `x-tagGroups` and `servers: []` and keeps only the first `info`).
- The contract lint of ADR-0012 (`redocly lint`, `extends: [recommended]`, on the two module roots only) runs with
  the same `@redocly/cli` dev dependency.
- Confirmation dialogs use the native HTML `<dialog>` element with `showModal()` (built-in focus containment and
  Escape handling); no UI component library.
- In development the Vite dev server proxies `/api` to the API, so the SPA and API share one origin.
- Tests: **Vitest** + **React Testing Library** for components, **vitest-axe** (axe-core) for component a11y checks,
  **Playwright** with **@axe-core/playwright** for end-to-end and page-level a11y checks.
- pnpm with a committed lockfile; versions are pinned by the react-engineer at build time.

Dependencies introduced (all MIT or Apache-2.0, actively maintained): react, react-dom, react-router (MIT); vite,
typescript (MIT / Apache-2.0); openapi-typescript (MIT); vitest, @testing-library/react, @testing-library/user-event
(MIT); vitest-axe, axe-core, @axe-core/playwright (MIT / MPL-2.0 for axe-core); @playwright/test (Apache-2.0).
axe-core is MPL-2.0, used unmodified as a test-only dependency. @redocly/cli (MIT) is a dev dependency, for
`web/redocly.yaml` and the contract lint of ADR-0012; the react-engineer pins its version.

## Consequences

- One origin in development means no CORS configuration and the session cookie works with `SameSite=Strict`.
- Generated types make contract changes a compile error in the UI.
- Two generated files each hold a `Problem` type; a type-level test keeps the two equal (sdd.md §13, §14), and the
  SPA names only `Problem`, typing a field error as `Problem['errors'][number]`, so it does not depend on whether a
  separate `FieldError` type is emitted.
- Native `<dialog>` keeps the dependency set small; focus return to the invoking control is still written by hand.
- No server-side rendering; acceptable for a local, signed-in app.

## Alternatives

- Next.js / Remix: server rendering and a Node server are not needed for a local SPA backed by a Java API.
- Create React App: unmaintained.
- A component library (MUI, Chakra): faster styling, but a large dependency for eight audited screens (SDD §13); accessible native
  elements suffice.
- TanStack Query / Redux: caching and global state are unnecessary; every change re-queries the list
  (REQ-TODO-005 AC-3).
- One generated types file from a bundled or joined contract: `redocly join` keeps only the first `info` and adds
  noise, and a separate bundle step adds a build artefact; generating per module needs neither.
