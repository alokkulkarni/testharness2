---
stage: sdd
changeId: CHG-0001
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0001
    agent: spec-author
    agentRunId: run-mw4rq9pt
upstream:
  - path: docs/specs/CHG-0001/requirements.md
    sha256: b4e11b8435570c1e0ce269019a40bde2cf5a5c11210bef3f22a4df32ee1a87dc
---

# SDD — CHG-0001 To-do list

## 1. Context and goals

A new, locally run to-do app: people sign up, log in, and manage only their own to-dos through a web UI backed by
an HTTP API, stored in SQLite. Requirements: `requirements.md` — REQ-AUTH-001..005, REQ-ISO-001, REQ-TODO-001..007,
REQ-LIST-001..003, REQ-DATA-001..002, REQ-ACC-001, NFR-001..005. Stack fixed by C-1 (NFR-005).

Interfaces: **[contracts/todo-api-v1.yaml](contracts/todo-api-v1.yaml)** (OpenAPI 3.1) is the source of truth for
every endpoint, schema, limit and error; this document does not repeat it. Decisions: [adr/](adr/) 0001–0006.

Nothing outside the requirements is designed here (no admin, password reset, email, sorting, page sizes, sharing).

## 2. Components

| Component | Location | Responsibility |
|---|---|---|
| SPA | `web/` — React + TypeScript, Vite (ADR-0001) | Screens, client-side validation (mirror only), routing, a11y |
| API | `api/` — Java 21, Spring Boot, Spring JDBC (ADR-0002) | Auth, sessions, validation (authoritative), to-do operations, error mapping |
| Database | SQLite file at `TODO_DB_PATH` (ADR-0003) | Users, sessions, to-dos |

Browser → (same origin via Vite proxy) → API `/api/v1/**` → SQLite. No other process or network service (NFR-002).

API packages (root `app.todo`):

- `auth`: `AuthController` (sign-up, login, logout, session), `AuthService`, `SessionService`,
  `SessionCookieAuthenticationFilter`, `ClientHeaderFilter`, `PasswordRules`, `IdentifierRules`.
- `user`: `UserRepository`.
- `todo`: `TodoController`, `TodoService`, `TodoRepository`, `TodoValidator`, `TodoQuery` (status, q, page).
- `common`: `ProblemDetailsAdvice`, `ApiException` subtypes (`ValidationException`, `NotFoundException`,
  `IdentifierTakenException`, `InvalidCredentialsException`), `ClockConfig`, `RequestIdFilter`.
- `config`: `SecurityConfig`, `DatabaseConfig` (creates the parent directory of `TODO_DB_PATH`, SQLite pragmas).

SPA modules: `src/api/client.ts` (fetch wrapper: JSON, `X-Todo-Client: web` on unsafe methods, 401 handling),
`src/api/schema.ts` (generated from the contract), `src/auth/` (`SessionProvider`, `RequireAuth`, `LoginPage`,
`SignupPage`), `src/todos/` (`TodoListPage`, `FilterGroup`, `SearchForm`, `Pagination`, `TodoRow`,
`TodoDetailPage`, `TodoFormPage`, `ConfirmDialog`), `src/validation/` (rules mirroring §5).

## 3. Data model (Flyway `V1__init.sql`, ADR-0003)

**users**

| Column | Type | Rule |
|---|---|---|
| `id` | INTEGER PK | internal only, never returned |
| `identifier` | TEXT NOT NULL | trimmed, original case (shown back as `User.identifier`) |
| `identifier_key` | TEXT NOT NULL UNIQUE | `identifier` lowered with `Locale.ROOT` (REQ-AUTH-001 AC-6) |
| `password_hash` | TEXT NOT NULL | Argon2id encoded string (ADR-0005) |
| `created_at` | TEXT NOT NULL | UTC timestamp |

No role column: every account has the same permissions (REQ-AUTH-005 AC-2).

**sessions**: `token_hash` TEXT PK (SHA-256 hex), `user_id` INTEGER NOT NULL FK → users ON DELETE CASCADE,
`created_at` TEXT NOT NULL, `expires_at` TEXT NOT NULL; index `ix_sessions_expires_at (expires_at)`.

**todos**

