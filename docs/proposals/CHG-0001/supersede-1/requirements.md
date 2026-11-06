---
stage: requirements
changeId: CHG-0001
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0001
    agent: requirements-analyst
    agentRunId: run-mw4qgud8
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0001
    agent: spec-author
    agentRunId: unknown
upstream:
  - path: docs/specs/CHG-0001/intent.md
    sha256: 8465eeb8e5e930d7bd165b4c152e9be5bd62e33d8350a11291867d0b16792c98
---

# Requirements — CHG-0001

## Context and goal

CHG-0001 is a new to-do list application (Q1: to-do app only). Individual users sign up, log in, and create and manage
their own to-dos in a web UI backed by an HTTP API (Q2, Q5, Q11). To-dos persist across restarts (Q4). The first
release runs locally only (Q10). Source of every requirement is the answered `clarifications.md` (Q1–Q34, and Q36–Q39
and Q44 for the items added or changed by the first supersede); the Q numbers
in brackets trace each item. Q33 records that the requester accepted every item the first draft had marked as
proposed; Q34 settled the three ambiguous criteria. The few points the requester did not decide carry the tag
`(analyst default)` and are listed under "Analyst defaults" for the approver to confirm at freeze.

## Glossary

- **User**: a person with a self-registered account.
- **Identifier**: the single value a user signs up and logs in with; it is either an email address or a username (Q14, Q31).
- **To-do**: one task record owned by exactly one user.
- **Active**: a to-do that is not done. **Completed**: a to-do that is done.
- **Filter**: one of All, Active, Completed (Q9).
- **Page**: a set of up to 20 to-dos (Q21, Q29).

## Constraints (recorded, not designed here)

- C-1: Web UI backed by an HTTP API (Q2); React + TypeScript UI and Java/Spring Boot API (Q3); SQLite embedded file database (Q4, Q13). These are fixed by the requester; the SDD decides everything else.

## Accounts and access

## REQ-AUTH-001 Self sign-up (M)

A new person creates an account with one identifier (an email address or a username) and a password. Traces: Q5, Q14, Q27, Q31, Q32, Q33.

- AC-1: Given I am not signed in and the identifier is not in use, when I submit a valid identifier and a valid password on sign-up, then an account is created and I can log in with them.
- AC-2: Given an account with identifier X exists, when someone signs up with identifier X, then sign-up is refused with a message that the identifier is taken and no second account is created.
- AC-3: Given I submit sign-up with an empty identifier or an empty password, when I submit, then it is refused with a field-level validation message and no account is created.
- AC-4: Given I sign up, when the account is created, then no email verification step is required before I can use the app (Q27).
- AC-5: Given a password of fewer than 8 characters, when I sign up, then it is refused with a validation message (minimum 8 accepted in Q33).
- AC-6: Given an account with identifier `Alice@Example.com` exists, when someone signs up with `alice@example.com` (or any other letter-case variant), then it is treated as a duplicate and refused as in AC-2 (Q32).
- AC-7: Given the sign-up form, when I fill it in, then there is one identifier field that accepts either an email address or a username (Q31).
- AC-8: Given an identifier, when it is saved, then leading and trailing whitespace is removed and the trimmed identifier must be 3 to 254 characters, otherwise sign-up is refused with a validation message (analyst default).
- AC-9: Given a trimmed identifier that contains "@", when I sign up, then it is treated as an email address and must be a valid email address, otherwise sign-up is refused with a validation message (analyst default).
- AC-10: Given a trimmed identifier that does not contain "@", when I sign up, then it is treated as a username and must contain only the characters A–Z, a–z, 0–9, ".", "_" and "-", otherwise sign-up is refused with a validation message (analyst default).
- AC-11: Given a password of more than 128 characters, when I sign up, then it is refused with a validation message; a password of exactly 8 or exactly 128 characters is accepted (maximum 128 is an analyst default; minimum 8 is Q33).

## REQ-AUTH-002 Login (M)

A user logs in with identifier and password and receives an authenticated session. Traces: Q14, Q31, Q32, Q33, Q36.

