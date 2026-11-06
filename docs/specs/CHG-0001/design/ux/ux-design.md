---
stage: design-ux
changeId: CHG-0001
node: N06-design-ux
agent: ux-designer
upstream:
  - docs/specs/CHG-0001/requirements.md
  - docs/specs/CHG-0001/sdd.md
  - docs/specs/CHG-0001/contracts/todo-api-v1.yaml
  - docs/specs/CHG-0001/adr/0001-react-typescript-spa.md
  - .harness/changes/CHG-0001/clarifications.md
companion: tokens.json (same directory)
---

# UX design — CHG-0001 To-do list

This document designs the screens, states, components, copy and accessibility behaviour of the React SPA described
in the frozen spec. It adds no behaviour: every screen, state and component cites the requirement (REQ-/NFR-) and
acceptance criterion (AC-n) it serves. Anything the spec leaves open is listed in section 11 as an open question and
is *not* decided silently; where a design choice was unavoidable it is marked **[proposed]** and also listed there.

Conventions: "AC-n" always refers to the requirement named in the same row or heading. SDD references are `sdd §n`.
Copy in quotation marks is the exact text to render unless marked as a template (`{n}`, `{title}`).

## 0. Provenance and tooling

- **Figma:** no Figma MCP tools were available to this run, there is no design-system file named by the config, and
  the repository has no existing design system, `design/tokens.json` or UI catalogue (checked 2026-11-06). No Figma
  frames, variables or Code Connect mappings were read or created. Nothing in this document claims otherwise. The
  design is delivered as Markdown plus `tokens.json`; the token names map one-to-one to Figma variables
  (`color.text.primary` to `color/text/primary`) if a file is configured later.
- **Component reuse:** there are no existing components to reuse (the repo has no product code, sdd §2). Section 5
  therefore lists the new components, each with the reason none existed. ADR-0001 forbids a component library, so
  components are native-HTML based.
- **Tokens location:** the designer role normally updates `design/tokens.json` at the repository root; this node's
  output is restricted to `docs/specs/CHG-0001/design/ux/`, so the tokens are `tokens.json` here. The react-engineer
  should copy or generate the CSS custom properties (`--color-text-primary` etc.) from this file.
- **WCAG version:** the task asks for WCAG 2.2 AA; NFR-001 AC-1 states WCAG 2.1 AA. 2.2 AA is a superset except that
  2.2 removes 4.1.1 Parsing. This design meets 2.2 AA, which satisfies NFR-001 as written. See section 9.

## 1. Users, tasks, scope

One user type (REQ-AUTH-005 AC-2; no admin). Tasks: sign up, log in, log out, add / see / search / filter / page /
edit / complete / delete a to-do, clear completed (REQ-ACC-001 AC-1). Single light theme, desktop and mobile web,
viewport 320 CSS px and wider (NFR-001; WCAG 1.4.10). No dark theme, no sorting control, no page-size control, no
password reset, no email verification, no admin or sharing UI (requirements "Out of scope").

## 2. Routes and user flows

| Route | Screen | Guard | Spec |
|---|---|---|---|
| `/signup` | S1 Sign up | public; signed-in visitor redirected to `/todos` | REQ-AUTH-001; sdd §4.1 |
| `/login` | S2 Log in | public; signed-in visitor redirected to `/todos` | REQ-AUTH-002; sdd §4.1 |
| `/todos?status=&q=&page=` | S3 My to-dos (list) | `RequireAuth` | REQ-TODO-003, REQ-LIST-001..003 |
| `/todos/new` | S4 Add a to-do | `RequireAuth` | REQ-TODO-001, REQ-TODO-002 |
| `/todos/:id` | S5 To-do detail | `RequireAuth` | REQ-TODO-003 AC-3 |
| `/todos/:id/edit` | S6 Edit a to-do | `RequireAuth` | REQ-TODO-004 |
| (overlay on S3, S5) | D1 Delete confirmation | n/a | REQ-TODO-006 |
| (overlay on S3) | D2 Clear-completed confirmation | n/a | REQ-TODO-007 |
| (any route, before probe resolves) | G1 App shell and session probe states | n/a | REQ-AUTH-002 AC-4; sdd §4.1 |

The six screens of NFR-001 AC-1 are S1, S2, S3, S4/S6 (the create/edit form, one screen with two modes), D1, D2; S5 is
the seventh checked screen (sdd §9, §14).

### 2.1 Flow: first visit, sign-up, login (REQ-AUTH-001, REQ-AUTH-002, REQ-AUTH-004)

```
Visitor opens /todos (signed out) --RequireAuth--> /login?returnTo=/todos   (no message)   [AUTH-004 AC-2]
Visitor opens /signup directly ------------------> S1 (no message, no redirect)            [AUTH-001 AC-1; sdd §14]
S1 submit valid --201--> /login with "Account created. Log in." (router state, not URL)    [AUTH-001 AC-1, AC-4]
S2 submit valid --200--> safeReturnTo(returnTo) else /todos                                 [AUTH-002 AC-1; sdd §4.2]
S2 submit wrong/unknown --401--> same page, generic alert, password cleared                 [AUTH-002 AC-2, AC-3]
Signed-in reload on /todos ---> G1 blank-until-probe, then S3 (no redirect)                 [AUTH-002 AC-4]
Log out (header) --> /login, no message                                                     [AUTH-004 AC-3]
Any call returns 401 while signed in --> /login?returnTo=... with "Your session has ended. Please log in again."  [AUTH-004 AC-4]
Reload after expiry (cookie gone) on a to-do route --> /login, no message                   [sdd §4.1; Q35]
```

### 2.2 Flow: managing to-dos (REQ-TODO-001..007, REQ-LIST-001..003)

```
S3 -> "Add a to-do" -> S4 -> submit valid -> /todos (All, no search, page 1; new item first)    [TODO-001 AC-1; sdd §8]
S3 -> title link -> S5 -> "Edit" -> S6 -> save valid -> S5 (values and Updated time shown)      [TODO-004 AC-1] [proposed]
S3 -> Edit link  -> S6
S3 checkbox toggle -> PUT done -> re-query current URL -> row leaves Active view if now done    [TODO-005 AC-3]
S3 / S5 "Delete" -> D1 -> Confirm -> deleted, list re-queried (last valid page if page empties) [TODO-006 AC-1..AC-5]
S3 "Clear all completed" -> re-fetch -> D2 with count -> Confirm -> all completed deleted       [TODO-007 AC-1..AC-3]
S3 filter/search change -> URL page removed -> page 1                                           [LIST-003 AC-5]
```

## 3. Layout shell (G1) — all authenticated and public screens