| Column | Type | Rule |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | internal; tie-break for ordering |
| `public_id` | TEXT NOT NULL UNIQUE | random UUID v4, lower case; the API's `id` (not guessable or countable) |
| `user_id` | INTEGER NOT NULL FK → users ON DELETE CASCADE | owner |
| `title` | TEXT NOT NULL | trimmed, 1–200 code points |
| `title_search` | TEXT NOT NULL | `title` lowered with `Locale.ROOT` |
| `description` | TEXT NULL | as sent, ≤ 2000 code points; NULL = no description |
| `description_search` | TEXT NULL | `description` lowered with `Locale.ROOT` |
| `due_date` | TEXT NOT NULL | `YYYY-MM-DD` |
| `priority` | TEXT NOT NULL CHECK IN ('LOW','MEDIUM','HIGH') | |
| `done` | INTEGER NOT NULL CHECK IN (0,1) | 0 Active, 1 Completed |
| `created_at`, `updated_at` | TEXT NOT NULL | UTC `yyyy-MM-ddTHH:mm:ss.SSSZ` (text order = time order) |

Indexes: `ix_todos_user_created (user_id, created_at DESC, id DESC)`,
`ix_todos_user_done_created (user_id, done, created_at DESC, id DESC)`.

## 4. Auth and session (ADR-0004, ADR-0005)

- **Sign-up** (`signUp`): trim identifier; validate (§5); hash password (Argon2id); insert. A `UNIQUE` violation on
  `identifier_key`, including a concurrent duplicate, maps to 409 `IDENTIFIER_TAKEN`. Sign-up does not start a
  session; the SPA shows "Account created — log in" on the login page (REQ-AUTH-001 AC-1). No verification step (AC-4).
- **Login** (`logIn`): trim the identifier exactly as sign-up does and look it up by its lower-cased key (finding 1,
  REQ-AUTH-002 AC-5); no format check at login. Empty identifier or password → 400. Unknown identifier → verify
  against a dummy hash, then 401 `INVALID_CREDENTIALS`; wrong password → the same 401 body; password over
  128 code points → the same 401 without hashing (REQ-AUTH-002 AC-2, AC-3). Success creates a session row and sets
  the cookie.
- **Session lifetime**: 8 hours **absolute from login**, not extended by activity (finding 2, REQ-AUTH-002 AC-4).
- **Each request**: `SessionCookieAuthenticationFilter` hashes the cookie token and loads the session; missing,
  unknown or `expires_at <= now` → unauthenticated (expired row deleted). All `/api/v1/todos/**` and
  `GET /api/v1/auth/session` require a session; sign-up, login and logout do not.
- **Order of checks** on protected unsafe requests: session (401) → `X-Todo-Client` header (403) → validation (400)
  → ownership (404). Unauthenticated requests never reach a repository (REQ-AUTH-004 AC-1).
- **Logout**: delete the session row, clear the cookie, 204 (REQ-AUTH-004 AC-3).
- **SPA**: on load, `GET /api/v1/auth/session`; `RequireAuth` sends signed-out users to `/login?returnTo=<path>`
  (REQ-AUTH-004 AC-2). The client wrapper treats any 401 other than from login as "session ended": it clears the
  user and navigates to `/login` with "Your session has ended. Please log in again." (AC-4). Signed-in users
  visiting `/login` or `/signup` go to `/todos`.

## 5. Validation (server authoritative, SPA mirrors; NFR-004 AC-2)

"Characters" means Unicode code points everywhere (Java `codePointCount`, JS `[...s].length`), matching JSON
Schema `maxLength`.

- **Trimming** (identifier, title, search text): remove leading and trailing characters for which Java
  `Character.isWhitespace` is true (`String.strip()`); this server rule is authoritative and the SPA mirrors it with
  an equivalent character set, not JS `trim()`. A non-breaking space (U+00A0) is therefore not trimmed.
- **Identifier** (REQ-AUTH-001 AC-7..AC-10): trimmed; 3–254 characters.
  Contains "@" → email; otherwise username of `[A-Za-z0-9._-]` only.