- AC-1: Given a registered user, when they submit the correct identifier and password, then they are signed in and see their own to-do list.
- AC-2: Given a registered user, when they submit a wrong password, then login is refused with a generic "invalid identifier or password" message and no session is created.
- AC-3: Given an identifier that does not exist, when someone attempts login, then the same generic message as AC-2 is shown; the login response must not reveal whether the identifier exists (Q33). Note: sign-up (REQ-AUTH-001 AC-2) necessarily discloses that an identifier is taken, so non-enumeration applies to login only; this is an accepted trade-off following from the single unique, case-insensitive identifier (Q31, Q32) and the local-only first release (Q10).
- AC-4: Given I am signed in and reload the page, when the app loads, then I remain signed in until I log out or the session expires; the session lifetime is 8 hours (Q33).
- AC-5: Given a registered identifier, when I log in using a different letter case of it (for example `ALICE@example.com` for `alice@example.com`), then the login succeeds with the correct password (Q32).
- Note (Q36): the 8-hour session of AC-4 ends with no warning before expiry and cannot be extended; this is accepted under the "essential" exception of WCAG 2.2.1 Timing Adjustable, as recorded in the note under NFR-001. Source: clarifications Q36.

## REQ-AUTH-003 Password storage (M)

Traces: security-baseline note (top of clarifications), Q14.

- AC-1: Given any account, when the database file is inspected, then no plaintext or reversibly encrypted password is present; only a salted one-way hash from a strong password-hashing algorithm.
- AC-2: Given two accounts with the same password, when their stored hashes are compared, then the hashes differ (unique salt per account).
- AC-3: Given any API response, log line or error message, when inspected, then it never contains a password or password hash.

## REQ-AUTH-004 Unauthenticated access and logout (M)

Traces: Q5, Q14, Q22, Q33.

- AC-1: Given I am not signed in, when I call any to-do API operation (create, list, update, delete, clear completed), then it is refused as unauthenticated and no data is returned or changed.
- AC-2: Given I am not signed in, when I open a to-do page in the UI, then I am sent to the login page.
- AC-3: Given I am signed in, when I choose the Log out control, then my session ends and API calls with that session are refused as unauthenticated (logout control accepted in Q33).
- AC-4: Given my session has expired, when I make a request, then it is refused as unauthenticated and the UI asks me to log in again.

## REQ-AUTH-005 No admin role (M)

Traces: Q15.

- AC-1: Given any user, when they use the app, then there is no operation that lists users or reads or changes another user's to-dos.
- AC-2: Given the sign-up form, when a user registers, then every account has the same single role with the same permissions.

## REQ-ISO-001 Per-user isolation (M)

Traces: Q5, Q15, Q22, Q34.

- AC-1: Given users A and B each have to-dos, when A lists to-dos with any filter, search or page, then only A's to-dos appear and none of B's.
- AC-2: Given a to-do owned by B, when A requests it, updates it, toggles it or deletes it by its identifier, then the response is "not found", the same as for an identifier that does not exist, and B's to-do is unchanged and not accessible to A (Q34).
- AC-3: Given A has completed to-dos and B has completed to-dos, when A runs "clear all completed", then only A's completed to-dos are deleted.
- AC-4: Given A searches for a term that appears only in B's to-dos, when the search runs, then A sees the empty-search-result state and no information about B's data.

## To-dos

## REQ-TODO-001 Create a to-do (M)

Traces: Q6, Q7, Q16, Q23–Q26, Q30.

- AC-1: Given I am signed in, when I submit a valid title, a due date and a priority, then a to-do is created for me, not done, with created and updated timestamps set, and it appears at the top of my list.
- AC-2: Given a create request (from the UI or sent directly to the API) that omits the priority, when it is processed, then the to-do is stored with priority Medium (Q24, Q26).
- AC-3: Given I provide a description, when I create the to-do, then the description is stored and shown; given I omit it, then the to-do has no description.
- AC-4: Given the new to-do, when I reload the app, then it is still present.

## REQ-TODO-002 To-do field rules and validation (M)

Traces: Q6, Q17, Q23, Q24, Q25, Q26, Q30, Q33.

