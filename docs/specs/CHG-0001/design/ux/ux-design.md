---
stage: design-ux
changeId: CHG-0001
node: N06-design-ux
agent: ux-designer
upstream:
  - docs/specs/CHG-0001/requirements.md
  - docs/specs/CHG-0001/sdd.md
  - docs/specs/CHG-0001/contracts/common-problem-v1.yaml
  - docs/specs/CHG-0001/contracts/identity-api-v1.yaml
  - docs/specs/CHG-0001/contracts/todo-api-v1.yaml
  - docs/specs/CHG-0001/adr/0001-react-typescript-spa.md
  - docs/specs/CHG-0001/adr/0008-absolute-session-lifetime.md
  - docs/specs/CHG-0001/adr/0011-problem-details-error-model.md
  - .harness/changes/CHG-0001/clarifications.md
companion: tokens.json (same directory)
---

# UX design — CHG-0001 To-do list

This document designs the screens, states, components, copy and accessibility behaviour of the React SPA in the frozen
spec. It adds no behaviour: each screen, state and component cites the requirement and acceptance criterion (AC-n) it
serves. An unavoidable design choice is marked **[proposed]**. Section 11 records the design decisions and lists every [proposed] item.

Conventions: "AC-n" always refers to the requirement named in the same row or heading. SDD references are `sdd §n`.
Copy in quotation marks is the exact text to render unless marked as a template (`{n}`, `{title}`).

## 0. Provenance and tooling

- **Figma:** no Figma tools or design-system file were available, and the repository has no design system or UI
  catalogue (checked 2026-11-07); see section 10. ADR-0001 forbids a component library, so section 5 lists new components.
- **Tokens location (decision DD-1, section 11):** the tokens live in `tokens.json` beside this file.
- **Accessibility level:** WCAG 2.1 Level AA, as NFR-001 AC-1 states and Q44 confirms. Section 9 audits against it.

## 1. Users, tasks, scope

One user type (REQ-AUTH-005 AC-2; no admin). Tasks: sign up, log in, log out, add / see / search / filter / page /
edit / complete / delete a to-do, clear completed (REQ-ACC-001 AC-1). Single light theme, desktop and mobile web,
viewport 320 CSS px and wider (NFR-001; WCAG 1.4.10). No dark theme, no sorting control, no page-size control, no
password reset, no email verification, no admin or sharing UI (requirements "Out of scope"). Every failure shown
gives a plain-language message with a next step and no technical detail (NFR-006; section 7.3).

## 2. Routes and user flows

| Route | Screen | Guard | Spec |
|---|---|---|---|
| `/` | none: replaced in history by `/todos`, query dropped | n/a | REQ-NAV-001 AC-1, AC-2; sdd §8 |
| `/signup` | S1 Sign up | public; signed-in visitor redirected to `/todos` | REQ-AUTH-001; sdd §4.1 |
| `/login` | S2 Log in | public; signed-in visitor redirected to `/todos` | REQ-AUTH-002; sdd §4.1 |
| `/todos?status=&q=&page=` | S3 My to-dos (list) | `RequireAuth` | REQ-TODO-003, REQ-LIST-001..003 |
| `/todos/new` | S4 Add a to-do | `RequireAuth` | REQ-TODO-001, REQ-TODO-002 |
| `/todos/:id` | S5 To-do detail | `RequireAuth` | REQ-TODO-003 AC-3 |
| `/todos/:id/edit` | S6 Edit a to-do | `RequireAuth` | REQ-TODO-004 |
| any other path (`*`) | S7 Not found (signed-in only) | `RequireAuth`; signed-out goes to login | REQ-NAV-001 AC-3, AC-8; sdd §4.1, §8 |
| (overlay on S3, S5) | D1 Delete confirmation | n/a | REQ-TODO-006 |
| (overlay on S3) | D2 Clear-completed confirmation | n/a | REQ-TODO-007 |
| (any route, before probe resolves) | G1 App shell and session probe states | n/a | REQ-AUTH-002 AC-4; sdd §4.1 |
| (any route, unexpected error) | E1 Application error page | n/a | NFR-006 AC-3; sdd §6.1 |

The six screens of NFR-001 AC-1 are S1, S2, S3, S4/S6 (one screen, two modes), D1, D2; S5 and S7 are the seventh and
eighth audited screens (sdd §9, §13, §14).

### 2.1 Flow: first visit, sign-up, login (REQ-AUTH-001, REQ-AUTH-002, REQ-AUTH-004, REQ-NAV-001)

```
Visitor opens /todos (signed out) --RequireAuth--> /login?returnTo=/todos   (no message)   [AUTH-004 AC-2]
Visitor opens / (signed out) --> /todos --> /login?returnTo=/todos, then /todos after login  [NAV-001 AC-2]
Visitor opens /nope (signed out) --> /login?returnTo=/nope, no message; after login /todos  [NAV-001 AC-8; sdd §4.2]
Visitor opens /signup directly ------------------> S1 (no message, no redirect)            [AUTH-001 AC-1; sdd §14]
S1 submit valid --201--> /login with "Account created. Log in." (router state, not URL)    [AUTH-001 AC-1, AC-4]
S2 submit valid --200--> safeReturnTo(returnTo) else /todos                                 [AUTH-002 AC-1; sdd §4.2]
S2 submit wrong/unknown --401--> same page, generic alert, password cleared                 [AUTH-002 AC-2, AC-3]
Signed-in reload on /todos ---> G1 blank-until-probe, then S3 (no redirect)                 [AUTH-002 AC-4]
Log out (header) --> /login, no message                                                     [AUTH-004 AC-3]
Any call returns 401 while signed in --> /login?returnTo=... with "Your session has ended. Please log in again."  [AUTH-004 AC-4]
Reload after expiry (cookie gone) on a to-do route --> /login, no message                   [sdd §4.1; Q35]
Signed in, opens /nope --> S7; "Go to home" --> / --> /todos                                [NAV-001 AC-3, AC-5]
Signed in, opens /todos/not-a-uuid or /todos/not-a-uuid/edit --> S5/S6 to-do not-found state, no API call, not S7  [NAV-001 AC-4; sdd §8]
```

### 2.2 Flow: managing to-dos (REQ-TODO-001..007, REQ-LIST-001..003)