- **Email rule** (finding 12, AC-9), ASCII only: exactly one "@"; local part 1–64 characters from `A–Z a–z 0–9`
  and ``! # $ % & ' * + / = ? ^ _ ` { | } ~ - .``, not starting or ending with "." and with no ".."; domain 1–253
  characters, at least two dot-separated labels, each label 1–63 characters of `A–Z a–z 0–9 -` not starting or
  ending with "-", the last label letters only and at least 2 long. No quoted local parts, comments, IP literals or
  non-ASCII addresses. Because identifiers are ASCII-only, lower-casing for uniqueness is unambiguous.
- **Password** (AC-3, AC-5, AC-11): 8–128 characters, not trimmed or normalised (finding 10, ADR-0005).
- **Title** (REQ-TODO-002 AC-1..AC-3): trim; 1–200 characters after trimming.
- **Description** (AC-4): ≤ 2000 characters measured as sent; absent, null, empty or whitespace-only → stored as
  NULL; otherwise stored unchanged (REQ-TODO-001 AC-3, REQ-TODO-004 AC-4).
- **Due date** (AC-5..AC-7, AC-9): required; `YYYY-MM-DD` parsed with `ResolverStyle.STRICT` (so `2026-02-30` and
  free text fail, code `INVALID_DATE`); year 0001–9999; past dates accepted. The SPA uses `<input type="date">`,
  keeps the value as a string and formats it from its parts; it never passes a due date through `new Date(string)`
  or a local-time conversion, so the calendar date is the same in every time zone (AC-7).
- **Priority** (AC-8): `LOW|MEDIUM|HIGH`, case-sensitive; omitted or null on create → `MEDIUM` (REQ-TODO-001 AC-2);
  required on update (ADR-0006). The SPA offers a three-option select.
- **Bodies**: unknown properties → 400 field error `UNKNOWN_FIELD`; wrong JSON type for a field → `INVALID_TYPE`;
  unparseable JSON → 400 `MALFORMED_REQUEST`. All field errors for a request are reported together.

## 6. Error model

RFC 9457 `application/problem+json` with `code` and `errors[{field, code, message}]` exactly as the contract's
`Problem` schema; status codes per operation in the contract. `ProblemDetailsAdvice` is the only place responses for
errors are built. Unexpected exceptions → 500 `INTERNAL_ERROR` with a fixed sentence; the stack trace is logged with
the request id and never returned; no SQL, exception text, password or hash appears in any response
(NFR-004 AC-2, REQ-AUTH-003 AC-3). The SPA shows `errors[].message` beside the named field (REQ-TODO-002 AC-10) and
a not-found message for 404 (REQ-TODO-004 AC-3, REQ-TODO-006 AC-4).

## 7. To-do operations, list semantics and isolation

Timestamps: an injected `Clock`, millisecond precision. Create sets `created_at = updated_at = now`. Every accepted
update or done change sets `updated_at = max(now, previous + 1 ms)` so it strictly advances; `created_at` never
changes (REQ-TODO-004 AC-1, REQ-TODO-005 AC-1, AC-2).

**Last write wins** (finding 11, REQ-DATA-002, ADR-0006): `PUT …/{todoId}` replaces all content fields (title,
description, due date, priority); `PUT …/{todoId}/done` sets only `done`. No version check, no conflict error.
Each write is one statement; edits to different to-dos touch different rows (AC-2).