- AC-1: Given a title, when it is saved, then leading and trailing whitespace is removed and the trimmed title must be 1 to 200 characters.
- AC-2: Given an empty title or a title of only whitespace, when I create or update a to-do, then it is refused with a validation message and nothing is saved.
- AC-3: Given a title whose trimmed length is 201 or more characters, when I save, then it is refused with a validation message; a title of exactly 200 characters is accepted.
- AC-4: Given a description of 2001 or more characters, when I save, then it is refused with a validation message; a description of exactly 2000 characters, or none, is accepted.
- AC-5: Given a to-do is saved without a due date, when I save, then it is refused with a validation message (the due date is required, Q30).
- AC-6: Given a due date that is not a valid calendar date (for example 2026-02-30 or free text), when I save, then it is refused with a validation message.
- AC-7: Given a due date, when it is stored and shown, then it is a date only with no time of day and is displayed as the same calendar date regardless of the viewer's time zone (Q25).
- AC-8: Given a priority other than Low, Medium or High, when I save through the API, then it is refused with a validation error; the UI offers only those three values.
- AC-9: Given a due date in the past, when I save, then it is accepted (Q33).
- AC-10: Given any refused save, when the response is shown, then the validation message identifies the offending field.

## REQ-TODO-003 View and order the list (M)

Traces: Q9, Q11, Q22.

- AC-1: Given I have to-dos, when I open my list, then they are ordered newest first by creation time, and each shows title, done status, due date and priority.
- AC-2: Given I have no to-dos at all, when I open my list, then an empty-state message is shown instead of an empty table, inviting me to add a to-do.
- AC-3: Given a to-do, when I view it, then I can see its description, created timestamp and updated timestamp.

## REQ-TODO-004 Edit a to-do (M)

Traces: Q7 (title, description), Q16 and Q24–Q26, Q30 (due date, priority). Editing the due date and priority is an elaboration of "update" in Q7 now that Q16 added those fields.

- AC-1: Given one of my to-dos, when I change its title, description, due date or priority to valid values and save, then the new values are shown and persisted and its updated timestamp advances; the created timestamp is unchanged.
- AC-2: Given I edit with invalid values (see REQ-TODO-002), when I save, then it is refused and the stored to-do is unchanged.
- AC-3: Given a to-do that does not exist (for example already deleted), when I save an edit, then I see a not-found message and nothing is created.
- AC-4: Given a to-do with a description, when I clear the description and save, then the to-do is saved with no description (the description is optional, Q6). A to-do's due date cannot be cleared, because it is required (Q30).

## REQ-TODO-005 Mark done and not done (M)

Traces: Q7.

- AC-1: Given an active to-do of mine, when I mark it done, then it is Completed, persisted, and its updated timestamp advances.
- AC-2: Given a Completed to-do of mine, when I mark it not done, then it is Active again, persisted, and its updated timestamp advances.
- AC-3: Given I am viewing the Active filter, when I mark a to-do done, then the list is re-queried immediately after the change and the to-do is no longer shown in the Active view, without a manual page reload.

## REQ-TODO-006 Delete a to-do (M)

Traces: Q8, Q33.

- AC-1: Given one of my to-dos, when I choose Delete, then the UI asks for confirmation before anything is deleted.
- AC-2: Given the confirmation prompt, when I cancel, then the to-do remains unchanged.
- AC-3: Given the confirmation prompt, when I confirm, then the to-do is permanently deleted and does not reappear after reload or restart.
- AC-4: Given a deleted to-do, when I try to update or delete it again, then I see a not-found message.
- AC-5: Given I delete the only to-do on the last page, when the list refreshes, then I see the last valid page, not an error (Q33: a page past the last shows the last valid page).

## REQ-TODO-007 Clear all completed (M)

Traces: Q19, Q33, Q34, Q38.

- AC-1: Given I have Completed to-dos, when I choose "Clear all completed", then the UI asks for confirmation that states the number of Completed to-dos that will be deleted, that this covers all of my Completed to-dos and not only those shown, and that deletion is permanent (count shown: Q33). When the number is 1 the sentence that states the number uses the singular ("Delete 1 completed to-do? …"); for any other number it uses the plural ("Delete all N completed to-dos? …"); the full texts of that sentence are fixed in the SDD (singular form: Q38). The rule covers only that sentence, not the dialog's heading or buttons. Source: clarifications Q38.
- AC-2: Given the confirmation, when I confirm, then all of my Completed to-dos, across all pages and regardless of the current filter or search, are permanently deleted and my Active to-dos are untouched (scope: Q33).
- AC-3: Given the confirmation, when I cancel, then nothing is deleted.
- AC-4: Given I have no Completed to-dos, when I view the list, then the "Clear all completed" control is disabled (Q34).

