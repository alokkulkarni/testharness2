# Architecture diagrams — CHG-0001 To-do list

Node N05-design-arch. Drawn on 2026-11-08 against the **current frozen spec** of CHG-0001: the re-freeze approved
2026-11-07 (`.harness/changes/CHG-0001/spec-freeze.json`, supersede-2, which supersedes
`sha256:90a67418…`; approval PR #3 review `pullrequestreview-5810670897`). Frozen sources in `docs/specs/CHG-0001/`:
`requirements.md` (incl. REQ-NAV-001, NFR-006, NFR-007, C-2), `sdd.md`, `contracts/identity-api-v1.yaml`,
`contracts/todo-api-v1.yaml`, `contracts/common-problem-v1.yaml` and `adr/0001..0018`. The screen flow also uses
`design/ux/ux-design.md` §2 (not frozen): its screen ids, and its edges where the sdd is silent, which are drawn dashed.
Each diagram is an editable draw.io source with a PNG (2x) and a cropped PDF export, produced by
`harness docs diagram export` (manifest `diagram-exports.json`; `--check` passes). Do not edit the PNG, PDF or manifest
by hand, re-export instead. Nothing is drawn that the frozen spec does not state. Layout rules applied to every
diagram: orthogonal edges with explicit waypoints, labels on clear stretches of their own edge (never over a shape),
12 pt text (set explicitly on every box, edge label, title and legend), 8 px text padding, containers sized to hold their children, a legend sized to its text; the one unavoidable edge crossing
(`routes /auth/**` over `resolves session via` in the component view) uses the arc line-hop style.

Each trace cell equals the trace line in the diagram's title block.

| # | Diagram | Shows | Traces to |
|---|---|---|---|
| 1 | [C4 context](c4-context.drawio) · [PNG](png/c4-context.png) · [PDF](pdf/c4-context.pdf) | The user and the to-do app as one system; no external system exists | NFR-002, NFR-005, NFR-007, REQ-AUTH-005, C-2; sdd §1, §2, §11; ADR-0018 |
| 2 | [C4 container](c4-container.drawio) · [PNG](png/c4-container.png) · [PDF](pdf/c4-container.pdf) | SPA, Vite dev proxy, the modular-monolith Spring Boot API (one process, one pool), SQLite file and their calls | NFR-002, NFR-005, NFR-007, REQ-DATA-001, C-2; sdd §2, §3, §11; ADR-0001, 0002, 0003, 0009, 0014, 0016, 0018 |
| 3 | [C4 component, API modules](c4-component-api.drawio) · [PNG](png/c4-component-api.png) · [PDF](pdf/c4-component-api.pdf) | Identity (auth), to-dos (todo), shared kernel (common) and composition root (config) boundaries; the `auth.api` `CurrentUser` seam; filters, controllers, services, validators, package-private JDBC repositories, clock; allowed dependency directions in the legend | NFR-007 AC-1..AC-3, REQ-ISO-001, REQ-AUTH-002 AC-4, REQ-AUTH-004 AC-1; sdd §2, §3, §4, §7, §10; ADR-0004, 0005, 0007..0010, 0013..0016, 0018 |
| 4 | [C4 component, API error path](c4-component-errors.drawio) · [PNG](png/c4-component-errors.png) · [PDF](pdf/c4-component-errors.pdf) | `ProblemWriter` as the only error-body builder and its callers: advice, entry point (401 UNAUTHENTICATED), client-header filter (403), firewall handler, `/error` controller, `RequestIdFilter` escape path | NFR-004, NFR-006 AC-2, NFR-007, REQ-AUTH-002 AC-2, REQ-AUTH-004 AC-1; sdd §2, §4, §6, §10; ADR-0004, 0007, 0011, 0018; `common-problem-v1.yaml` `Problem`, `FieldError` |
| 5 | [Sequence: sign-up, login, cookie, header defence](seq-signup-login.drawio) · [PNG](png/seq-signup-login.png) · [PDF](pdf/seq-signup-login.pdf) | Sign-up (no session), login with Argon2id and session row, `todo_session` cookie, 403 for a direct caller without `X-Todo-Client`, cross-origin preflight note | REQ-AUTH-001, REQ-AUTH-002, REQ-AUTH-003, NFR-004; sdd §2, §4, §5, §6; ADR-0004, 0005, 0007, 0008, 0011, 0015, 0016, 0018; `identity-api-v1.yaml` `signUp`, `logIn` |
| 6 | [Sequence: create, edit, list](seq-todo-crud-list.drawio) · [PNG](png/seq-todo-crud-list.png) · [PDF](pdf/seq-todo-crud-list.pdf) | Create, whole-content update with owner-scoped SQL and 404 path, list with search, filter, paging and clamping; owner from the `CurrentUser` principal | REQ-TODO-001, REQ-TODO-002, REQ-TODO-003 AC-1, REQ-TODO-004, REQ-LIST-001..003, REQ-ISO-001, REQ-DATA-002, NFR-007; sdd §2, §5, §7; ADR-0006, 0009, 0010, 0013, 0015, 0016, 0017, 0018; `todo-api-v1.yaml` `createTodo`, `updateTodo`, `listTodos` |
| 7 | [Sequence: clear completed](seq-clear-completed.drawio) · [PNG](png/seq-clear-completed.png) · [PDF](pdf/seq-clear-completed.pdf) | Totals re-fetch, plural and singular confirmation, N = 0 branch, owner-scoped `DELETE ... done = 1`, re-query | REQ-TODO-007 AC-1..AC-4, REQ-ISO-001 AC-3, NFR-007; sdd §2, §7, §8; ADR-0009, 0018; `todo-api-v1.yaml` `listTodos`, `clearCompletedTodos` |
| 8 | [Data model](data-model.drawio) · [PNG](png/data-model.png) · [PDF](pdf/data-model.pdf) | `users`, `sessions` (owner auth), `todos` (owner todo), `flyway_schema_history` (owner config); keys, indexes, cascade rules; `todos.user_id` as the accepted cross-module foreign key | sdd §2, §3; NFR-007 AC-3; ADR-0003, 0004, 0005, 0008, 0014, 0015, 0016, 0017, 0018; REQ-AUTH-001 AC-6, REQ-AUTH-005 AC-2, REQ-ISO-001, REQ-DATA-001 |
| 9 | [Deployment, local only](deployment-local.drawio) · [PNG](png/deployment-local.png) · [PDF](pdf/deployment-local.pdf) | One machine: browser, Vite :5173, API :8080 (one process), `todo.db`, settings; no cloud | NFR-002, NFR-005, NFR-007 AC-1, REQ-DATA-001 AC-3; sdd §11, §13; ADR-0001, 0002, 0003, 0013, 0016, 0018 |
| 10 | [Screen flow](screen-flow.drawio) · [PNG](png/screen-flow.png) · [PDF](pdf/screen-flow.pdf) | Routes incl. `/` and catch-all `*` (S7), guards, session probe and probe-failed state, login/returnTo, dialogs, application error page E1 | REQ-AUTH-001, REQ-AUTH-002 AC-1 AC-4, REQ-AUTH-004 AC-2..AC-4, REQ-NAV-001, REQ-TODO-001..007, NFR-006 AC-1 AC-3; sdd §4.1, §4.2, §6.1, §8; `design/ux/ux-design.md` §2 |

## Conventions

C4 colours and shapes follow the `drawio-c4` skill. Sequence diagrams use draw.io lifelines: solid arrows are calls
(numbered), dashed arrows are responses; each label ends with the protocol or mechanism. Brackets after a participant
name its module package. Where one lifeline lists several components (filters plus controller), the order inside the API
is in the component views. In the error-path view the dashed arrow from `SessionCookieAuthenticationFilter` to
`ProblemAuthenticationEntryPoint` is not a call by the filter: Spring Security invokes the entry point (sdd §4).

## Known simplifications and omissions

- Component views: `ApiException` subtypes and the test-source-set classes (sdd §2, §11) are not drawn. Edges between
  filters show runtime order, not code dependencies (legend). `SecurityConfig` has no drawn edge: it wires every other
  component (config may use any package).
- The test-only clock run (ADR-0013, sdd §11) is not part of the normal deployment and is not drawn.
- Sequences do not draw per-request cookie resolution, logout, `getTodo` or `setTodoDone`; their trace cells name only
  what is drawn.
- Screen flow: dashed edges (Edit link on a row, Delete from the detail page, Save on edit, Q40 and Q41) come from
  `ux-design.md` §2.2, which is not frozen; the sdd is silent on them.