**List** (`listTodos`):
- Filter: `all` (default) | `active` (`done = 0`) | `completed` (`done = 1`) (REQ-LIST-001).
- Search (REQ-LIST-002): `q` trimmed; empty → no restriction. Otherwise lower-cased with `Locale.ROOT` and matched
  with `instr(title_search, :q) > 0 OR instr(description_search, :q) > 0`, a literal substring test, so `%`, `_`,
  `\` and quotes match themselves (AC-6). Combined with the filter by AND (AC-3).
- Order: `created_at DESC, id DESC` — a total order, so pages never repeat or skip (REQ-TODO-003 AC-1, REQ-LIST-003 AC-2).
- Page size fixed at 20; no size parameter (AC-8). `totalItems` = matches; `totalPages = ceil(totalItems / 20)`.
- **Page past the last** (finding 5, AC-6): the API itself clamps — effective page = `min(requested, totalPages)`,
  or 1 when `totalItems = 0` — and returns 200 with that page in `page`. The `page` parameter is validated as a
  string matching `^0*[1-9][0-9]*$`; values too large for a `long` are treated as "beyond the last page" rather than
  refused. Zero, negative and non-numeric values → 400 field `page` (AC-7).
- `totals {all, active, completed}` ignore `status`, `q` and `page`. Counts and the page are read in one read
  transaction so they agree.

**Clear completed** (`clearCompletedTodos`, REQ-TODO-007, REQ-ISO-001 AC-3): `DELETE FROM todos WHERE user_id = ?
AND done = 1`, returning `deletedCount`; when nothing is completed the API returns 200 with `deletedCount: 0`
(finding 4); the SPA disables the control when `totals.completed = 0` (AC-4).

**Isolation** (REQ-ISO-001, REQ-AUTH-005): the owner comes only from the session, never from the request (bodies
have no owner field and unknown fields are refused). Every `TodoRepository` method takes the owner as a required
parameter and every statement filters on it in the same statement as the id:

```java
Optional<Todo> findByPublicId(long userId, String publicId);
TodoPageResult list(long userId, TodoQuery query);
TodoTotals totals(long userId);
Todo insert(long userId, NewTodo todo);
boolean updateContent(long userId, String publicId, TodoContent content, Instant updatedAt);
boolean updateDone(long userId, String publicId, boolean done, Instant updatedAt);
boolean delete(long userId, String publicId);
int deleteCompleted(long userId);
```

`update*`/`delete` run `… WHERE public_id = ? AND user_id = ?` and report whether one row changed; `false` → 404.
A missing id, a malformed id and another user's id therefore give the identical 404 with no read-then-write gap
(REQ-ISO-001 AC-2). There is no endpoint that lists users or reads across users (REQ-AUTH-005 AC-1).

## 8. SPA behaviour

- Routes: `/signup`, `/login`, `/todos?status=&q=&page=`, `/todos/new`, `/todos/:id` (detail: description, created
  and updated timestamps in the viewer's local time, REQ-TODO-003 AC-3), `/todos/:id/edit`.
- List rows show title, done status (checkbox), due date and priority as text (REQ-TODO-003 AC-1).
- Empty states: `totals.all = 0` with filter All and no search → first-use message with an "Add a to-do" link
  (REQ-TODO-003 AC-2); any other `totalItems = 0` (a filter other than All, or a search, is applied) → "No matching
  to-dos" (REQ-LIST-001 AC-4, REQ-LIST-002 AC-4).
- Filter or search change → URL `page` removed (page 1) (REQ-LIST-003 AC-5). Search is submitted with Enter or the
  Search button; the SPA trims `q` and omits it when empty.
- **URL page values** (finding 6): the SPA normalises the URL before calling the API. A missing, zero, negative or
  non-numeric `page` is replaced (history replace) with page 1; the API's 400 for such values is for direct API
  callers. When the response's `page` differs from the URL (past the last page), the SPA replaces the URL with the
  returned page (REQ-LIST-003 AC-6, REQ-TODO-006 AC-5).
- Pagination: Previous, numbered links (first, last, current ±2, with ellipses), Next; Previous disabled on page 1,
  Next disabled on the last page; hidden when `totalPages ≤ 1` (REQ-LIST-003 AC-1, AC-3, AC-4).
- After create: navigate to `/todos` (All, no search, page 1) where the new to-do is first (REQ-TODO-001 AC-1).
  After done toggle, delete or clear: re-query the current URL's list immediately (REQ-TODO-005 AC-3).
- Delete: `ConfirmDialog` naming the to-do; Cancel changes nothing (REQ-TODO-006 AC-1..AC-3).
- Clear completed: before opening the dialog the SPA re-fetches the list and shows `totals.completed` in the text:
  "Delete all N completed to-dos? This deletes all your completed to-dos, including any not shown because of the
  current page, filter or search. This cannot be undone." (REQ-TODO-007 AC-1..AC-3).
- Log out control in the header (REQ-AUTH-004 AC-3).

## 9. Accessibility (NFR-001)

Semantic HTML first: real `<button>`, `<a>`, `<form>`, `<label for>`; the filter is a `fieldset` of radio inputs;
pagination is `<nav aria-label="Pagination">` with `aria-current="page"`. Field errors: `aria-invalid="true"` and
`aria-describedby` pointing at the message; on a refused submit, an error summary with `role="alert"` receives focus
and links to each field (AC-3). Dialogs: native `<dialog>` with `showModal()`, labelled by its heading, initial focus
on Cancel, Escape cancels, focus returns to the invoking control or, if that row was deleted, to the list heading
(AC-2). A polite live region announces list results ("Showing 1–20 of 45"). Visible focus outline on every
control; text contrast ≥ 4.5:1; priority and status never conveyed by colour alone; each route sets `document.title`
and moves focus to the page `<h1>` on navigation.

## 10. Observability

Structured (JSON) logs per request: request id (generated by `RequestIdFilter`, held in the logging context only and
not returned to the client), method,
route template, status, duration, internal user id when authenticated. Never logged: request or response bodies,
identifiers, passwords, hashes, cookies, session tokens, titles, descriptions, search text (REQ-AUTH-003 AC-3).
Unexpected exceptions are logged with stack trace and request id. No metrics or tracing (local only).

## 11. Local run (NFR-002, REQ-DATA-001 AC-3)

Prerequisites: JDK 21, Node.js (current LTS, pinned in `web/.nvmrc`), pnpm.

- API: `cd api && ./mvnw spring-boot:run` → `http://localhost:8080`. Creates `TODO_DB_PATH` (default
  `./data/todo.db`) and its parent directory on first start, then runs the Flyway migration. `api/data/` is git-ignored.