## Listing: filter, search, pagination

## REQ-LIST-001 Filter by status (M)

Traces: Q9.

- AC-1: Given a mix of to-dos, when I select All, then both Active and Completed to-dos are listed.
- AC-2: Given a mix, when I select Active, then only not-done to-dos are listed.
- AC-3: Given a mix, when I select Completed, then only done to-dos are listed.
- AC-4: Given a filter that matches nothing, when applied, then a "no matching to-dos" state is shown (distinct from the first-use empty state in REQ-TODO-003 AC-2).
- AC-5: Given no filter is chosen, when the list opens, then All is selected.

## REQ-LIST-002 Search (M)

Traces: Q9, Q20, Q33.

- AC-1: Given to-dos with the text "Buy Milk" in a title, when I search "milk", then it is listed (case-insensitive contains).
- AC-2: Given a to-do whose description (not title) contains the search text, when I search, then it is listed.
- AC-3: Given a search and a filter selected together, when applied, then only to-dos matching both are listed.
- AC-4: Given a search term that matches nothing, when applied, then a "no matching to-dos" state is shown.
- AC-5: Given an empty search box, when the list loads, then no search restriction applies.
- AC-6: Given a search term containing characters such as `%`, `_` or quotes, when applied, then they are matched literally and cause no error (Q33).
- AC-7: Given search text that is only leading or trailing whitespace, when applied, then it behaves as an empty search (Q33).

## REQ-LIST-003 Pagination (M)

Traces: Q9, Q21, Q29, Q33, Q34.

- AC-1: Given more than 20 matching to-dos, when I open the list, then exactly 20 are shown on page 1, newest first, with numbered page links and Previous and Next controls.
- AC-2: Given I am on page 2, when I choose Next or Previous or a page number, then the corresponding page is shown; the order is consistent across pages with no to-do repeated or skipped.
- AC-3: Given I am on the first page, when I view the controls, then Previous is disabled; on the last page Next is disabled.
- AC-4: Given 20 or fewer matching to-dos, when the list opens, then a single page is shown and no Next is available.
- AC-5: Given filter or search changes, when applied, then the list returns to page 1 and the page count reflects the matches (Q33).
- AC-6: Given a requested page number beyond the last page (for example by URL or API), when requested, then the last valid page is shown and no error occurs (Q34). When there are no matches at all, the empty or "no matching to-dos" state is shown.
- AC-7: Given a page number that is zero, negative or not a number, when requested, then the API refuses it with a validation error (analyst default).
- AC-8: Given the page size, when the list is requested, then it is fixed at 20 and the user cannot change it (Q29).

## Navigation

## REQ-NAV-001 Home route and not-found page (M)

The web app's root address goes to the to-do list, and an address the web app does not define shows an animated "not found" page with a link back to home. "Home" is the root address `/`. Traces: Q37. Source: clarifications Q37.