```
S3 -> "Add a to-do" -> S4 -> submit valid -> /todos (All, no search, page 1; new item first)    [TODO-001 AC-1; sdd §8]
S3 -> title link -> S5 -> "Edit" -> S6 -> save valid -> S5 (values and Updated time shown)      [TODO-004 AC-1; Q41]
S3 -> Edit link  -> S6
S3 checkbox toggle -> PUT done -> re-query current URL -> row leaves Active view if now done    [TODO-005 AC-3]
S3 / S5 "Delete" -> D1 -> Confirm -> deleted, list re-queried (last valid page if page empties) [TODO-006 AC-1..AC-5; Q40]
S3 "Clear all completed" -> re-fetch -> D2 with count -> Confirm -> all completed deleted       [TODO-007 AC-1..AC-3]
S3 filter/search change -> URL page removed -> page 1                                           [LIST-003 AC-5]
```

## 3. Layout shell (G1) — all authenticated and public screens

Serves: REQ-AUTH-004 AC-3 (Log out in header), NFR-001 AC-2 (landmarks, skip link), REQ-AUTH-002 AC-4 (probe).

- **Skip link** "Skip to main content" is the first focusable element; visible on focus; targets `<main id="main">`.
- **Header** (`<header>`): app name "To-do list" (plain text, not a heading). When `signed-in`: a **Log out** button
  (secondary style); no other text (no identifier shown; the spec asks for none). Public routes: header shows the
  app name only. Log out is a `<button>`, 44 px minimum target.
- **Main** (`<main>`): content column `size.content.maxWidth` (720) centred; auth screens `size.content.authMaxWidth`
  (420). Page padding `space.inset.md` (16) at 320 px.
- `<html lang="en">`. Each route sets `document.title` to "{Screen name} - To-do list" and moves focus to the page
  `<h1>` (tabindex -1) on a pathname change, not on a query-string-only change (sdd §9). Titles: "Sign up", "Log in", "My to-dos", "Add a to-do",
  "{title}" (detail), "Edit to-do", "Page not found", "Something went wrong".

States of the session probe (sdd §4.1):

| State | Rendered | Spec |
|---|---|---|
| `unknown`, in flight, to-do route or `*` | nothing in `<main>` (header only); no redirect; never S7 | REQ-AUTH-002 AC-4; REQ-NAV-001 AC-8 |
| `unknown`, in flight, `/login` or `/signup` | nothing in `<main>` until resolved | sdd §4.1 |
| probe failed (network error / 500) | in `<main>`: alert (role="alert") "The app could not reach the server." + button **Retry**. Focus moves to the alert. Retry re-runs the probe; state stays `unknown`; never redirects or clears a user; never S7 | sdd §4.1; NFR-006 AC-1 |
| `signed-out` on a to-do route or `*` | immediate replace to `/login?returnTo=...`, no message | REQ-AUTH-004 AC-2; REQ-NAV-001 AC-8 |

## 4. Screens

Common form rules (S1, S2, S4, S6) — NFR-001 AC-3, REQ-TODO-002 AC-10, REQ-AUTH-001 AC-3, NFR-006:

1. Every field has a visible `<label for>`. Required fields say "(required)" in the label text; optional says
   "(optional)". No placeholder carries essential information.
2. Validation runs on submit (SPA mirror of the server rules; server remains authoritative). Fields are not
   validated on blur, to avoid announcing errors while the user is still typing.
3. On a refused submit: an **error summary** appears at the top of the form: `role="alert"`, heading "There is a
   problem", a list of links, one per error, each link moving focus to its field. Focus moves to the summary. Each
   invalid field gets `aria-invalid="true"`, `aria-describedby` = its message id, a red border (`color.border.danger`)
   and the message text directly under the label/field, prefixed visibly with "Error:" so colour is never the only cue
   (WCAG 1.4.1, 3.3.1). Message text colour `color.text.danger`. From a 400 response the SPA shows only
   `errors[].message`, only beside a field that form shows (S1, S2: identifier, password; S4, S6: title, description,
   dueDate, priority); any other entry is not displayed (sdd §6.1).
4. Fixing a field and re-submitting rebuilds the summary; successful submit removes it.
5. Submit button shows a busy state while the request is in flight: label unchanged, `aria-disabled="true"` (not
   native disabled, to keep focus), repeated activation ignored; text "Saving..." / "Creating account..." /
   "Logging in..." replaces the label. No spinner animation is required.
6. Any other failure (network error, 5xx, 403, 405, 415, `MALFORMED_REQUEST`, `REQUEST_REJECTED`, a body that is not
   a Problem, or a 400 whose `errors[]` names no field the form shows) shows a form-level alert above the form with
   the action's message from section 7.3 (for example "Your changes could not be saved. Please try again."). The
   user's input is kept. Nothing from the response (`title`, `detail`, `code`, request id, raw body) or from the
   browser's error text is ever shown (NFR-006 AC-2; sdd §6.1; ADR-0011).