- SPA: `pnpm -C web install` then `pnpm -C web dev` → `http://localhost:5173`, proxying `/api` to `:8080`.
- Settings (`.env.example`, placeholders only, no secrets needed): `TODO_DB_PATH`, `TODO_COOKIE_SECURE=false`,
  `SERVER_PORT=8080`. No cloud service, hosted database or email service; no `dev`/`test` deployment.

## 12. Design decisions settling the requirements critic's findings

| Finding | Decision | Serves | Where |
|---|---|---|---|
| 1 Login identifier trimming | Trimmed exactly like sign-up, then compared by lower-cased key; no format check at login | REQ-AUTH-002 AC-3, AC-5 | §4, contract `logIn` |
| 2 Session lifetime | 8 hours absolute from login; activity does not extend it | REQ-AUTH-002 AC-4, REQ-AUTH-004 AC-4 | §4, ADR-0004 |
| 4 Clear-completed with nothing completed | 200 `{deletedCount: 0}`; UI control disabled | REQ-TODO-007 AC-4 | §7, contract |
| 5 Page past the last (API) | API clamps to the last page, 200, effective page in `page`; page 1 with no items when nothing matches | REQ-LIST-003 AC-6, REQ-TODO-006 AC-5 | §7, contract `listTodos` |
| 6 Bad page in the UI URL | SPA replaces it with page 1 before calling the API; API still refuses it with 400 | REQ-LIST-003 AC-7 | §8 |
| 10 bcrypt 72-byte limit | Argon2id (m=19 MiB, t=2, p=1), whole password hashed, no pre-hash | REQ-AUTH-003, REQ-AUTH-001 AC-11 | ADR-0005 |
| 11 Last write wins scope | Whole-content replace on update; done set by its own operation | REQ-DATA-002 | §7, ADR-0006 |
| 12 Email rule | ASCII dot-atom local part ≤ 64, DNS-style domain ≤ 253 with letter TLD ≥ 2, one "@" | REQ-AUTH-001 AC-9 | §5 |

No requirement is changed by these decisions. Analyst defaults 1–6 are designed as written.

## 13. Risks

| Risk | Kind | Mitigation |
|---|---|---|
| Squad roster on `develop` lacks `java-engineer` and `react-engineer`; squad PR #2 adding them awaits the requester's merge (Q12). Build nodes cannot be claimed until then. | Delivery | Requester merges PR #2 before the build phase; spec work proceeds meanwhile |
| WCAG 2.1 AA cannot be proven by automated tools alone | Quality | axe checks plus a manual keyboard and screen-reader pass on the six screens (§14) |
| Session cookie not `Secure` on local HTTP; any hosting would need HTTPS and `TODO_COOKIE_SECURE=true` | Security | Local-only release (Q10); documented setting |
| No login throttling (not required); online guessing possible if ever exposed beyond localhost | Security | Local only; Argon2id cost slows guessing |
| Sign-up discloses that an identifier exists | Security | Accepted in REQ-AUTH-002 AC-3 |
| Simple lower-case mapping, not full case folding (e.g. "ß" vs "SS") in search | Functional | Accepted; ASCII and common accented letters match |
| One DB connection serialises requests | Performance | Ample for one local user and 500 to-dos; NFR-003 test guards it |
| Flyway's SQLite support packaging differs between majors | Build | java-engineer adds the SQLite module if the pinned major needs it (ADR-0003) |