- AC-1: Given I am signed in, when I open `/`, then I am sent to my to-do list at `/todos` (filter All, no search, page 1, whatever query string `/` had; analyst default).
- AC-2: Given I am not signed in, when I open `/`, then I am sent to `/todos` and from there to the login page as for any to-do page (REQ-AUTH-004 AC-2), and after a successful login I see my to-do list.
- AC-3: Given an address that matches none of the web app's pages (for example `/nope`, `/todos/abc/def` or `/login/extra`), when I open it, signed in or not, then a "not found" page is shown with a link back to home, and I am not sent to the login page.
- AC-4: Given I am signed in and an address of the form `/todos/<id>` or `/todos/<id>/edit` whose id does not name one of my to-dos (including an id that is not a valid to-do identifier), when I open it, then the web app shows its to-do not-found message, the same as for a to-do that does not exist (REQ-ISO-001 AC-2), and not the page of AC-3 (analyst default).
- AC-5: Given the "not found" page, when I choose the link back to home, then I go to `/`, which behaves as in AC-1 and AC-2.
- AC-6: Given the "not found" page, when it is shown, then its animation is decorative (the page's heading, text and link are readable and usable without it), it plays once (analyst default) and stops within 5 seconds (WCAG 2.2.2 Pause, Stop, Hide), and it does not flash more than three times in any one second (WCAG 2.3.1 Three Flashes or Below Threshold); both criteria are Level A, so they are part of NFR-001's WCAG 2.1 Level AA.
- AC-7: Given the "not found" page, when audited as in NFR-001 AC-1 and operated as in NFR-001 AC-2, then no WCAG 2.1 Level AA failure is found and the link back to home is reachable and operable by keyboard with visible focus.

## Data

## REQ-DATA-001 Persistence (M)

Traces: Q4, Q13, Q22, Q33.

- AC-1: Given users and to-dos created, when the application is stopped and started again, then all accounts and to-dos (including done status, due date, priority, timestamps) are still present and unchanged.
- AC-2: Given the application storage, when inspected, then data is held in a SQLite file database (Q13).
- AC-3: Given the database file is absent at first start, when the app starts, then it is created and the app works with no manual database setup (Q33).

## REQ-DATA-002 Concurrent edits: last write wins (M)

Traces: Q18.

- AC-1: Given two edits to the same to-do arrive one after the other (for example from two browser tabs), when both are accepted, then the stored to-do holds the values of the later write and no conflict error is shown.
- AC-2: Given concurrent edits to different to-dos, when both complete, then each is stored independently with no data lost.

## REQ-ACC-001 Overall success scenario (M)

Traces: Q22 ("A signed-in user can add, see, search, filter, edit, complete and delete their own to-dos; another user cannot see them; and the to-dos are still there after a restart.")

- AC-1: Given a signed-in user, when they add, see, search, filter, edit, complete and delete their own to-dos, then every step works as described in REQ-TODO-001 to REQ-TODO-006 and REQ-LIST-001 to REQ-LIST-003.
- AC-2: Given a second user is signed in, when they look for the first user's to-dos by listing, searching or direct identifier, then they cannot see or change them (REQ-ISO-001).
- AC-3: Given the application is restarted, when the first user logs in again, then their to-dos are still there (REQ-DATA-001).

## Non-functional requirements

## NFR-001 Accessibility (M)

Traces: Q10, Q36, Q44.

- AC-1: Given the UI, when audited against WCAG 2.1 Level AA, then no AA failure is found on the sign-up, login, list, create/edit form, delete confirmation and clear-completed confirmation screens.
- AC-2: Given only a keyboard, when I sign up, log in, and create, edit, complete, delete, filter, search and page, then every action is reachable and operable, and focus is visible and moves into and out of confirmation dialogs.
- AC-3: Given a validation error, when it appears, then it is associated with its field and announced to assistive technology.
- Note (Q36): the 8-hour session of REQ-AUTH-002 AC-4 is absolute from login (sdd §4, ADR-0008): it ends with no warning before expiry, it cannot be extended, and input typed but not saved when it ends is lost. The requester accepts this under the "essential" exception of WCAG success criterion 2.2.1 Timing Adjustable, so the audit of AC-1 records 2.2.1 as met by that exception, not as a failure. The audit level stays WCAG 2.1 Level AA (Q44). Source: clarifications Q36.

## NFR-002 Run locally (M)

Traces: Q10, Q27, Q28.

- AC-1: Given a developer machine with the documented prerequisites, when the app is started, then the UI and API are usable on that machine with no cloud, hosted database or email service.
- AC-2: Given the first release, when delivered, then no deployment to a `dev` or `test` environment is required.

## NFR-003 Responsiveness (M)

Traces: Q10, Q33.

- AC-1: Given one user with up to 500 to-dos on the local machine, when they list, filter, search, page, create, edit, complete or delete, then the result is shown within 1 second in at least 95% of attempts (Q10 "feels instant" for a few hundred items; the 95% / 1 second / 500 figures accepted in Q33).

## NFR-004 Security baseline (M)

Traces: security-baseline note (AC-1), Q14.

- AC-1: Given the requirements in REQ-AUTH-003, when reviewed, then password hashing meets them (strong salted one-way hash).
- AC-2: Given a request with a to-do or user input, when processed, then input is validated on the server, not only in the UI, and the API never returns stack traces or SQL errors to the client (security baseline elaboration; analyst default).
- AC-3: Given the repository, when scanned, then no secrets or real credentials are committed (CLAUDE.md rule 10).

## NFR-005 Technology stack (M)

Traces: Q2, Q3, Q4, Q13.

- AC-1: Given the delivered system, when inspected, then the UI is React + TypeScript, the API is Java/Spring Boot, the browser talks to the API over HTTP, and storage is SQLite.

## NFR-006 User-friendly error messages (M)

The web app shows every error, and every unexpected failure of its own, as a user-friendly message with next steps. Traces: Q39, NFR-004 AC-2. Source: clarifications Q39.

- AC-1: Given any operation in the web app fails (a refused input, a to-do that no longer exists, a refused login, a taken identifier, an ended session, an unreachable server, or any other error response), when the web app shows the failure, then the message says in plain language what went wrong and gives at least one next step I can take (for example correct the named field, try again, log in again, or go back to my to-dos).
- AC-2: Given any failure message the web app shows, when inspected, then it contains no technical internals: no stack trace, exception or class name, SQL, error `code` value (such as `INTERNAL_ERROR`), request id, the response body as a whole, or browser error text (such as "Failed to fetch"), consistent with NFR-004 AC-2.
- AC-3: Given an unexpected error inside the web app itself (one not reported by the API), when it occurs, then instead of a blank or broken screen a user-friendly message is shown with a way to continue (reload the page or go to home), and with no technical detail as in AC-2.
- AC-4: Given a refused login, when the message and its next step are shown, then they are identical for a wrong password and for an identifier that does not exist (REQ-AUTH-002 AC-2, AC-3 still hold).

## Out of scope

- Email verification (Q27) and password reset or "forgot password" (Q28) in the first release.
- Admin role, user listing or disabling, and any view of other users' to-dos (Q15).
- Login through an external identity provider (Q14).
- Separate email and username fields (Q31 chose one identifier field).
- Soft delete, undo or recovery of deleted to-dos (Q8).
- Time of day or time zones on due dates (Q25).
- User-selectable sorting (for example by due date or priority); the list is ordered newest first only (Q9).
- Page size selection, infinite scroll or "load more" (Q21, Q29).
- Deployment to `dev`/`test` or any hosted environment (Q10).
- Optimistic conflict rejection on concurrent edits (Q18).
- Sharing, teams, tags, sub-tasks, reminders or notifications (not requested).

## Analyst defaults (for the approver to confirm at freeze)

The requester did not decide these; they are tagged `(analyst default)` above:

1. Identifier: trimmed, 3–254 characters (REQ-AUTH-001 AC-8).
2. Identifier containing "@" is an email and must be a valid email address (REQ-AUTH-001 AC-9).
3. Identifier without "@" is a username of only A–Z, a–z, 0–9, ".", "_", "-" (REQ-AUTH-001 AC-10).
4. Password maximum 128 characters (REQ-AUTH-001 AC-11).
5. A page number that is zero, negative or not a number is refused with a validation error (REQ-LIST-003 AC-7); Q33 and Q34 cover only a page past the last.
6. Server-side input validation and no stack traces or SQL errors in API responses, as a security baseline elaboration (NFR-004 AC-2).
7. `/` goes to `/todos` with filter All, no search and page 1, and any query string on `/` is not carried over (REQ-NAV-001 AC-1); Q37 says only that `/` goes to `/todos`.
8. `/todos/<id>` and `/todos/<id>/edit` with an id that does not name one of my to-dos, including an id that is not a valid to-do identifier, show the to-do not-found message, not the "not found" page (REQ-NAV-001 AC-4); Q37 says only that unknown routes show the "not found" page.
9. The "not found" page's animation plays once (REQ-NAV-001 AC-6); Q37 asks only for an animated page.

## Open questions

None.