Serves: REQ-AUTH-004 AC-3 (Log out in header), NFR-001 AC-2 (landmarks, skip link), REQ-AUTH-002 AC-4 (probe).

- **Skip link** "Skip to main content" is the first focusable element; visible on focus; targets `<main id="main">`.
- **Header** (`<header>`): app name "To-do list" (plain text, not a heading). When `signed-in`: the user's identifier
  as text ("Signed in as {identifier}") and a **Log out** button (secondary style). Public routes: header shows the
  app name only. Log out is a `<button>`, 44 px minimum target.
- **Main** (`<main>`): content column `size.content.maxWidth` (720) centred; auth screens `size.content.authMaxWidth`
  (420). Page padding `space.inset.md` (16) at 320 px.
- `<html lang="en">`. Each route sets `document.title` to "{Screen name} - To-do list" and moves focus to the page
  `<h1>` (tabindex -1) on navigation (sdd §9). Titles: "Sign up", "Log in", "My to-dos", "Add a to-do",
  "{title}" (detail), "Edit to-do".

States of the session probe (sdd §4.1):

| State | Rendered | Spec |
|---|---|---|
| `unknown`, in flight, to-do route | nothing in `<main>` (header only); no redirect | REQ-AUTH-002 AC-4 |
| `unknown`, in flight, `/login` or `/signup` | nothing in `<main>` until resolved | sdd §4.1 |
| probe failed (network error / 500) | in `<main>`: alert (role="alert") "The app could not reach the server." + button **Retry**. Focus moves to the alert. Retry re-runs the probe; state stays `unknown`; never redirects or clears a user | sdd §4.1 |
| `signed-out` on a to-do route | immediate replace to `/login?returnTo=...`, no message | REQ-AUTH-004 AC-2 |

## 4. Screens

Common form rules (S1, S2, S4, S6) — NFR-001 AC-3, REQ-TODO-002 AC-10, REQ-AUTH-001 AC-3:

1. Every field has a visible `<label for>`. Required fields say "(required)" in the label text; optional says
   "(optional)". No placeholder carries essential information.
2. Validation runs on submit (SPA mirror of the server rules; server remains authoritative and its
   `errors[].message` is shown beside the named `field`). Fields are not validated on blur, to avoid announcing errors
   while the user is still typing.
3. On a refused submit: an **error summary** appears at the top of the form: `role="alert"`, heading "There is a
   problem", a list of links, one per error, each link moving focus to its field. Focus moves to the summary. Each
   invalid field gets `aria-invalid="true"`, `aria-describedby` = its message id, a red border (`color.border.danger`)
   and the message text directly under the label/field, prefixed visibly with "Error:" so colour is never the only cue
   (WCAG 1.4.1, 3.3.1). Message text colour `color.text.danger`.
4. Fixing a field and re-submitting rebuilds the summary; successful submit removes it.
5. Submit button shows a busy state while the request is in flight: label unchanged, `aria-disabled="true"` (not
   native disabled, to keep focus), repeated activation ignored; text "Saving..." / "Creating account..." /
   "Logging in..." replaces the label. No spinner animation is required.
6. Any non-field failure (500, network error, 403 `CLIENT_HEADER_MISSING`, 415, other 4xx with `code` not
   `VALIDATION_FAILED`) shows a form-level alert above the form: "Something went wrong. Please try again." (copy
   **[proposed]**; sdd §6 requires only that the SPA never show internals, NFR-004 AC-2). The user's input is kept.
7. A 401 on any request while `signed-in` is not a form error: the session-ended flow of section 2.1 takes over.
   (Exception: S2's own 401 is invalid credentials.)

### Validation message catalogue

Server `errors[].message` is authoritative and is displayed when present. The strings below are the SPA-mirror copy
**[proposed]** and should equal the server's wording so the user sees identical text from either source (open
question Q-UX-2).

| Field | Rule (source) | Message |
|---|---|---|
| identifier (S1) | empty after trim (AUTH-001 AC-3) | "Enter an email address or a username." |
| identifier (S1) | trimmed length < 3 or > 254 (AC-8) | "The email address or username must be 3 to 254 characters." |
| identifier (S1) | contains "@" and not a valid email (AC-9) | "Enter a valid email address, for example name@example.com." |
| identifier (S1) | no "@" and has a character outside A-Z a-z 0-9 . _ - (AC-10) | "A username can contain only letters A to Z, digits 0 to 9, and the characters . _ -" |
| identifier (S1) | 409 `IDENTIFIER_TAKEN`, field error `TAKEN` (AC-2, AC-6) | "This email address or username is already taken. Choose a different one, or log in." |
| password (S1) | empty (AC-3) | "Enter a password." |
| password (S1) | < 8 (AC-5) | "The password must be at least 8 characters." |
| password (S1) | > 128 (AC-11) | "The password must be 128 characters or fewer." |
| identifier (S2) | empty after trim (400, AUTH-002 sdd §4) | "Enter your email address or username." |
| password (S2) | empty | "Enter your password." |
| title (S4/S6) | empty or whitespace-only (TODO-002 AC-2) | "Enter a title." |
| title (S4/S6) | trimmed > 200 (AC-3) | "The title must be 200 characters or fewer." |
| description (S4/S6) | > 2000 (AC-4) | "The description must be 2000 characters or fewer." |
| dueDate (S4/S6) | empty (AC-5) | "Enter a due date." |
| dueDate (S4/S6) | not a real date (AC-6) | "Enter a real date, for example 2026-11-06." |

"Characters" are Unicode code points; whitespace is the sdd §5 set (so a title of only non-breaking spaces gets
"Enter a title."). Priority has no UI error: the select offers only Low, Medium, High (AC-8). Past due dates are
accepted with no warning (AC-9).

---

### S1 Sign up — `/signup`

Serves: REQ-AUTH-001 AC-1..AC-11; NFR-001 AC-1..AC-3.

Layout (single column, 420 max): `<h1>` "Sign up"; form; below the form a text link "Already have an account? Log in"
to `/login`.

Fields:

| Order | Label | Control | Notes (AC) |
|---|---|---|---|
| 1 | "Email address or username (required)" | `<input type="text" autocomplete="username" autocapitalize="none" spellcheck="false">` | One field accepts either (AUTH-001 AC-7). Help text under label: "Use an email address, or a username of letters, digits, . _ -. 3 to 254 characters." (id for `aria-describedby`, combined with error id when invalid) |
| 2 | "Password (required)" | `<input type="password" autocomplete="new-password">` | Help text: "8 to 128 characters." (AC-5, AC-11). A **Show password** checkbox below the field toggles type to text (WCAG 3.3.8 accessible authentication aid; label "Show password"). No composition rules beyond length |