7. A 401 on any request while `signed-in` is not a form error: the session-ended flow of section 2.1 takes over.
   (Exception: S2's own 401 is invalid credentials.)

### Validation message catalogue

Wording approved in Q39(a). The server's `errors[].message` is displayed as sent and follows the same wording, so the
user sees identical text from either source (sdd §6.1: plain language, says how to fix the field, no internals).

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

### S1 Sign up — `/signup`

Serves: REQ-AUTH-001 AC-1..AC-11; NFR-001 AC-1..AC-3; NFR-006 AC-1.

Layout (single column, 420 max): `<h1>` "Sign up"; form; below the form a text link "Already have an account? Log in"
to `/login`.

Fields:

| Order | Label | Control | Notes (AC) |
|---|---|---|---|
| 1 | "Email address or username (required)" | `<input type="text" autocomplete="username" autocapitalize="none" spellcheck="false">` | One field accepts either (AUTH-001 AC-7). Help text under label: "Use an email address, or a username of letters, digits, . _ -. 3 to 254 characters." (id for `aria-describedby`, combined with error id when invalid) |
| 2 | "Password (required)" | `<input type="password" autocomplete="new-password">` | Help text: "8 to 128 characters." (AC-5, AC-11). No composition rules beyond length |

Submit button: "Sign up" (primary). No email-verification text, no confirm-password field (not in spec).

| State | Behaviour and copy | Spec |
|---|---|---|
| Default | Empty form, focus on `<h1>` on arrival | AUTH-001 AC-1 |
| Visitor opened directly while signed out / signed in | Signed out: form shown, no message, no redirect. Signed in: replaced by redirect to `/todos` | sdd §4.1, §14 |
| Validation errors (client or 400) | Error summary + inline messages from the catalogue; no account created | AC-3, AC-5, AC-8..AC-11; NFR-001 AC-3 |
| Identifier taken (409), incl. case variants | Summary "There is a problem" with link "This email address or username is already taken..." to identifier; the entered password is kept. The message names the next step: choose another identifier, or log in | AC-2, AC-6; NFR-006 AC-1 |
| Submitting | Button busy state | |
| Other failure | Form-level alert "Your account could not be created. Please try again.", input kept | NFR-006 AC-1 |
| Success (201) | Navigate to `/login`, showing "Account created. Log in." (success message on S2); no automatic login, no verification step | AC-1, AC-4; sdd §4 |

### S2 Log in — `/login`

Serves: REQ-AUTH-002 AC-1..AC-5; REQ-AUTH-004 AC-2, AC-4; NFR-001; NFR-006 AC-4.

Layout: `<h1>` "Log in"; optional message region directly under the `<h1>` (before the form); form; link "No account
yet? Sign up".

Fields: "Email address or username (required)" (`autocomplete="username"`), "Password (required)"
(`autocomplete="current-password"`). No format hints and no format validation beyond
non-empty (sdd §4). Submit: "Log in".

| State | Behaviour and copy | Spec |
|---|---|---|
| Default (typed/bookmarked `/login`, redirect from RequireAuth, after logout, after reload-post-expiry) | No message | AUTH-004 AC-2; sdd §4.1 |
| After sign-up | Message (info surface, `role="status"`): "Account created. Log in." Delivered by router state, never URL | AUTH-001 AC-1; sdd §4.1 |
| Session ended while signed in | Message (info surface, `role="status"`): "Your session has ended. Please log in again." Form below; `returnTo` preserved in URL. Anything typed but not saved when the session ended is lost (accepted, Q36) | AUTH-004 AC-4; sdd §4.1 |
| Empty field(s) | Error summary + inline "Enter your email address or username." / "Enter your password." | AUTH-002 sdd §4 (400) |
| Invalid credentials (401, wrong password, unknown identifier, password > 128) | One alert above the form, identical for all three: "Invalid identifier or password. Check them and try again." (the SPA's own text, never the response `detail`). Not attached to a single field, so it does not reveal which was wrong. Password field cleared and focus on the identifier field; the alert is announced (role="alert") | AUTH-002 AC-2, AC-3; NFR-006 AC-4; sdd §6.1 |
| Submitting | Button busy state "Logging in..." | |
| Other failure | Form-level alert "You could not be logged in. Please try again." (section 4 rule 6) | NFR-006 AC-1 |
| Success (also a case-variant or padded identifier: no UI difference) | Navigate with the router to `safeReturnTo(returnTo)` or `/todos`; no flash of the form | AUTH-002 AC-1, AC-5; sdd §4.2 |

### S3 My to-dos (list) — `/todos`

Serves: REQ-TODO-003, REQ-LIST-001..003, REQ-TODO-005, REQ-TODO-006, REQ-TODO-007, REQ-ISO-001 AC-1/AC-4,
REQ-ACC-001, NFR-001, NFR-003, NFR-006.

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
     checked radio dot (not colour alone). No per-filter counts are shown (decision DD-2).
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
| Loading (first load or any re-query) | First load: text "Loading your to-dos..." (`role="status"`) in place of the list. Re-query (filter, search or page change): the existing rows stay until the response arrives (no flicker, no remount), the list region has `aria-busy="true"` and no replacement text is shown | NFR-003 AC-1; sdd §6.1 |
| Populated | Rows, summary, pagination as above | TODO-003 AC-1 |
| First-use empty (`totals.all = 0`, filter All, no search) | Empty-state panel replaces the list (no table/list, no filter-no-match text): `<h2>` "You have no to-dos yet" + text "Add your first to-do to start keeping track of your tasks." + link-button **Add a to-do** (to `/todos/new`). Filter and search controls still shown; **Clear all completed** disabled | TODO-003 AC-2; sdd §8 |
| No matching (`totalItems = 0` with filter other than All, or a search applied; including a user with no to-dos who picks Completed or searches) | `<h2>` "No matching to-dos" + text "Try a different search or filter."; no link or other control is added (the filter and search controls stay on screen). A search matching only another user's data looks identical and reveals nothing | LIST-001 AC-4, LIST-002 AC-4, REQ-ISO-001 AC-4 |
| Page past last / bad page / bad status in URL | URL is replaced (history replace) with the effective page; the user sees the last valid page, no error. Bad `page` (0, negative, non-numeric) becomes page 1. A `status` other than all, active, completed is removed from the URL (filter All) before any request | LIST-003 AC-6, AC-7; LIST-001 AC-5; sdd §8, §12.1 item 8; TODO-006 AC-5 |
| Single page (`totalPages <= 1`) | No pagination | LIST-003 AC-4 |
| Clear all completed disabled | `totals.completed = 0`: button natively `disabled`, muted style (`color.surface.muted`, `color.text.disabled`) | TODO-007 AC-4 |
| Row toggle in flight | The checkbox is `aria-disabled` until the response; no double-submit | TODO-005 |
| Toggle done under Active filter | After the 200, the list is re-queried; the row is gone. Live region says: "Marked done: {title}." Focus moves to the `<h1>`, see section 6 | TODO-005 AC-3 |
| Toggle failure (non-401, non-404) | Page-level alert above the list: "That to-do could not be updated. Please try again." **[proposed]** (sec. 11); checkbox reverts to server state; focus stays on the checkbox | NFR-006 AC-1; sdd §6.1 |
| 404 on toggle (to-do deleted elsewhere) | The current URL's list is re-queried. Once that succeeds, page-level alert "That to-do no longer exists, and the list has been refreshed." and no **Back to my to-dos** link. If the re-query fails, the "Your to-dos could not be loaded. Please try again." state with **Retry** is shown instead, never the "refreshed" sentence; after a successful retry the list shows without it | TODO-005; REQ-ISO-001 AC-2; sdd §6.1, §8; Q48, Q54 |
| List fetch failure (non-401) | Alert in place of list: "Your to-dos could not be loaded. Please try again." **[proposed]** (sec. 11) + button **Retry** | NFR-006 AC-1; sdd §6.1; Q42 |
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
| Success (204) | Dialog closes; list re-queried (S3) or navigate to `/todos` (S5; Q40); live region "Deleted: {title}." If the last item of the last page was deleted the last valid page is shown. Focus goes to the `<h1>` because the invoking row no longer exists | TODO-006 AC-3, AC-5 |
| 404, opened from S3 | Dialog closes; same as "404 on toggle" in S3: list re-queried, then "That to-do no longer exists, and the list has been refreshed." (no Back link); a failed re-query shows the load-failure state instead | TODO-006 AC-4; sdd §6.1; Q54 |
| 404, opened from S5 | Dialog closes; S5 page-level alert "That to-do no longer exists." with link **Back to my to-dos** to `/todos`; no list re-query | TODO-006 AC-4; sdd §6.1; Q48 |
| Other error | Dialog stays open; error text inside dialog above buttons, `role="alert"`: "The to-do could not be deleted. Please try again." **[proposed]** (sec. 11) | NFR-006 AC-1 |
| 401 | Session-ended flow | AUTH-004 AC-4 |

### D2 Clear-completed confirmation (dialog)

Serves: REQ-TODO-007 AC-1..AC-4.

Opening sequence (sdd §8): the click first re-fetches the list; the dialog opens with `totals.completed` (N) from that
response. While the re-fetch runs, the button shows busy state. If N = 0 (for example cleared in another tab) no
dialog opens and no `clearCompletedTodos` call is made; the control is disabled (TODO-007 AC-4) and the polite live
region announces "There are no completed to-dos to clear." (Q43; sdd §8).

- Heading: "Clear completed to-dos?" **[proposed]** (same for every N; avoids "all" when N = 1; sdd §8: N changes only the sentence below).
- Body, N of 2 or more: "Delete all {N} completed to-dos? This deletes all your completed to-dos, including any not
  shown because of the current page, filter or search. This cannot be undone." N = 1 (Q38; sdd §8, §12.1 item 5):
  "Delete 1 completed to-do? This deletes your only completed to-do, even if it is not shown because of the current
  page, filter or search. This cannot be undone."
- Buttons: **Cancel** (initial focus), **Delete {N} completed** (danger).

| State | Behaviour | Spec |
|---|---|---|
| Open | As D1 | TODO-007 AC-1 |
| Opening re-fetch fails (network, 5xx, other non-401, non-404) | No dialog opens; the button leaves its busy state; page-level alert above the list (S3 surface): "Your to-dos could not be loaded. Please try again." (the `load-list` message, sdd §6.1; next step: activate **Clear all completed** again); nothing is deleted; focus stays on **Clear all completed**. A 401 is the session-ended flow | NFR-006 AC-1; sdd §6.1, §9 |
| Cancel / Escape | Closes, nothing deleted, focus back to **Clear all completed** | TODO-007 AC-3 |
| Confirm in flight | Busy "Deleting..." | |
| Success (200 `deletedCount`) | Dialog closes; list re-queried (active to-dos untouched); live region "Deleted {deletedCount} completed to-dos." ("Deleted 1 completed to-do." for 1, **[proposed]**); if now `totals.completed = 0` the trigger is disabled, so focus goes to the `<h1>` | TODO-007 AC-2, AC-4 |
| Error | Inline `role="alert"` in dialog: "Completed to-dos could not be deleted. Please try again." | NFR-006 AC-1 |
| 401 | Session-ended flow | AUTH-004 AC-4 |

### S4 Add a to-do — `/todos/new` and S6 Edit a to-do — `/todos/:id/edit`

One form component (`TodoForm`), two modes. Serves: REQ-TODO-001 AC-1..AC-4, REQ-TODO-002 AC-1..AC-10, REQ-TODO-004 AC-1..AC-4, REQ-DATA-002 (no conflict UI), NFR-001.

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
| S6 not found (`getTodo` 404: deleted, another user's id, or an id that is not a to-do identifier, which makes no API call) | In place of the form: `<h1>` "To-do not found" and text "This to-do does not exist or has been deleted." + link **Back to my to-dos**. Identical for every cause; the link stays on the edit page (Q53) | REQ-ISO-001 AC-2; TODO-004 AC-3; NAV-001 AC-4; sdd §8 |
| Validation errors | Section 4 rules; nothing saved; stored to-do unchanged | TODO-002 AC-2..AC-6, AC-10; TODO-004 AC-2 |
| Save in flight | Busy state ("Saving...") | |
| Save refused 404 (to-do deleted meanwhile) | Form-level alert: "This to-do no longer exists, so your changes could not be saved." with link **Back to my to-dos** (kept on the edit page, Q53); nothing created | TODO-004 AC-3 |
| Save error (other) | Form-level alert "Your changes could not be saved. Please try again." (S4: "Your to-do could not be added. Please try again."); input kept | NFR-006 AC-1 |
| S4 success (201) | Navigate to `/todos` (All, no search, page 1); the new to-do is first; live region on arrival not needed (focus goes to `<h1>`) | TODO-001 AC-1; sdd §8 |
| S6 success (200) | Navigate to `/todos/:id` showing new values and the advanced Updated time | TODO-004 AC-1; Q41 |
| Concurrent edit elsewhere | No conflict message ever; the last save wins silently | REQ-DATA-002 AC-1 |
| S6 load error (`getTodo` non-401, non-404) | In place of the form: alert "This to-do could not be loaded. Please try again." + **Retry** (as S5) | NFR-006 AC-1; sdd §6.1 |

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

Actions: **Edit** (primary link-button to `/todos/:id/edit`), **Delete** (danger, opens D1), link **Back to my to-dos** (to `/todos`).
(A Mark done control is not placed here: the spec requires it on the list only through the row checkbox, and adding it would add behaviour.)

| State | Behaviour and copy | Spec |
|---|---|---|
| Loading | `<h1>` "To-do" and text "Loading..." (`role="status"`) | NFR-003 |
| Loaded | As above | TODO-003 AC-3 |
| Not found (404 any cause, or an id that is not a to-do identifier, which makes no API call) | `<h1>` "To-do not found"; text "This to-do does not exist or has been deleted."; link **Back to my to-dos** | REQ-ISO-001 AC-2; NAV-001 AC-4 |
| Load error (non-401, non-404) | Alert "This to-do could not be loaded. Please try again." + **Retry** | NFR-006 AC-1 |
| Delete from here | D1; on success navigate to `/todos` (Q40); a 404 shows "That to-do no longer exists." with **Back to my to-dos** | TODO-006; sdd §6.1 |
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
| `LinkButton` (anchor styled as button) | "Add a to-do", "Edit", "Cancel", "Back", "Go to home" | default, hover, focus-visible | NFR-001 AC-2 |
| `Message` (info, success, danger variants; `role="status"` or `role="alert"`; optional action: Retry button or link) | S2, S3, S5, forms, dialogs | text + variant surface; always a text prefix word where severity matters ("Error:") | AUTH-001 AC-1; AUTH-004 AC-4; NFR-006 |
| `SearchForm` | S3 | `role="search"`, labelled input, Search button | LIST-002 |
| `FilterGroup` (fieldset + 3 radios) | S3 | selected / unselected, focus | LIST-001 |
| `ResultSummary` (polite live region) | S3 | "Showing a-b of n" | NFR-001 AC-2; sdd §9 |
| `TodoList` / `TodoRow` | S3 | active, completed, toggling (`aria-disabled`), focus; stacked layout < 640, inline >= 640 | TODO-003 AC-1; TODO-005 |
| `PriorityText`, `StatusBadge` | S3, S5 | text "Priority: High" etc. in `color.text.priority.*`; "Completed" text for done rows; the word carries the meaning | NFR-001 (no colour-only) |
| `EmptyState` (first-use, no-match variants) | S3 | heading, text, action | TODO-003 AC-2; LIST-001 AC-4 |
| `Pagination` | S3 | Previous, numbered links (first, last, current +-2, ellipsis "..." as non-interactive text), Next; current page `aria-current="page"` and visually distinct (bold, underlined, filled); disabled Previous/Next not focusable | LIST-003 AC-1..AC-4 |
| `ConfirmDialog` (native `<dialog>`) | D1, D2 | open, busy, error; initial focus Cancel; Escape cancels; focus return | TODO-006; TODO-007; NFR-001 AC-2 |
| `TodoForm` | S4, S6 | create / edit modes | TODO-001, 002, 004 |
| `TodoDetail` (`<dl>`) | S5 | loaded / not found / error | TODO-003 AC-3 |
| `LoadingText` | S3, S5, S6 | `role="status"` text "Loading..." | NFR-003 |
| `NotFoundPage` (heading, sentence, home link, decorative animated illustration) | S7 | signed-in only; animation plays once, `aria-hidden` | NAV-001 AC-3..AC-7 |
| `AppErrorBoundary` page | E1 | heading, sentence, Reload control, home link | NFR-006 AC-3 |

Why new: no component library is permitted (ADR-0001) and the repo has none.
Not designed (no requirement asks for them): a Show password control, an identifier display in the header.

### Component sizing and tokens

- Buttons, links-as-buttons, inputs, selects, checkboxes (hit area), radios (hit area), pagination links: minimum
  `size.target.min` (44 x 44 CSS px) including the checkbox/radio label hit area; a design target above any WCAG 2.1 AA need.
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
| Pathname change (incl. S7, E1); not a query-string-only change | page `<h1>` | (title via `document.title`) |
| List re-query after filter / search change | stays on the control the user used (no remount) | "Showing a-b of n", or the empty-state heading text |
| Pagination link activated | `<h1>` "My to-dos" (a focus move, not a remount) | "Showing a-b of n" |
| Toggle done/not done, row remains | stays on the checkbox | "Marked done: {title}." / "Marked not done: {title}." |
| Toggle done, row leaves the Active view | `<h1>` "My to-dos" | "Marked done: {title}." |
| D1 opens / D2 opens | Cancel | (dialog heading read by AT) |
| D1/D2 cancelled | invoking control | none |
| D1 confirmed on S3 | `<h1>` (row is gone) | "Deleted: {title}." |
| D2 confirmed | `<h1>` | "Deleted {n} completed to-dos." (1: "Deleted 1 completed to-do.") |
| D2 not opened (re-fetch found none) | `<h1>`, because the trigger became disabled **[proposed]** | "There are no completed to-dos to clear." |
| Refused submit | Error summary | summary read (`role="alert"`) |
| S2 invalid credentials | identifier field | alert text |
| Probe error | the alert | alert text |
| D2 opening re-fetch fails | stays on **Clear all completed** | alert text (page-level alert above the list) |
| S3 404 on toggle, after the refresh | `<h1>` (the row may be gone) | the "no longer exists" alert |
| D1 closes on a 404, opened from S3 | `<h1>` | the "no longer exists" alert |
| D1 closes on a 404, opened from S5 | `<h1>` (the S5 alert follows it) | the S5 alert text |
| Toggle failure | stays on the checkbox | alert text |

One polite live region (`role="status"`) in the app shell carries the toggle/delete/clear messages so they survive
removal of the invoking element. The list "Loading" state does not steal focus. Using `<h1>` as the fall-back focus
target satisfies sdd §9 ("focus returns to the invoking control or, if that row was deleted, to the list heading").

## 7. Interaction details, S7 Not found, E1 Application error, failure messages

### 7.1 Interaction details

- Keyboard: Tab order follows DOM = visual order. Radio group by arrow keys (native). Dialog: Tab cycles inside
  (native modal), Escape cancels. Enter submits forms. No keyboard traps; no custom shortcuts.
- Pointer: hover colours use `color.action.*Hover`; hover alone never conveys state. No drag, no gestures.
- Motion: the only animation is the S7 illustration (section 7.2). Any transition uses `motion.default` and
  `motion.reduced` under `prefers-reduced-motion: reduce`.
- Text input behaviour: the client trims with the sdd §5 whitespace set when validating; the field text itself is not
  rewritten while typing.

### 7.2 S7 Not found (any address the app does not define; signed-in only)

Serves: REQ-NAV-001 AC-3, AC-5, AC-6, AC-7, AC-8; NFR-001. Copy, layout and motion are the UX design's (sdd §8);
all of it is **[proposed]**.

- `<h1>` "Page not found". Sentence: "We could not find that page. Check the address, or go to home." Link
  **Go to home** to `/` (which sends the user on to `/todos`; AC-5, Q46). `document.title` "Page not found - To-do
  list"; focus moves to the `<h1>` on arrival. S7 never redirects and calls no API.
- Illustration (decorative): a drawn to-do card with three lines, built from CSS and the colour tokens, in an element
  with `aria-hidden="true"`, placed above the heading. Heading, sentence and link are visible and operable from the
  first render; the animation never hides or moves them (sdd §8).
- Motion (AC-6): the card fades in and rises 16 px (`motion.notFound.enter.duration`, 600 ms), then its third line is
  struck through left to right (`motion.notFound.strike.duration` 600 ms after `motion.notFound.strike.delay` 600 ms).
  Total 1.2 s, ceiling 5 s (`motion.notFound.maxTotal`), `animation-iteration-count` 1 (`motion.notFound.iterations`),
  opacity and transform only, no flashing (WCAG 2.2.2, 2.3.1). Under `prefers-reduced-motion: reduce` the illustration
  is drawn in its end state with no animation.

| State | Behaviour | Spec |
|---|---|---|
| Signed in, address undefined | S7 as above | NAV-001 AC-3 |
| Probe in flight (`unknown`) | nothing in `<main>`; after a failed probe the G1 alert; never S7 | NAV-001 AC-8; sdd §8 |
| Signed out | replace with `/login?returnTo=<path>`, no message; after login `/todos` | NAV-001 AC-8; sdd §4.2 |
| `/todos/<anything>`, `/todos/<anything>/edit` | stay on S5 / S6 to-do not-found state, not S7 | NAV-001 AC-4 |

### 7.3 E1 Application error page and failure messages (NFR-006)

E1 replaces a screen whose rendering threw (AC-3): `<h1>` "Something went wrong"; sentence "This page could not be shown.
Reload the page, or go to home."; button **Reload the page** (full reload) and link **Go to home** to `/`. Copy
**[proposed]**; no technical detail. The error clears only when the pathname changes, so the home link renders the
normal page without a full load (sdd §6.1). Title "Something went wrong - To-do list"; focus on the `<h1>`.

Every other failure message is chosen by action and status (sdd §6.1). Next steps: try again, log in again, fix the
named field, go back to my to-dos, the refreshed list, or reload / home. Copy is **[proposed]** unless a source is named.

| Action | Message (try again) | Where shown |
|---|---|---|
| session-probe | "The app could not reach the server." + **Retry** (sdd §4.1) | G1 |
| sign-up / log-in | "Your account could not be created. Please try again." / "You could not be logged in. Please try again." | form-level alert |
| log-out | "You could not be logged out. Please try again." | alert at top of `<main>`; the user stays signed in on the same screen; focus moves to the alert |
| load-list / load-todo | "Your to-dos could not be loaded. Please try again." / "This to-do could not be loaded. Please try again." + **Retry** | in place of the content |
| create / save | "Your to-do could not be added. Please try again." / "Your changes could not be saved. Please try again." | form-level alert |
| toggle-done | "That to-do could not be updated. Please try again." | alert above the list |
| delete-from-list / -detail | "The to-do could not be deleted. Please try again." | inside D1 |
| clear-completed | "Completed to-dos could not be deleted. Please try again." | inside D2 |
| clear-completed, opening re-fetch | the `load-list` message "Your to-dos could not be loaded. Please try again." | page-level alert above the list; no dialog opens |

An unexpected error in an event handler or promise callback (sdd §6.1) uses the same sentence with "Reload the page,
or go to home." in place of "Please try again.", shown in the same place with a **Reload the page** button and a
**Go to home** link; the screen is not replaced. Fixed texts elsewhere: "Invalid identifier or password. Check them and
try again." (S2); "Your session has ended. Please log in again." (S2); "That to-do no longer exists, and the list has
been refreshed." (S3, D1); "That to-do no longer exists." with **Back to my to-dos** (S5); "This to-do does not exist
or has been deleted." with **Back to my to-dos** (S5, S6 not found).

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
| REQ-AUTH-002 AC-4 | G1 probe states (blank until resolved, no redirect, error + Retry); note on the 8-hour session (Q36) in S2 and section 9 |
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
| REQ-TODO-006 AC-4 | D1 404 states; S5/S6 not-found |
| REQ-TODO-006 AC-5 | D1 success -> last valid page; S3 "Page past last" |
| REQ-TODO-007 AC-1 | D2 text with count and scope; singular for N = 1 (Q38) |
| REQ-TODO-007 AC-2, AC-3 | D2 confirm / cancel |
| REQ-TODO-007 AC-4 | S3 "Clear all completed disabled"; D2 N = 0 handling (Q43) |
| REQ-LIST-001 AC-1..AC-3, AC-5 | S3 FilterGroup (default All; unknown `status` removed from URL) |
| REQ-LIST-001 AC-4 | S3 "No matching to-dos" (distinct from first-use) |
| REQ-LIST-002 AC-1..AC-3, AC-5 | S3 SearchForm (combined with filter) |
| REQ-LIST-002 AC-4 | S3 "No matching to-dos" |
| REQ-LIST-002 AC-6, AC-7 | S3 search: literal characters, whitespace-only = no search (no UI difference) |
| REQ-LIST-003 AC-1..AC-4 | S3 Pagination component states |
| REQ-LIST-003 AC-5 | S3 filter/search change removes `page` |
| REQ-LIST-003 AC-6, AC-7, AC-8 | S3 "Page past last / bad page"; no page-size control |
| REQ-NAV-001 AC-1, AC-2 | Routes table `/`; flow 2.1 |
| REQ-NAV-001 AC-3, AC-5 | S7 Not found; "Go to home" to `/` |
| REQ-NAV-001 AC-4 | S5 / S6 to-do not-found states for ids that are not the user's to-dos |
| REQ-NAV-001 AC-6, AC-7 | S7 motion (section 7.2); section 9 rows 2.2.2, 2.3.1 |
| REQ-NAV-001 AC-8 | G1 probe states; S7 "Signed out"; flow 2.1 |
| REQ-DATA-001 AC-1..AC-3 | no UI surface |
| REQ-DATA-002 AC-1, AC-2 | S6 "Concurrent edit elsewhere": no conflict UI |
| REQ-ACC-001 AC-1..AC-3 | Flows 2.1, 2.2; S3-S6, D1, D2 together |
| NFR-001 AC-1 | Section 9 audit of S1, S2, S3, S4/S6, D1, D2 (+ S5, S7) |
| NFR-001 AC-2 | Section 6 focus rules; keyboard section 7.1 |
| NFR-001 AC-3 | Common form rules (error summary, `aria-describedby`, `role="alert"`) |
| NFR-002 AC-1, AC-2 | no UI surface (no cloud/email UI) |
| NFR-003 AC-1 | Loading states, no blocking spinners; list stays visible during re-query |
| NFR-004 AC-2 | Section 7.3: no internals shown |
| NFR-005 AC-1 | Components are plain React + HTML (section 5) |
| NFR-006 AC-1..AC-4 | Section 7.3 (messages with next steps, E1, unexpected-error variant); S2 identical login text; section 4 rule 6 |
| NFR-007 AC-1..AC-3, C-2 | no UI surface (API module split) |

## 9. Accessibility review (WCAG 2.1 Level AA; NFR-001)

Design-time review of this specification, against the level NFR-001 AC-1 and Q44 name. These are design checks, not a
substitute for the axe, keyboard-only and screen-reader passes assigned to the build and test nodes (sdd §14). Contrast
values were computed by hand from the hex values in `tokens.json` using the WCAG relative-luminance formula and have
**not** been re-checked with a tool; web-qa must confirm them.

### 9.1 Success criteria checklist

| Criterion | Level | How the design meets it | Status |
|---|---|---|---|
| 1.1.1 Non-text content | A | No icon-only controls. The S7 illustration is decorative and `aria-hidden` | Met |
| 1.3.1 Info and relationships | A | Real labels, fieldset/legend for filter, `<dl>` on S5, `<ul>` for rows, `<nav>` Pagination, headings in order (h1 then h2 in empty states and dialogs), landmarks header/main/search/nav | Met |
| 1.3.2 Meaningful sequence | A | DOM order equals visual order; Cancel precedes Delete in dialogs | Met |
| 1.2.x Time-based media | A/AA | No audio or video | N/A |
| 1.3.3 Sensory characteristics | A | Instructions name controls by text ("Search", "Retry"), never by shape, colour or position | Met |
| 1.3.4 Orientation | AA | No orientation lock | Met |
| 1.3.5 Identify input purpose | AA | `autocomplete="username"`, `new-password`, `current-password` | Met |
| 1.4.1 Use of colour | A | Priority is the text "Priority: High"; done is checkbox + "Completed" text; errors have "Error:" prefix + text; selected filter has radio dot; current page has `aria-current` and bold/underline | Met |
| 1.4.2 Audio control | A | No audio | N/A |
| 1.4.3 Contrast (min) | AA | `text.primary` 17.7:1, `secondary` 7.6:1, `link` 6.7:1, `danger` 6.5:1, `success` 7.1:1, white on `action.primary` 6.7:1, on `action.danger` 6.5:1, priority medium 7.1:1; also on tinted surfaces (see tokens `$extensions`). All >= 4.5:1. Disabled text is exempt | Met (verify by tool) |
| 1.4.4 Resize text | AA | rem units; layout reflows at 200% | Met |
| 1.4.5 Images of text | AA | All text is real text; the S7 illustration carries no text | Met |
| 1.4.10 Reflow | AA | 320 px, no 2-D scroll (section 5) | Met |
| 1.4.11 Non-text contrast | AA | Input/select/checkbox/radio border `gray.500` 4.8:1 on white; focus ring `blue.700` 6.7:1 (>= 5.9 on all surfaces); danger border 6.5:1 | Met (verify by tool) |
| 1.4.12 Text spacing | AA | No fixed heights on text containers | Met |
| 1.4.13 Content on hover/focus | AA | No tooltips or hover content | N/A |
| 2.1.1 Keyboard / 2.1.2 No trap | A | Native controls; native modal dialog traps only while open and Escape exits; S7 home link reachable | Met |
| 2.1.4 Character key shortcuts | A | No single-key shortcuts | N/A |
| 2.2.1 Timing adjustable | A | The 8-hour absolute session (REQ-AUTH-002 AC-4, ADR-0008) gives no warning or extension and unsaved input is lost; recorded as met under the "essential" exception the requester accepted (Q36; NFR-001 note) | Met by exception |
| 2.2.2 Pause, stop, hide | A | The only moving content is the S7 illustration: plays once, ends within 1.2 s (ceiling 5 s), no control needed (REQ-NAV-001 AC-6) | Met |
| 2.3.1 Three flashes | A | S7 animation is one fade and one wipe; no flashing; no other animation | Met |
| 2.4.1 Bypass blocks | A | Skip link | Met |
| 2.4.2 Page titled | A | Per-route `document.title` | Met |
| 2.4.3 Focus order | A | Section 6 | Met |
| 2.4.4 Link purpose | A | Title links name the to-do; Edit/Delete carry visually hidden text "{title}" so each is unique ("Edit {title}", "Delete {title}") | Met |
| 2.4.5 Multiple ways | AA | Applies to a set of web pages; in a task-based app every to-do page is reached by the list links, with search and filter as a second way. Other screens are steps of a process (exempt) | Met (note) |
| 2.4.6 Headings and labels | AA | Descriptive | Met |
| 2.4.7 Focus visible | AA | Every focusable element: `focus.width` 2px ring in `color.focus.ring`, offset `focus.offset` 2px; never `outline: none` without replacement; S7 home link included | Met |
| 2.5.1 Pointer gestures | A | No path-based or multipoint gestures | N/A |
| 2.5.2 Pointer cancellation | A | Native controls act on release | Met |
| 2.5.3 Label in name | A | Visible text equals accessible name start ("Delete" button's name "Delete {title}" starts with visible "Delete") | Met |
| 2.5.4 Motion actuation | A | No motion-triggered function | N/A |
| 3.1.1 Language of page | A | `lang="en"` | Met |
| 3.1.2 Language of parts | AA | All copy is English; user-entered text is shown as typed with no language claim | Met |
| 3.2.1 / 3.2.2 On focus / on input | A | Filter radios change the list on selection: a change of content on input, announced by the live summary and expected for a filter group; no navigation or context switch occurs. Search needs Enter/Search | Met (note) |
| 3.2.3 / 3.2.4 Consistent navigation / identification | AA | One header and one set of component names on every screen | Met |
| 3.3.1 Error identification | A | Section 4 | Met |
| 3.3.2 Labels or instructions | A | Visible labels, "(required)/(optional)", help text with limits | Met |
| 3.3.3 Error suggestion | AA | Messages say how to fix; section 7.3 names a next step | Met |
| 3.3.4 Error prevention (legal/financial/data) | AA | Destructive actions (delete, clear completed) need confirmation | Met |
| 4.1.1 Parsing | A | Valid HTML, unique ids | Met |
| 4.1.2 Name, role, value | A | Native elements; checkbox row has accessible name from the title; busy via `aria-disabled` | Met |
| 4.1.3 Status messages | AA | Polite live region for results and confirmations; `role="alert"` for errors | Met |

### 9.2 Beyond the audited level (WCAG 2.2, informational; not part of NFR-001)

2.4.11 Focus not obscured (min): no sticky header or footer. 2.5.7 Dragging: no drag interactions. 2.5.8 Target size
(min): 44 px design target, 24 px floor (`size.target.wcagMin`). 3.2.6 Consistent help: no help mechanism. 3.3.7
Redundant entry: create and edit are prefilled. 3.3.8 Accessible authentication (min): no cognitive test; paste and
password managers allowed. The design meets these; the audit does not depend on them.

### 9.3 Screen-reader text (accessible names)

- Row checkbox: `aria-labelledby` the title element; the state is announced natively as checked / not checked.
- Edit link: "Edit" visible + visually hidden " {title}". Delete button: "Delete" + hidden " {title}".
- Pagination: `<nav aria-label="Pagination">`; links "Page {n}"; current "Page {n}, current page" via
  `aria-current="page"`; Previous / Next have visible text "Previous" / "Next"; ellipses `aria-hidden`.
- Result summary and confirmations: polite live region.
- Dialogs: labelled by heading; description via `aria-describedby` body text.
- Error summary: `role="alert"`; heading level 2; links with the message text.

### 9.4 Findings

| # | Criterion | Element | Issue | Severity | Fix / disposition |
|---|---|---|---|---|---|
| F1 | 3.2.2 / usability | Filter radios apply on change | Acceptable, but if testers judge it a context change, add an "Apply" button | Minor | Keep; recorded |
| F2 | 1.4.3 | Contrast numbers hand-computed | Unverified by tool | Minor | web-qa to confirm with axe/contrast tool |

## 10. Figma

Not created; no Figma tools were available and no target Figma file is configured. A frame plan for when one
exists: one frame per screen and state in sections 3-7 at widths 360 (mobile) and 1440 (desktop), bound to the
variables in `tokens.json`. No node ids, links or read-backs exist and none are claimed.

## 11. Decisions and open questions

**Decisions recorded**

| ID | Decision | Reason |
|---|---|---|
| DD-1 | The design tokens stay in `docs/specs/CHG-0001/design/ux/tokens.json`; no root `design/tokens.json` is created by this node. The react-engineer generates the CSS custom properties (`--color-text-primary` etc.) in `web/` from it and does not edit values in the generated output. | This node may write only under `docs/specs/CHG-0001/design/ux/`; the frozen spec names no tokens path; one source file beside the design avoids two copies drifting; the generated stylesheet is product code owned by the build node. |
| DD-2 | No per-filter counts (`totals.active`, `totals.completed`) are shown beside the filter radios (REQ-LIST-001). | The spec does not ask for them; showing them adds a feature. `totals.completed` is used only to enable or disable **Clear all completed** and in the D2 text. |

**Removed additions (round 2):** Show password control (S1, S2), "Show all to-dos" link in the no-match state (sdd §12.1
item 8 rejected that next step), "Signed in as {identifier}" in the header. No cited requirement needs any of them.

**Discrepancy for spec-author (do not edit the SDD):** sdd.md L155 and L209 quote the sign-up message as "Account
created — log in."; this design uses the Q39(a)-approved "Account created. Log in." (sdd §6.1 and §8 give exact copy to
the UX design; the SDD quote predates Q39). Align the SDD at the next re-freeze. Testers assert the Q39(a) text.

**Every [proposed] item** (none needs a requester decision unless stated; all are design-owned under sdd §6.1 / §8):

| Item | Reason | Owner |
|---|---|---|
| D2 heading "Clear completed to-dos?" | Same for every N; avoids "all" when N = 1 (sdd §8 varies only the sentence) | design (sdd §8) |
| D2 singular live-region text "Deleted 1 completed to-do." | Singular form for N = 1 (Q38); "Deleted 0 completed to-dos." is accepted if a parallel clear left none | design (sdd §8) |
| Focus to `<h1>` when D2 does not open | The trigger becomes disabled | design (sdd §9) |
| D2 opening re-fetch failure: `load-list` alert above the list, focus stays on the trigger | NFR-006 AC-1; sdd §6.1 (re-query failure uses `load-list`), §9 | design (sdd §6.1) |
| S7 copy, layout and motion (section 7.2) | sdd §8 delegates them | design (sdd §8) |
| E1 copy (section 7.3) | NFR-006 AC-3; sdd §6.1 | design (sdd §6.1) |
| Section 7.3 failure messages | NFR-006 AC-1: name the failed action and a next step | design (sdd §6.1) |
| Reworded from Q39(a)/Q42 text: "That to-do could not be updated. Please try again."; "Your to-dos could not be loaded. Please try again."; "The to-do could not be deleted. Please try again."; "Completed to-dos could not be deleted. Please try again."; generic "Something went wrong. Please try again." | Reworded to name the failed action and a next step (NFR-006 AC-1; sdd §6.1). If the requester prefers the earlier wording, say so before build | design (sdd §6.1) |
| Log-out failure keeps the session and screen; focus to the alert | sdd §4.1 is silent; NFR-006 AC-1 | design |
| S6 load-error state | NFR-006 AC-1; mirrors S5 | design |

**Open questions for the requester:** none. The questions of the first pass are settled by the frozen spec: session
timeout without warning (Q36; NFR-001 note), message wording (Q39(a)), Delete and Edit placement (Q40), destination
after an edit (Q41), failure alerts with Retry (Q42), nothing completed on refresh (Q43; sdd §8), audit level (Q44),
clear-completed singular (Q38; REQ-TODO-007 AC-1), `/` and unknown routes (Q37, Q46, Q47; REQ-NAV-001).