## 14. Verification and traceability

Levels: **unit** (JUnit 5 validators/services with a fixed `Clock`; Vitest for SPA logic), **integration**
(Spring Boot + MockMvc against a fresh temporary SQLite file per test class), **contract** (every integration
response validated against `todo-api-v1.yaml` with swagger-request-validator, plus an endpoint-inventory test
comparing Spring's mapped routes with the contract's operations), **component** (Vitest + React Testing Library),
**e2e** (Playwright against the real API and SPA with a temporary DB), **a11y** (vitest-axe, @axe-core/playwright,
Playwright keyboard-only scripts, manual screen-reader pass). Test names start with the requirement ID and AC.

| Requirement | SDD § | Contract element | Verified by |
|---|---|---|---|
| REQ-AUTH-001 | §3, §4, §5 | `signUp`, `SignupRequest`, `SignupResult` | unit: `IdentifierRules`, email rule, `PasswordRules` boundaries (2/3/254/255 identifier, 7/8/128/129 password, code points); integration: create, 409 duplicate incl. case variants and concurrent duplicate, 400 per field; component: one identifier field, field messages; e2e: sign up then log in, no verification step |
| REQ-AUTH-002 | §4 | `logIn`, `getSession`, `LoginRequest`, `Session`, `sessionCookie` | integration: success, wrong password and unknown identifier give byte-identical 401 bodies, case-variant and whitespace-padded login, cookie attributes, expiry at login + 8 h with a fixed `Clock`; e2e: reload stays signed in |
| REQ-AUTH-003 | §3, §4, §6, §10; ADR-0005 | `SignupRequest.password`, `InternalError` (no hash in any schema) | integration: stored hash is `$argon2id$`, two equal passwords give different hashes, no password or hash in any response body or captured log line; unit: 100-character passwords differing only after byte 72 do not cross-verify |
| REQ-AUTH-004 | §4; ADR-0004 | `logOut`, `Unauthenticated`, `ClientHeader`, `ClientHeaderMissing` | integration: every to-do operation without, with expired, and with logged-out session → 401 and DB unchanged; 403 without `X-Todo-Client`; e2e: protected route redirects to login; expired session (API clock advanced) returns user to login with message |
| REQ-AUTH-005 | §3, §7 | whole contract (no user-listing operation), `User` | contract: endpoint inventory equals the contract (no user-listing or cross-user operation); integration: after migration the `users` table has no role or permission column, and two new accounts get identical responses on every operation |
| REQ-ISO-001 | §7 | `TodoId`, `NotFound`, `listTodos`, `clearCompletedTodos` | integration: two users; list/search/filter/page never return B's rows; get/update/done/delete on B's id → 404 identical to a random UUID and to a malformed id, B's row unchanged; clear completed deletes only A's; e2e: REQ-ACC-001 AC-2 |
| REQ-TODO-001 | §7, §8 | `createTodo`, `TodoCreate`, `Todo` | integration: create with/without priority (→ MEDIUM) and description, timestamps set, `done=false`, first in list; e2e: create then reload |
| REQ-TODO-002 | §5, §6 | `TodoCreate`, `TodoUpdate`, `DueDate`, `Priority`, `BadRequest`, `FieldError` | unit: `TodoValidator` boundaries (title 0/1/200/201 after trim, whitespace-only, description 2000/2001, `2026-02-30`, free text, past date, priority values); integration: each refusal names its field; component: date shown identically under `TZ=Pacific/Kiritimati` and `TZ=Pacific/Pago_Pago` |
| REQ-TODO-003 | §7, §8 | `listTodos`, `getTodo`, `TodoPage`, `TodoTotals` | integration: order by created desc with tie-break; component: row fields, first-use empty state, detail view fields |
| REQ-TODO-004 | §5, §7; ADR-0006 | `updateTodo`, `TodoUpdate`, `NotFound` | integration: whole-content replace, `updatedAt` advances and `createdAt` unchanged, invalid edit leaves row unchanged, 404 on deleted id with no row created, clearing description; e2e: edit flow |
| REQ-TODO-005 | §7, §8; ADR-0006 | `setTodoDone`, `TodoDoneUpdate` | integration: done true/false, `updatedAt` strictly advances within the same millisecond; e2e: toggling under Active removes the row without reload |
| REQ-TODO-006 | §7, §8 | `deleteTodo`, `NotFound`, `listTodos` (page clamp) | component: confirm and cancel; integration: delete then 404 on update and delete; e2e: deleting the only item on the last page shows the previous page; restart persistence covered by REQ-DATA-001 |
| REQ-TODO-007 | §7, §8 | `clearCompletedTodos`, `ClearCompletedResult`, `TodoTotals.completed` | integration: deletes all completed across pages regardless of params, keeps active, 200 `deletedCount: 0` when none; component: dialog text with count and scope, disabled when `totals.completed = 0`, cancel deletes nothing |
| REQ-LIST-001 | §7, §8 | `listTodos` `status` | integration: all/active/completed, default all, invalid status → 400; component: "No matching to-dos" vs first-use state, including a user with no to-dos who selects Completed or searches (sees "No matching to-dos") |
| REQ-LIST-002 | §5, §7, §8; ADR-0003 | `listTodos` `q` | integration: case-insensitive title and description match, `%`, `_`, `\`, `'`, `"` literal, whitespace-only = no search, search AND filter; e2e: search flow |
| REQ-LIST-003 | §7, §8 | `listTodos` `page`, `TodoPage` | integration: 45 items → pages of 20/20/5, no repeats or gaps, page 99 and a 30-digit page → last page, no matches → page 1 empty, `0`/`-1`/`abc`/`1.5` → 400, `pageSize` ignored; component: controls disabled/hidden, filter change resets page, bad URL page → page 1; e2e: paging flow |
| REQ-DATA-001 | §3, §11; ADR-0003 | — (storage, no interface) | integration: write, close the Spring context, start a new context on the same file, data unchanged; start with no file and no directory creates both; e2e: REQ-ACC-001 AC-3 restarts the API process |
| REQ-DATA-002 | §7; ADR-0006 | `updateTodo`, `setTodoDone` | integration: two sequential content writes → second stored, no error; done and content writes from "two tabs" do not undo each other; concurrent writes to different to-dos both stored |
| REQ-ACC-001 | §4–§8 | all `todos` operations | e2e: one scenario for AC-1 (add, see, search, filter, edit, complete, delete), AC-2 (second user by list, search and direct URL), AC-3 (API restart, log in again) |
| NFR-001 | §9; ADR-0001 | `FieldError.message` | a11y: axe (WCAG 2.1 A/AA rules) zero violations on sign-up, login, list, create/edit form, delete dialog, clear-completed dialog; keyboard-only Playwright scripts for every action incl. dialog focus in/out; component: error `aria-describedby` and alert summary; manual screen-reader and contrast pass recorded by web-qa |
| NFR-002 | §2, §11 | `servers` (localhost) | e2e: the suite starts the API and SPA with the documented §11 commands and talks only to `localhost`; review: README run steps on a fresh checkout, no cloud/email/hosted-DB configuration, no deployment workflow |
| NFR-003 | §3 (indexes), §7; ADR-0003 | `listTodos` and all write operations | e2e (performance spec): user seeded with 500 to-dos; each of list, filter, search, page, create, edit, complete, delete run 20 times, time from action to rendered result, p95 ≤ 1 s |
| NFR-004 | §4, §5, §6, §11; ADR-0004, ADR-0005 | `Problem`, `BadRequest`, `InternalError`, `additionalProperties: false` | review + integration: server refuses invalid input sent directly (all REQ-TODO-002/REQ-AUTH-001 cases via API), forced internal error returns generic 500 with no stack trace or SQL; secret scan of the repository in the gates; AC-1 via REQ-AUTH-003 tests and ADR-0005 review |
| NFR-005 | §2; ADR-0001..0003 | whole contract (HTTP API) | review: `web/package.json` (React, TypeScript), `api/pom.xml` (Spring Boot), sqlite-jdbc driver and `.db` file; e2e exercises browser → HTTP API |