Submit button: "Sign up" (primary). No email-verification text, no confirm-password field (not in spec).

| State | Behaviour and copy | Spec |
|---|---|---|
| Default | Empty form, focus on `<h1>` on arrival | AUTH-001 AC-1 |
| Visitor opened directly while signed out | Form shown, no message, no redirect | sdd §4.1, §14 |
| Signed in visitor | Replaced by redirect to `/todos` | sdd §4.1 |
| Validation errors (client or 400) | Error summary + inline messages from the catalogue; no account created | AC-3, AC-5, AC-8..AC-11; NFR-001 AC-3 |
| Identifier taken (409), incl. case variants | Summary "There is a problem" with link "This email address or username is already taken..." to identifier; the entered password is kept | AC-2, AC-6 |
| Submitting | Button busy state | |
| Server/network error | Form-level alert, input kept | NFR-004 AC-2 |
| Success (201) | Navigate to `/login`, showing "Account created. Log in." (success message on S2); no automatic login, no verification step | AC-1, AC-4; sdd §4 |

### S2 Log in — `/login`

Serves: REQ-AUTH-002 AC-1..AC-5; REQ-AUTH-004 AC-2, AC-4; NFR-001.

Layout: `<h1>` "Log in"; optional message region directly under the `<h1>` (before the form); form; link "No account
yet? Sign up".

Fields: "Email address or username (required)" (`autocomplete="username"`), "Password (required)"
(`autocomplete="current-password"`, with **Show password** checkbox). No format hints and no format validation beyond
non-empty (sdd §4). Submit: "Log in".

| State | Behaviour and copy | Spec |
|---|---|---|
| Default (typed/bookmarked `/login`, redirect from RequireAuth, after logout, after reload-post-expiry) | No message | AUTH-004 AC-2; sdd §4.1 |
| After sign-up | Message (info surface, `role="status"`): "Account created. Log in." Delivered by router state, never URL | AUTH-001 AC-1; sdd §4.1 |
| Session ended while signed in | Message (info surface, `role="status"`): "Your session has ended. Please log in again." Form below; `returnTo` preserved in URL | AUTH-004 AC-4; sdd §4.1 |
| Empty field(s) | Error summary + inline "Enter your email address or username." / "Enter your password." | AUTH-002 sdd §4 (400) |
| Invalid credentials (401, wrong password, unknown identifier, password > 128) | One alert above the form, identical for all three: "Invalid identifier or password." (the API `detail`). Not attached to a single field, so it does not reveal which was wrong. Password field cleared and focus on the identifier field; the alert is announced (role="alert") | AUTH-002 AC-2, AC-3 |
| Submitting | Button busy state "Logging in..." | |
| Server/network error | Form-level alert (section 4 rule 6) | NFR-004 AC-2 |
| Success | Navigate with the router to `safeReturnTo(returnTo)` or `/todos`; no flash of the form | AUTH-002 AC-1; sdd §4.2 |
| Case-variant / padded identifier | Behaves as success; no UI difference | AUTH-002 AC-5 |

### S3 My to-dos (list) — `/todos`

Serves: REQ-TODO-003, REQ-LIST-001..003, REQ-TODO-005, REQ-TODO-006, REQ-TODO-007, REQ-ISO-001 AC-1/AC-4,
REQ-ACC-001, NFR-001, NFR-003.

Layout, top to bottom (within `<main>`):

1. `<h1>` "My to-dos".
2. Action row: link-styled primary button **Add a to-do** (to `/todos/new`) and **Clear all completed** (secondary
   button, danger text). Wraps on narrow widths.
3. Controls region `<section aria-label="Find to-dos">`:
   - **Search form** (`role="search"`): visible label "Search to-dos", `<input type="search">` pre-filled from `q`,
     button **Search**. Submits with Enter or the button (sdd §8). No search-as-you-type, no clear (x) icon (not in
     spec; the user empties the box and presses Search).
   - **Filter** `<fieldset>` with `<legend>` "Show": radio group **All**, **Active**, **Completed**; default All
     (LIST-001 AC-5). Changing a radio applies immediately (no apply button) and resets to page 1 (LIST-003 AC-5).
     Radios are rendered as a segmented control with visible text; selected state is a filled background *and* a
     checked radio dot (not colour alone).
4. **Result summary** (visible text and polite live region `role="status"`): "Showing {from}-{to} of {totalItems}"
   (sdd §9), e.g. "Showing 1-20 of 45". Not shown in empty states.
5. **To-do list** `<ul>` of rows (section 5, `TodoRow`).
6. **Pagination** `<nav aria-label="Pagination">` (section 5, `Pagination`); hidden when `totalPages <= 1`.

Row content (TODO-003 AC-1): checkbox, title (link to `/todos/:id`), "Completed" text badge when done, "Due {date}",
"Priority: {Low|Medium|High}", then **Edit** (link to `/todos/:id/edit`) and **Delete** (button). The description,
created and updated timestamps are not on the row; they are on S5 (TODO-003 AC-3). Ordering is fixed newest first
(TODO-003 AC-1); there is no sort control.

Date format: "{d} {Mon} {yyyy}" built from the `YYYY-MM-DD` string parts (for example "6 Nov 2026"), never through
`new Date(string)`, so it is the same calendar date in every time zone (TODO-002 AC-7). Month names are a fixed
English list.

States:

| State | Rendered (copy exact) | Spec |
|---|---|---|
| Loading (first load or any re-query) | In place of the list: text "Loading your to-dos..." with `role="status"`. Previous rows are removed during first load; on re-query the existing rows stay until the response arrives (no flicker) and the list region has `aria-busy="true"` | NFR-003 AC-1 |
| Populated | Rows, summary, pagination as above | TODO-003 AC-1 |
| First-use empty (`totals.all = 0`, filter All, no search) | Empty-state panel replaces the list (no table/list, no filter-no-match text): `<h2>` "You have no to-dos yet" + text "Add your first to-do to start keeping track of your tasks." + link-button **Add a to-do** (to `/todos/new`). Filter and search controls still shown; **Clear all completed** disabled | TODO-003 AC-2; sdd §8 |
| No matching (`totalItems = 0` with filter other than All, or a search applied; including a user with no to-dos who picks Completed or searches) | `<h2>` "No matching to-dos" + text "Try a different search or filter." + (if a search or filter is applied) link **Show all to-dos** that clears `status`, `q` and `page` | LIST-001 AC-4, LIST-002 AC-4, REQ-ISO-001 AC-4 |
| Search in another user's data only | Identical to "No matching to-dos"; reveals nothing | REQ-ISO-001 AC-4 |
| Page past last / bad page in URL | URL is replaced (history replace) with the effective page; the user sees the last valid page, no error. Bad `page` (0, negative, non-numeric) becomes page 1 | LIST-003 AC-6, AC-7 (UI normalisation: sdd §8); TODO-006 AC-5 |
| Single page (`totalPages <= 1`) | No pagination | LIST-003 AC-4 |
| Clear all completed disabled | `totals.completed = 0`: button natively `disabled`, muted style (`color.surface.muted`, `color.text.disabled`) | TODO-007 AC-4 |
| Row toggle in flight | The checkbox is `aria-disabled` until the response; no double-submit | TODO-005 |
| Toggle done under Active filter | After the 200, the list is re-queried; the row is gone. Live region says: "Marked done: {title}." Focus moves to the `<h1>`, see section 6 | TODO-005 AC-3 |
| Toggle failure (non-401) | Page-level alert above the list: "Could not update that to-do. Try again." (**[proposed]**); checkbox reverts to server state | NFR-004 AC-2 |
| 404 on toggle (to-do deleted elsewhere) | Same alert text "That to-do no longer exists." (**[proposed]**) and the list is re-queried | TODO-005, REQ-ISO-001 AC-2 |
| List fetch failure (non-401) | Alert in place of list: "Your to-dos could not be loaded." + button **Retry** (**[proposed]**) | NFR-004 AC-2 |
| Session ended (401 while signed in) | Navigates to S2 with the session-ended message | AUTH-004 AC-4 |

Filtered/searched view resets: the URL carries `status`, `q`, `page` (ADR-0001), so the Back button restores them.
The search text is displayed as the user typed it (the SPA submits the trimmed value; whitespace-only submits as no
search, LIST-002 AC-7). The `%`, `_`, quotes characters need no special UI (LIST-002 AC-6).

### D1 Delete confirmation (dialog)

Serves: REQ-TODO-006 AC-1..AC-5; NFR-001 AC-2.

Invoked from a row's **Delete** (S3) or the **Delete** button on S5. Native `<dialog>` opened with `showModal()`,
`aria-labelledby` the dialog heading.

- Heading `<h2>`: "Delete this to-do?"
- Body: "{title}" shown in quotes (truncate nothing; wraps), then "This permanently deletes the to-do. This cannot be
  undone." (REQ-TODO-006 AC-1, Q8: no undo.)
- Buttons: **Cancel** (secondary, first in DOM, **initial focus**) and **Delete** (danger).

| State | Behaviour | Spec |
|---|---|---|
| Open | Focus on Cancel; background inert; Escape = Cancel | TODO-006 AC-1; NFR-001 AC-2 |
| Cancel / Escape | Closes; nothing deleted; focus returns to the invoking control | TODO-006 AC-2 |
| Confirm, in flight | Delete button busy ("Deleting..."), `aria-disabled`; Cancel still works only before the request is sent | |
| Success (204) | Dialog closes; list re-queried (S3) or navigate to `/todos` (S5) **[proposed]**; live region "Deleted: {title}." If the last item of the last page was deleted the last valid page is shown. Focus goes to the `<h1>` because the invoking row no longer exists | TODO-006 AC-3, AC-5 |
| 404 (already deleted / not found) | Dialog closes; alert "That to-do no longer exists." and the list is re-queried | TODO-006 AC-4 |
| Other error | Dialog stays open; error text inside dialog above buttons, `role="alert"`: "The to-do could not be deleted. Try again." (**[proposed]**) | NFR-004 AC-2 |
| 401 | Session-ended flow | AUTH-004 AC-4 |

### D2 Clear-completed confirmation (dialog)

Serves: REQ-TODO-007 AC-1..AC-4.

Opening sequence (sdd §8): the click first re-fetches the list; the dialog opens with `totals.completed` from that
response. While the re-fetch runs, the button shows busy state. If the re-fetched count is 0 the dialog does not open;
the control becomes disabled and the live region says "There are no completed to-dos to clear." (**[proposed]**
handling of the race; no AC covers it).

- Heading: "Delete all completed to-dos?"
- Body, exactly the sdd §8 sentence with the count: "Delete all {N} completed to-dos? This deletes all your
  completed to-dos, including any not shown because of the current page, filter or search. This cannot be undone."
  Where N = 1 the word is singular: "Delete the 1 completed to-do? ..." is **not** specified by the spec; use the spec
  sentence as written for all N (open question Q-UX-6).
- Buttons: **Cancel** (initial focus), **Delete {N} completed** (danger).

| State | Behaviour | Spec |
|---|---|---|
| Open | As D1 | TODO-007 AC-1 |
| Cancel / Escape | Closes, nothing deleted, focus back to **Clear all completed** | TODO-007 AC-3 |
| Confirm in flight | Busy "Deleting..." | |
| Success (200 `deletedCount`) | Dialog closes; list re-queried (active to-dos untouched); live region "Deleted {deletedCount} completed to-dos."; if now `totals.completed = 0` the trigger is disabled, so focus goes to the `<h1>` | TODO-007 AC-2, AC-4 |
| Error | Inline `role="alert"` in dialog: "Completed to-dos could not be deleted. Try again." (**[proposed]**) | NFR-004 AC-2 |
| 401 | Session-ended flow | AUTH-004 AC-4 |

### S4 Add a to-do — `/todos/new` and S6 Edit a to-do — `/todos/:id/edit`

One form component (`TodoForm`), two modes. Serves: REQ-TODO-001 AC-1..AC-4, REQ-TODO-002 AC-1..AC-10,
REQ-TODO-004 AC-1..AC-4, REQ-DATA-002 (no conflict UI), NFR-001.

`<h1>`: "Add a to-do" (S4) / "Edit to-do" (S6).

Fields (common form rules apply):

| Order | Label | Control | Notes (AC) |
|---|---|---|---|
| 1 | "Title (required)" | `<input type="text">` | Trimmed server and client; 1-200 (TODO-002 AC-1..3). Help: "Up to 200 characters." |
| 2 | "Description (optional)" | `<textarea rows="5">` | Up to 2000 (AC-4). Clearing and saving removes the description (TODO-004 AC-4); whitespace-only = none. Help: "Up to 2000 characters." |
| 3 | "Due date (required)" | `<input type="date">` | Value kept as the `YYYY-MM-DD` string (TODO-002 AC-7). Required, cannot be cleared on edit (TODO-004 AC-4: an emptied date gets "Enter a due date." on save). Past dates allowed without warning (AC-9). Help: "Format: year-month-day." |
| 4 | "Priority" | `<select>` with options Low, Medium, High | Default **Medium** (TODO-001 AC-2); on edit the stored value. Always a valid value, no empty option (TODO-002 AC-8) |

