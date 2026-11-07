# Architecture diagrams — CHG-0001 To-do list

Node N05-design-arch. Drawn from the **frozen** spec of CHG-0001 (`docs/specs/CHG-0001/`: `requirements.md`, `sdd.md`,
`contracts/todo-api-v1.yaml`, `adr/0001..0017`) and, for the screen flow only, `design/ux/ux-design.md`. Each diagram
is an editable draw.io source with a PNG (2x) and a cropped PDF export, produced by `harness docs diagram export`
(manifest `diagram-exports.json`; `--check` passes). Do not edit the PNG, PDF or manifest by hand, re-export instead.
Nothing is drawn that the frozen spec does not state.

| # | Diagram | Shows | Traces to |
|---|---|---|---|
| 1 | [C4 context](c4-context.drawio) · [PNG](png/c4-context.png) · [PDF](pdf/c4-context.pdf) | The user and the to-do app as one system; no external system exists | NFR-002, NFR-005, REQ-AUTH-005; sdd §1, §2, §11 |
| 2 | [C4 container](c4-container.drawio) · [PNG](png/c4-container.png) · [PDF](pdf/c4-container.pdf) | SPA, Vite dev proxy, Spring Boot API, SQLite file and their calls | NFR-002, NFR-005, REQ-DATA-001; sdd §2, §3, §11; ADR-0001, 0002, 0003, 0009, 0014, 0016 |
| 3 | [C4 component, API](c4-component-api.drawio) · [PNG](png/c4-component-api.png) · [PDF](pdf/c4-component-api.pdf) | Filters, controllers, services, validators, JDBC repositories, session and auth, problem-details error path, clock | REQ-AUTH-001..005, REQ-ISO-001, REQ-TODO-001..007, REQ-LIST-001..003, NFR-004; sdd §2, §4–§7, §10; ADR-0004, 0005, 0007–0011, 0013, 0015, 0016 |
| 4 | [Sequence: sign-up, login, cookie, header defence](seq-signup-login.drawio) · [PNG](png/seq-signup-login.png) · [PDF](pdf/seq-signup-login.pdf) | Sign-up (no session), login with Argon2id and session row, `todo_session` cookie, 403 without `X-Todo-Client` | REQ-AUTH-001..004; sdd §4, §5; ADR-0004, 0005, 0007, 0008, 0015; contract `signUp`, `logIn` |
| 5 | [Sequence: create, edit, list](seq-todo-crud-list.drawio) · [PNG](png/seq-todo-crud-list.png) · [PDF](pdf/seq-todo-crud-list.pdf) | Create, whole-content update with owner-scoped SQL and 404 path, list with search, filter, paging and clamping | REQ-TODO-001..005, REQ-LIST-001..003, REQ-ISO-001, REQ-DATA-002; sdd §5, §7; ADR-0006, 0009, 0010, 0013, 0015, 0017; contract `createTodo`, `updateTodo`, `listTodos` |
| 6 | [Sequence: clear completed](seq-clear-completed.drawio) · [PNG](png/seq-clear-completed.png) · [PDF](pdf/seq-clear-completed.pdf) | Totals re-fetch, confirmation, owner-scoped `DELETE ... done = 1`, re-query | REQ-TODO-007, REQ-ISO-001 AC-3; sdd §7, §8; contract `clearCompletedTodos` |
| 7 | [Data model](data-model.drawio) · [PNG](png/data-model.png) · [PDF](pdf/data-model.pdf) | `users`, `sessions`, `todos` tables, keys, indexes, cascade rules, derived columns | REQ-AUTH-001 AC-6, REQ-AUTH-005 AC-2, REQ-ISO-001, REQ-DATA-001; sdd §3; ADR-0003, 0004, 0005, 0008, 0014–0017 |
| 8 | [Deployment, local only](deployment-local.drawio) · [PNG](png/deployment-local.png) · [PDF](pdf/deployment-local.pdf) | One machine: browser, Vite :5173, API :8080, `todo.db`, settings; no cloud | NFR-002, NFR-005, REQ-DATA-001 AC-3; sdd §11, §13; ADR-0001–0003 |
| 9 | [Screen flow](screen-flow.drawio) · [PNG](png/screen-flow.png) · [PDF](pdf/screen-flow.pdf) | Routes, guards, session probe, login/returnTo, dialogs | REQ-AUTH-001, 002, 004; REQ-TODO-001..007; sdd §4.1, §4.2, §8; `design/ux/ux-design.md` §2 |

## Conventions

C4 colours and shapes follow the `drawio-c4` skill. Sequence diagrams use draw.io lifelines: solid arrows are calls
(numbered), dashed arrows are responses; each label ends with the protocol or mechanism.

## Known simplifications and omissions

- Component view: `SecurityConfig` and `DatabaseConfig` wiring, the `ApiException` subtypes and the test-source-set
  classes (sdd §2, §11) are not drawn. Some edge labels sit close to crossing lines.
- The test-only clock run (ADR-0013, sdd §11) is not part of the normal deployment and is not drawn.
- Screen flow: the sdd does not say where "save" on the edit form navigates, so no such edge is drawn; the delete
  dialog opened from the detail page (ux-design.md) is not drawn.

## Pending, not drawn

A spec supersede is **proposed but not frozen** (`docs/proposals/CHG-0001/supersede-1/`: a `/` to `/todos` redirect, an
animated not-found route REQ-NAV-001, NFR-006 user-friendly errors). It is not authority, so none of it appears in these
diagrams. After a re-freeze, the screen flow, the component view and possibly the sequence error paths must be updated.