Buttons: primary **Add to-do** (S4) / **Save changes** (S6); secondary **Cancel** (link: to `/todos` from S4, to
`/todos/:id` from S6; discards input, no confirm because the spec requires none).

| State | Behaviour and copy | Spec |
|---|---|---|
| S4 default | Empty form, priority Medium | TODO-001 AC-2 |
| S6 loading | `<h1>` "Edit to-do" and text "Loading..." (`role="status"`) in place of the form; no form until loaded | NFR-003 |
| S6 prefilled | Fields hold the to-do's current values; description empty if none | TODO-004 AC-1 |
| S6 not found (`getTodo` 404: deleted, malformed id, or another user's id) | In place of the form: `<h1>` "To-do not found" and text "This to-do does not exist or has been deleted." + link **Back to my to-dos**. Identical for every cause | REQ-ISO-001 AC-2; TODO-004 AC-3 |
| Validation errors | Section 4 rules; nothing saved; stored to-do unchanged | TODO-002 AC-2..AC-6, AC-10; TODO-004 AC-2 |
| Save in flight | Busy state ("Saving...") | |
| Save refused 404 (to-do deleted meanwhile) | Form-level alert: "This to-do no longer exists, so your changes could not be saved." with link **Back to my to-dos**; nothing created | TODO-004 AC-3 |
| Save error (other) | Form-level alert (section 4 rule 6); input kept | NFR-004 AC-2 |
| S4 success (201) | Navigate to `/todos` (All, no search, page 1); the new to-do is first; live region on arrival not needed (focus goes to `<h1>`) | TODO-001 AC-1; sdd §8 |
| S6 success (200) | Navigate to `/todos/:id` **[proposed]** showing new values and the advanced Updated time (spec does not say where to go; open question Q-UX-4) | TODO-004 AC-1 |
| Concurrent edit elsewhere | No conflict message ever; the last save wins silently | REQ-DATA-002 AC-1 |

### S5 To-do detail — `/todos/:id`

Serves: REQ-TODO-003 AC-3, REQ-ISO-001 AC-2, REQ-TODO-004/006 entry points, NFR-001 (seventh audited screen).

Layout: `<h1>` = the to-do's title. A definition list (`<dl>`) with visible terms:

| Term | Value |
|---|---|
| Status | "Active" or "Completed" |
| Due date | "{d} {Mon} {yyyy}" |
| Priority | "Low", "Medium", "High" |
| Description | the text, line breaks preserved (`white-space: pre-wrap`), or "No description" when none |
| Created | "{d} {Mon} {yyyy}, {HH:mm}" in the viewer's local time (sdd §8) |
| Updated | same format |

Actions: **Edit** (primary link-button to `/todos/:id/edit`), **Delete** (danger, opens D1), link **Back to my to-dos**
(to `/todos`). (A Mark done control is not placed here: the spec requires it on the list only through the row
checkbox, and adding it would add behaviour.)

| State | Behaviour and copy | Spec |
|---|---|---|
| Loading | `<h1>` "To-do" and text "Loading..." (`role="status"`) | NFR-003 |
| Loaded | As above | TODO-003 AC-3 |
| Not found (404, any cause) | `<h1>` "To-do not found"; text "This to-do does not exist or has been deleted."; link **Back to my to-dos** | REQ-ISO-001 AC-2 |
| Load error (non-401, non-404) | Alert "This to-do could not be loaded." + **Retry** (**[proposed]**) | NFR-004 AC-2 |
| Delete from here | D1; on success navigate to `/todos` **[proposed]** | TODO-006 |
| 401 | Session-ended flow | AUTH-004 AC-4 |

## 5. Component inventory

All components are new (no existing system; section 0). Each is native-HTML first (ADR-0001, sdd §9). Names follow
the SPA modules in sdd §2.

| Component | Used by | Structure and states | Serves |
|---|---|---|---|
| `AppShell` (skip link, header, main, Log out) | all | Signed-in / public header variants; Log out button | AUTH-004 AC-3; NFR-001 AC-2 |
| `SessionProbeError` | G1 | alert + Retry | AUTH-002 AC-4 |
| `FormField` (label, help, input/textarea/select, error) | S1, S2, S4, S6 | default, invalid (`aria-invalid`, border `color.border.danger`, "Error:" prefixed message), focus | NFR-001 AC-3; TODO-002 AC-10 |
| `ErrorSummary` | S1, S2, S4, S6 | `role="alert"`, heading, links to fields, receives focus | NFR-001 AC-3 |
| `Button` (primary, secondary, danger; busy via `aria-disabled`; disabled) | all | default, hover, focus-visible, busy, disabled | NFR-001 AC-2 |
| `LinkButton` (anchor styled as button) | "Add a to-do", "Edit", "Cancel", "Back" | default, hover, focus-visible | NFR-001 AC-2 |
| `Message` (info, success, danger variants; `role="status"` or `role="alert"`) | S2, S3 | text + variant surface; always a text prefix word where severity matters ("Error:") | AUTH-001 AC-1; AUTH-004 AC-4 |
| `ShowPasswordToggle` (checkbox) | S1, S2 | checked / unchecked | WCAG 3.3.8 aid |
| `SearchForm` | S3 | `role="search"`, labelled input, Search button | LIST-002 |
| `FilterGroup` (fieldset + 3 radios) | S3 | selected / unselected, focus | LIST-001 |
| `ResultSummary` (polite live region) | S3 | "Showing a-b of n" | NFR-001 AC-2; sdd §9 |
| `TodoList` / `TodoRow` | S3 | active, completed, toggling (`aria-disabled`), focus; stacked layout < 640, inline >= 640 | TODO-003 AC-1; TODO-005 |
| `PriorityText` | S3, S5 | text "Priority: High" etc. in `color.text.priority.*`; the word carries the meaning | NFR-001 (no colour-only) |
| `StatusBadge` ("Completed" text) | S3 | shown only for done rows | NFR-001 (no colour-only) |
| `EmptyState` (first-use, no-match variants) | S3 | heading, text, action | TODO-003 AC-2; LIST-001 AC-4 |
| `Pagination` | S3 | Previous, numbered links (first, last, current +-2, ellipsis "..." as non-interactive text), Next; current page `aria-current="page"` and visually distinct (bold, underlined, filled); disabled Previous/Next not focusable | LIST-003 AC-1..AC-4 |
| `ConfirmDialog` (native `<dialog>`) | D1, D2 | open, busy, error; initial focus Cancel; Escape cancels; focus return | TODO-006; TODO-007; NFR-001 AC-2 |
| `TodoForm` | S4, S6 | create / edit modes | TODO-001, 002, 004 |
| `TodoDetail` (`<dl>`) | S5 | loaded / not found / error | TODO-003 AC-3 |
| `LoadingText` | S3, S5, S6 | `role="status"` text "Loading..." | NFR-003 |

Why new: no component library is permitted (ADR-0001) and the repo has none.

### Component sizing and tokens

- Buttons, links-as-buttons, inputs, selects, checkboxes (hit area), radios (hit area), pagination links: minimum
  `size.target.min` (44 x 44 CSS px) including the checkbox/radio label hit area. WCAG 2.5.8 needs only 24; 44 is the
  design target (and avoids a spacing exception for dense rows).
- Spacing from `space.*`; row padding `space.inset.md`; gap between rows `space.stack.sm`; row boundary uses
  `color.border.default` (3:1) rather than `color.border.subtle`.
- Radius `radius.control` on controls, `radius.surface` on rows and dialogs; dialog elevation `elevation.dialog`.
- Typography: body 1rem; headings from `typography.heading1/2`; labels `typography.label`. Sizes in rem so 200% text
  zoom works.

### Responsive behaviour (WCAG 1.4.10 Reflow, 1.4.4 Resize text)

- Mobile (320-639 px): single column; row content stacks (checkbox + title; below: Completed badge, due date,
  priority; below: Edit, Delete); filter radios wrap; Search input full width with button below; dialog full width
  minus `space.inset.md`; pagination wraps onto two lines (Previous/Next on their own line is allowed).
- >= 640 px: row meta inline; Edit/Delete right-aligned; filter radios on one line; search input and button inline.
- No horizontal scrolling at 320 px wide or at 400% zoom for any screen (long titles and descriptions wrap with
  `overflow-wrap: anywhere`).

## 6. Focus and announcements (NFR-001 AC-2, AC-3)

| Event | Focus goes to | Announcement (live region) |
|---|---|---|
| Route change | page `<h1>` | (title via `document.title`) |
| List re-query after filter / search change | stays on the control the user used | "Showing a-b of n", or the empty-state heading text |
| Pagination link activated | `<h1>` "My to-dos" | "Showing a-b of n" |
| Toggle done/not done, row remains | stays on the checkbox | "Marked done: {title}." / "Marked not done: {title}." |
| Toggle done, row leaves the Active view | `<h1>` "My to-dos" | "Marked done: {title}." |
| D1 opens / D2 opens | Cancel | (dialog heading read by AT) |
| D1/D2 cancelled | invoking control | none |
| D1 confirmed on S3 | `<h1>` (row is gone) | "Deleted: {title}." |
| D2 confirmed | `<h1>` | "Deleted {n} completed to-dos." |
| Refused submit | Error summary | summary read (`role="alert"`) |
| S2 invalid credentials | identifier field | alert text |
| Probe error | the alert | alert text |

One polite live region (`role="status"`) in the app shell carries the toggle/delete/clear messages so they survive
removal of the invoking element. The list "Loading" state does not steal focus. Using `<h1>` as the fall-back focus
target satisfies sdd §9 ("focus returns to the invoking control or, if that row was deleted, to the list heading").

## 7. Interaction details

- Keyboard: Tab order follows DOM = visual order. Radio group by arrow keys (native). Dialog: Tab cycles inside
  (native modal), Escape cancels. Enter submits forms. No keyboard traps; no custom shortcuts.
- Pointer: hover colours use `color.action.*Hover`; hover alone never conveys state. No drag, no gestures
  (WCAG 2.5.1, 2.5.7 not applicable).
- Motion: none required. Any transition uses `motion.default` and `motion.reduced` under
  `prefers-reduced-motion: reduce` (WCAG 2.3.3 is AAA, honoured anyway).
- Text input behaviour: the client trims with the sdd §5 whitespace set when validating; the field text itself is not
  rewritten while typing.

## 8. Traceability: requirement to screen / state

| Requirement | Screen / component / state |
|---|---|
| REQ-AUTH-001 AC-1 | S1 success -> S2 "Account created. Log in." |
| REQ-AUTH-001 AC-2, AC-6 | S1 state "Identifier taken (409)" |
| REQ-AUTH-001 AC-3 | S1 validation errors (empty identifier / password), catalogue rows |
| REQ-AUTH-001 AC-4 | S1 has no verification step; success goes to S2 |
| REQ-AUTH-001 AC-5, AC-11 | S1 password field rules and messages |
| REQ-AUTH-001 AC-7 | S1 single identifier field |
| REQ-AUTH-001 AC-8, AC-9, AC-10 | S1 identifier messages (length, email, username) |
| REQ-AUTH-002 AC-1 | S2 success -> `/todos` / safe `returnTo` |
| REQ-AUTH-002 AC-2, AC-3 | S2 "Invalid credentials" single generic alert |
| REQ-AUTH-002 AC-4 | G1 probe states (blank until resolved, no redirect, error + Retry) |
| REQ-AUTH-002 AC-5 | S2 case-variant identifier = success (no UI difference) |
| REQ-AUTH-003 AC-1, AC-2 | no UI surface (storage); AC-3: no password or hash shown anywhere; password field masked, never echoed in messages |
| REQ-AUTH-004 AC-1 | no UI surface (API refusal) |
| REQ-AUTH-004 AC-2 | G1 / S2 "Default" (redirect from RequireAuth, no message) |
| REQ-AUTH-004 AC-3 | AppShell Log out -> S2 |
| REQ-AUTH-004 AC-4 | S2 "Session ended while signed in" |
| REQ-AUTH-005 AC-1, AC-2 | No user lists, no role UI, one user type in every screen |
| REQ-ISO-001 AC-1 | S3 shows only own to-dos (no UI distinction needed) |
| REQ-ISO-001 AC-2 | S5 / S6 "Not found" states (same for deleted, malformed, other user's); S3 404 on toggle; D1 404 |
| REQ-ISO-001 AC-3 | D2 (clears only own; copy "your completed to-dos") |
| REQ-ISO-001 AC-4 | S3 "No matching to-dos" |
| REQ-TODO-001 AC-1 | S4 success -> S3 new item first |
| REQ-TODO-001 AC-2 | S4 priority default Medium |
| REQ-TODO-001 AC-3 | S4 description optional; S5 shows description / "No description" |
| REQ-TODO-001 AC-4 | persistence (no UI surface); S3 shows item after reload |
| REQ-TODO-002 AC-1..AC-6 | S4/S6 fields and validation catalogue (title, description, due date) |
| REQ-TODO-002 AC-7 | Date format from string parts (S3, S5); `<input type="date">` string value |
| REQ-TODO-002 AC-8 | S4/S6 priority select with three options |
| REQ-TODO-002 AC-9 | S4/S6 accept past dates, no warning |
| REQ-TODO-002 AC-10 | ErrorSummary + inline message naming the field |
| REQ-TODO-003 AC-1 | S3 TodoRow fields and order |
| REQ-TODO-003 AC-2 | S3 "First-use empty" |
| REQ-TODO-003 AC-3 | S5 detail fields |
| REQ-TODO-004 AC-1 | S6 success -> S5 |
| REQ-TODO-004 AC-2 | S6 validation errors, stored to-do unchanged |
| REQ-TODO-004 AC-3 | S6 "Save refused 404" / "not found" |
| REQ-TODO-004 AC-4 | S6 clearing description OK; due date emptied -> "Enter a due date." |
| REQ-TODO-005 AC-1, AC-2 | S3 row checkbox toggle done / not done |
| REQ-TODO-005 AC-3 | S3 "Toggle done under Active filter" (re-query, row gone) |
| REQ-TODO-006 AC-1..AC-3 | D1 open / cancel / confirm |
| REQ-TODO-006 AC-4 | D1 404 state; S5/S6 not-found |
| REQ-TODO-006 AC-5 | D1 success -> last valid page; S3 "Page past last" |
| REQ-TODO-007 AC-1 | D2 text with count and scope |
| REQ-TODO-007 AC-2, AC-3 | D2 confirm / cancel |
| REQ-TODO-007 AC-4 | S3 "Clear all completed disabled" |
| REQ-LIST-001 AC-1..AC-3, AC-5 | S3 FilterGroup (default All) |
| REQ-LIST-001 AC-4 | S3 "No matching to-dos" (distinct from first-use) |
| REQ-LIST-002 AC-1..AC-3, AC-5 | S3 SearchForm (combined with filter) |
| REQ-LIST-002 AC-4 | S3 "No matching to-dos" |
| REQ-LIST-002 AC-6, AC-7 | S3 search: literal characters, whitespace-only = no search (no UI difference) |
| REQ-LIST-003 AC-1..AC-4 | S3 Pagination component states |
| REQ-LIST-003 AC-5 | S3 filter/search change removes `page` |
| REQ-LIST-003 AC-6, AC-7, AC-8 | S3 "Page past last / bad page"; no page-size control |
| REQ-DATA-001 AC-1..AC-3 | no UI surface |
| REQ-DATA-002 AC-1, AC-2 | S6 "Concurrent edit elsewhere": no conflict UI |
| REQ-ACC-001 AC-1..AC-3 | Flows 2.1, 2.2; S3-S6, D1, D2 together |
| NFR-001 AC-1 | Section 9 audit of S1, S2, S3, S4/S6, D1, D2 (+ S5) |
| NFR-001 AC-2 | Section 6 focus rules; keyboard section 7 |
| NFR-001 AC-3 | Common form rules (error summary, `aria-describedby`, `role="alert"`) |
| NFR-002 AC-1, AC-2 | no UI surface (no cloud/email UI) |
| NFR-003 AC-1 | Loading states, no blocking spinners; list stays visible during re-query |
| NFR-004 AC-2 | Form-level and page-level generic error copy; no internals shown |
| NFR-005 AC-1 | Components are plain React + HTML (section 5) |

## 9. Accessibility review (WCAG 2.2 AA; NFR-001)

Design-time review of this specification. These are design checks, not a substitute for the axe, keyboard-only and
screen-reader passes assigned to the build and test nodes (sdd §14). Contrast values were computed by hand from the
hex values in `tokens.json` using the WCAG relative-luminance formula and have **not** been re-checked with a tool;
web-qa must confirm them.

### 9.1 Success criteria checklist

| Criterion | Level | How the design meets it | Status |
|---|---|---|---|
| 1.1.1 Non-text content | A | No images or icon-only controls. Any future icon must be `aria-hidden` next to text | Met |
| 1.3.1 Info and relationships | A | Real labels, fieldset/legend for filter, `<dl>` on S5, `<ul>` for rows, `<nav>` Pagination, headings in order (h1 then h2 in empty states and dialogs), landmarks header/main/search/nav | Met |
| 1.3.2 Meaningful sequence | A | DOM order equals visual order; Cancel precedes Delete in dialogs | Met |
| 1.3.5 Identify input purpose | AA | `autocomplete="username"`, `new-password`, `current-password` | Met |
| 1.4.1 Use of colour | A | Priority is the text "Priority: High"; done is checkbox + "Completed" text; errors have "Error:" prefix + text; selected filter has radio dot; current page has `aria-current` and bold/underline | Met |
| 1.4.3 Contrast (min) | AA | `text.primary` 17.7:1, `secondary` 7.6:1, `link` 6.7:1, `danger` 6.5:1, `success` 7.1:1, white on `action.primary` 6.7:1, on `action.danger` 6.5:1, priority medium 7.1:1; also on tinted surfaces (see tokens `$extensions`). All >= 4.5:1. Disabled text is exempt | Met (verify by tool) |
| 1.4.4 Resize text | AA | rem units; layout reflows at 200% | Met |
| 1.4.10 Reflow | AA | 320 px, no 2-D scroll (section 5) | Met |
| 1.4.11 Non-text contrast | AA | Input/select/checkbox/radio border `gray.500` 4.8:1 on white; focus ring `blue.700` 6.7:1 (>= 5.9 on all surfaces); danger border 6.5:1 | Met (verify by tool) |
| 1.4.12 Text spacing | AA | No fixed heights on text containers | Met |
| 1.4.13 Content on hover/focus | AA | No tooltips or hover content | N/A |
| 2.1.1 Keyboard / 2.1.2 No trap | A | Native controls; native modal dialog traps only while open and Escape exits | Met |
| 2.2.1 Timing adjustable | A | **Not fully met by the spec:** the 8-hour absolute session (REQ-AUTH-002 AC-4, ADR-0008) offers no warning or extension. Designed as specified; see open question Q-UX-1 | Gap raised |
| 2.3.1 Three flashes | A | None | Met |
| 2.4.1 Bypass blocks | A | Skip link | Met |
| 2.4.2 Page titled | A | Per-route `document.title` | Met |
| 2.4.3 Focus order | A | Section 6 | Met |
| 2.4.4 Link purpose | A | Title links name the to-do; Edit/Delete carry visually hidden text "{title}" so each is unique ("Edit {title}", "Delete {title}") | Met |
| 2.4.6 Headings and labels | AA | Descriptive | Met |
| 2.4.7 Focus visible | AA | Every focusable element: `focus.width` 2px ring in `color.focus.ring`, offset `focus.offset` 2px; never `outline: none` without replacement | Met |
| 2.4.11 Focus not obscured (min) | AA | No sticky header/footer is specified; the header is not sticky; ring offset keeps ring outside overflow clipping (no `overflow:hidden` on rows) | Met |
| 2.5.3 Label in name | A | Visible text equals accessible name start ("Delete" button's name "Delete {title}" starts with visible "Delete") | Met |
| 2.5.7 Dragging | AA | No drag interactions | N/A |
| 2.5.8 Target size (min) | AA | 44 px design target, 24 px floor | Met |
| 3.1.1 Language of page | A | `lang="en"` | Met |
| 3.2.1 / 3.2.2 On focus / on input | A | Filter radios change the list on selection: this is a change of content on input, announced by the live summary and expected for a filter group; no navigation or context switch occurs. Search needs Enter/Search | Met (note) |
| 3.2.6 Consistent help | A | No help mechanism | N/A |
| 3.3.1 Error identification | A | Section 4 | Met |
| 3.3.2 Labels or instructions | A | Visible labels, "(required)/(optional)", help text with limits | Met |
| 3.3.3 Error suggestion | AA | Messages say how to fix | Met |
| 3.3.4 Error prevention (legal/financial/data) | AA | Destructive actions (delete, clear completed) need confirmation | Met |
| 3.3.7 Redundant entry | A | Create/edit prefilled; nothing re-asked in a flow | Met |
| 3.3.8 Accessible authentication (min) | AA | No cognitive test; paste and password managers allowed (`autocomplete` set, paste not blocked); Show password | Met |
| 4.1.2 Name, role, value | A | Native elements; checkbox row has accessible name from the title; busy via `aria-disabled` | Met |
| 4.1.3 Status messages | AA | Polite live region for results/toast-like confirmations; `role="alert"` for errors | Met |

### 9.2 Screen-reader text (accessible names)

- Row checkbox: `aria-labelledby` the title element; the state is announced natively as checked / not checked.
- Edit link: "Edit" visible + visually hidden " {title}". Delete button: "Delete" + hidden " {title}".
- Pagination: `<nav aria-label="Pagination">`; links "Page {n}"; current "Page {n}, current page" via
  `aria-current="page"`; Previous / Next have visible text "Previous" / "Next"; ellipses `aria-hidden`.
- Result summary and confirmations: polite live region.
- Dialogs: labelled by heading; description via `aria-describedby` body text.
- Error summary: `role="alert"`; heading level 2; links with the message text.

### 9.3 Findings

| # | Criterion | Element | Issue | Severity | Fix / disposition |
|---|---|---|---|---|---|
| F1 | 2.2.1 Timing Adjustable (A) | 8-hour absolute session | No warning or extension before the session ends; an unsaved edit form is lost on expiry (data entered on S4/S6 is not preserved across the login redirect) | Major if the "essential security" exception is not accepted | Not changed (spec behaviour). Raised as Q-UX-1; options: accept the exception in writing, or add a pre-expiry warning (uses `Session.expiresAt`) |
| F2 | 3.2.2 / usability | Filter radios apply on change | Acceptable, but if testers judge it a context change, add an "Apply" button | Minor | Keep; recorded |
| F3 | 1.4.3 | Contrast numbers hand-computed | Unverified by tool | Minor | web-qa to confirm with axe/contrast tool |

## 10. Figma

Not created: no Figma tools were available to this run and no target Figma file is configured. A frame plan for when
one exists: one frame per screen and state in sections 3-4 at widths 360 (mobile) and 1440 (desktop), bound to the
variables in `tokens.json`. No node ids, links or read-backs exist and none are claimed.

## 11. Open questions and gaps (for the orchestrator to escalate)

Nothing below is decided silently; **[proposed]** items in the design are the least-behaviour choices.

| ID | Gap | Requirement IDs | Proposed in this design |
|---|---|---|---|
| Q-UX-1 | WCAG 2.2.1 Timing Adjustable vs. a fixed 8-hour session with no warning/extension; plus loss of unsaved form input at expiry (the spec returns the user to the route, not the typed data) | REQ-AUTH-002 AC-4, REQ-AUTH-004 AC-4, NFR-001 AC-1 | None: designed as specified; human to accept the essential-timeout exception or request a warning |
| Q-UX-2 | Exact message strings are not in the contract (only `INVALID_CREDENTIALS` detail "Invalid identifier or password." and the sdd §8 clear-completed sentence and sdd §4.1 messages are fixed). Client mirror copy must match server copy | REQ-AUTH-001 AC-2..AC-11, REQ-TODO-002 AC-10, NFR-001 AC-3 | Catalogue in section 4; api and web engineers should share wording |
| Q-UX-3 | Where Delete and Edit live: spec says "choose Delete" but not which screen. Designed on every row and on the detail page | REQ-TODO-006 AC-1, REQ-TODO-004 | Row + detail; after delete on detail go to `/todos` |
| Q-UX-4 | Destination after a successful edit is unspecified | REQ-TODO-004 AC-1 | `/todos/:id` (detail) |
| Q-UX-5 | UI for generic failures (non-401/404 errors, network errors, 403 header, load failures) is not specified beyond "never show internals" | NFR-004 AC-2, REQ-TODO-003 | Generic alerts with Retry, copy in section 4 |
| Q-UX-6 | Clear-completed sentence has no singular/plural form for N = 1; designed as written ("Delete all 1 completed to-dos?") | REQ-TODO-007 AC-1 | Spec sentence used verbatim; approver may allow singular |
| Q-UX-7 | Behaviour for unknown SPA routes and `/` is unspecified (the spec lists only six routes) | sdd §8, REQ-AUTH-004 AC-2 | Not designed; needs a decision (likely `/` to `/todos`) |
| Q-UX-8 | NFR-001 says WCAG 2.1 AA; this design meets 2.2 AA (requested by the task). Confirm 2.2 is acceptable as the audit target | NFR-001 AC-1 | Design satisfies both |
| Q-UX-9 | Race: "Clear all completed" re-fetch returns 0 completed (cleared elsewhere); no AC | REQ-TODO-007 AC-4 | Dialog not opened, control disabled, status message |
| Q-UX-10 | Row counts per filter (`totals.active/completed`) are available but the spec does not ask to display them; not shown | REQ-LIST-001 | Not shown (no added feature) |
| Q-UX-11 | Tokens file is placed under `docs/specs/CHG-0001/design/ux/` not `design/tokens.json` because of this node's output restriction | n/a | Move/generate at build time |
