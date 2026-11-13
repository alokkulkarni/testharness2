---
stage: requirements
changeId: CHG-0004
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0004
    agent: requirements-analyst
    agentRunId: run-chg4-n02-1
upstream: []
---

# Requirements — CHG-0004

## Context and goal

CHG-0001's SDD (§14, rows NFR-001 and REQ-NAV-001; §9; §11) asks for accessibility evidence its pack lacks: announcement
and state checks, a contrast pass, a recorded run of the not-found animation and a flash check (WCAG 2.3.1). This change
adds that evidence as automated Playwright tests, written test-first, run against develop's existing web app, and
records it. It is audit-only: it changes no product code. The manual screen-reader pass stays a human task, recorded as
an open note. Requester: @alokkulkarni. Audit level stays WCAG 2.1 Level AA (CHG-0001 Q44).

Glossary:
- "Audited screens" are the eight screens of CHG-0001 SDD §14 (sign-up, login, list, create/edit form, delete dialog,
  clear-completed dialog, to-do detail, not-found page).
- "Evidence" is what `harness evidence attach` copies into `.harness/changes/CHG-0004/evidence/`.
- "Summary" is the one Markdown evidence file of REQ-A11Y-011.
- "Holds the text" means the element's text contains the quoted text; "is the text" is not used. Where a quoted text
  differs from what the app renders, the app's text is checked (analyst defaults 1 and 2).
- "Polite live region" means an element with `role="status"` or `aria-live="polite"`, the attribute explicit or implied
  by the role (analyst default 3).
- "Spec-wording difference" means a place where the CHG-0001 spec or the requester's Q10 wording differs from what the
  app renders, and the difference is not a WCAG failure (REQ-A11Y-012 AC-5).

## Functional requirements

### Announcements and states (accessibility tree)

Playwright cannot hear a screen reader. These checks read the accessibility tree: each element's role, accessible name
and state, the live region a message appears in, and where focus lands. Source: clarifications Q10 (and Q2, Q3 for the
run).

## REQ-A11Y-001 Sign-up, login and form announcements (M)

Evidences CHG-0001 NFR-001 AC-3 and AC-1. Source: clarifications Q10 items 1-3.

- AC-1: Given the sign-up or login form, when I submit it with invalid or missing fields, then an error summary with `role="alert"` is shown and holds focus, and it contains a link to each field in error.
- AC-2: Given a refused submit on sign-up or login, when I read the accessibility tree, then each field in error has `aria-invalid="true"` and an accessible description (`aria-describedby`) that includes the text of its field error (the description may also carry the "Error:" prefix and help text).
- AC-3: Given I have just created an account, when the login page is shown, then a `role="status"` region holds the text "Account created. Log in." (analyst default 1: the app renders this wording, `web/src/auth/LoginPage.tsx:L14`, in a `role="status"` message at L89; Q10 item 2 and CHG-0001 write "Account created — log in").
- AC-4: Given my session has ended, when the login page is shown, then the text "Your session has ended. Please log in again." is announced in a live region of role `status` or `alert` (analyst default 4).
- AC-5: Given a login with a wrong identifier or password, when it is refused, then a `role="alert"` element holds the text "Invalid identifier or password. Check them and try again."
- AC-6: Given the create form and the edit form, when I submit them with an empty title, then the error summary, link and field-error checks of AC-1 and AC-2 hold for them.
- AC-7: Given I open the create route or the edit route, when the page has rendered, then the `<h1>` "Add a to-do" or "Edit to-do" holds focus.

## REQ-A11Y-002 List announcements and focus (M)

Evidences CHG-0001 NFR-001 AC-2 and AC-3. Source: clarifications Q10 items 4-5.

- AC-1: Given the list has 45 to-dos, when page 1 is shown, then a polite live region holds the text "Showing 1-20 of 45" with a hyphen-minus (analyst default 2: the app renders it so, `web/src/todos/TodoListPage.tsx:L314`, as a `<p role="status">`; Q10 item 4 and CHG-0001 SDD §9 at `docs/specs/CHG-0001/sdd.md:L558` write an en dash).
- AC-2: Given a user with no to-dos, when the list is shown, then a level-2 heading "You have no to-dos yet" is in the accessibility tree; and given a search with no match, then a level-2 heading "No matching to-dos" is (analyst default 5).
- AC-3: Given the list, when I read the accessibility tree, then the filter is a group with an accessible name containing radio inputs, exactly one of them checked.
- AC-4: Given more than one page, when I read the accessibility tree, then a navigation landmark named "Pagination" exists and the current page's link has `aria-current="page"`.
- AC-5: Given the list, when I choose a pagination link, then focus lands on the `<h1>` "My to-dos".
- AC-6: Given the list, when I change the filter or run a search, then focus is on the control I used.
- AC-7: Given the list has no completed to-dos at the moment I choose to clear completed (for example after a clear in another tab), when the action is refused, then the polite live region (the shell's, `aria-live="polite"`) holds "There are no completed to-dos to clear." and no dialog opens.
- AC-8: Given a to-do that no longer exists when I act on it from the list, when the action is refused, then an element with `role="alert"` holds "That to-do no longer exists, and the list has been refreshed."

## REQ-A11Y-003 Confirmation dialogs (M)

Evidences CHG-0001 NFR-001 AC-2. Source: clarifications Q10 item 6.

- AC-1: Given the delete dialog or the clear-completed dialog is open, when I read the accessibility tree, then it has role `dialog` and its accessible name is its heading.
- AC-2: Given either dialog has just opened, when I check focus, then focus is on its Cancel button.
- AC-3: Given either dialog is open, when I close it with Cancel or Escape, then focus returns to the control that opened it (`web/src/todos/ConfirmDialog.tsx:L42-L52`).
- AC-4: Given the delete dialog is open, when I confirm and the to-do is deleted, then the opener no longer exists and focus is on the list heading (analyst default 6: `web/src/todos/TodoListPage.tsx:L244`; Q10 item 6 says "the invoking control (or the list heading)").
- AC-5: Given the clear-completed dialog is open, when I confirm and the completed to-dos are cleared, then focus is on the list heading (analyst default 6: `web/src/todos/TodoListPage.tsx:L268`).

## REQ-A11Y-004 To-do detail and not-found pages (M)

Evidences CHG-0001 NFR-001 AC-1 and REQ-NAV-001 AC-6 and AC-7. Source: clarifications Q10 items 7-8.

- AC-1: Given I open a to-do's detail page, when it has rendered, then the `<h1>` holds focus and `document.title` is set (not empty, and different from the sign-up page's title).
- AC-2: Given I open a detail address whose to-do does not exist, when the to-do not-found state has rendered, then the `<h1>` holds focus and `document.title` is set.
- AC-3: Given I am signed in and open an address the web app does not define, when the not-found page first renders, then its `<h1>`, its sentence and its link back to home are in the accessibility tree.
- AC-4: Given the not-found page, when I read the accessibility tree, then the animated element is hidden from it (`aria-hidden="true"`) and contributes no name or text.

## REQ-A11Y-005 Start-up alert (M)

Evidences CHG-0001 NFR-001 AC-3 (SDD §4.1). Source: clarifications Q10 item 9.

- AC-1: Given the web app cannot reach the server at start-up, when it opens, then an element with `role="alert"` holds the text "The app could not reach the server." (it continues "Please try again.", `web/src/errors/userMessage.ts:L44`) and a button named "Retry" is in the accessibility tree and operable by keyboard.

### Contrast

## REQ-A11Y-006 Non-text contrast, WCAG 1.4.11 (M)

Evidences CHG-0001 NFR-001 AC-1. Source: clarifications Q7.

- AC-1: Given each audited screen, when the test measures the visible focus outline of every kind of focusable control against its adjacent colour, then each ratio is at least 3:1.
- AC-2: Given each audited screen, when the test measures the border of every input, checkbox, radio and button against its adjacent colour, then each ratio is at least 3:1. For a control identified by its fill, the fill against its adjacent colour is measured (analyst default 7).
- AC-3: Given each audited screen, when the test measures the hover state and the selected state of each control that has one against its adjacent colour, then each ratio is at least 3:1.
- AC-4: Given a control in a disabled state, when the test measures it, then its ratio is listed in the ratio table (REQ-A11Y-011 AC-3) and is not asserted.
- AC-5: Given any measurement in AC-1 to AC-3 is under 3:1, when the test runs, then it fails and names the control, the state and the measured ratio.

"Every kind of control and state" means each kind that exists on an audited screen; "adjacent colour" is the colour the control sits against (analyst default 7).

## REQ-A11Y-007 Text contrast, WCAG 1.4.3 (M)

Evidences CHG-0001 NFR-001 AC-1 and SDD §9 ("text contrast ≥ 4.5:1"). Source: clarifications Q7.

- AC-1: Given each audited screen, when the test measures the text of every control in each of its states (normal, focus, hover, selected), then each ratio against its background is at least 4.5:1, or at least 3:1 when the text is large text as WCAG 2.1 defines it.
- AC-2: Given a control in a disabled state, when the test measures its text, then its ratio is listed in the ratio table and not asserted.
- AC-3: Given any measurement in AC-1 is under its threshold, when the test runs, then it fails and names the control, the state and the measured ratio.
- AC-4: Given the existing axe contrast rule in `web/e2e/a11y.spec.ts`, when the new tests run, then that rule and its tests are unchanged and still pass; the ratio table adds measured values to them.

### Not-found animation

## REQ-A11Y-008 Video of the not-found animation (M)

Evidences CHG-0001 REQ-NAV-001 AC-6 (SDD §14 row: "a screen recording of its full run"). Source: clarifications Q1, Q2, Q3.

- AC-1: Given a signed-in user opening an address the web app does not define, when the video test runs in chromium, then Playwright records a video of the page covering the whole run of the animation, and the video file is produced.
- AC-2: Given the same test in webkit, when it runs, then the same video is produced.
- AC-3: Given the other new tests (including the REQ-A11Y-004 and REQ-A11Y-009 tests that open the not-found page) and the existing tests, when they run, then they record no video; video is on only for the REQ-A11Y-008 tests (Q2).
- AC-4: Given the recorded videos, when the evidence is assembled, then each video is a `.webm` file of at most 1 MB (analyst default 8; the requester said "a few hundred KB each").

## REQ-A11Y-009 Automated flash check, WCAG 2.3.1 (M)

Evidences CHG-0001 REQ-NAV-001 AC-6 (no more than three flashes in any one second). Source: clarifications Q8, Q3.

- AC-1: Given the not-found page, when the test pauses the page's animations and steps them through their whole run with the Web Animations API at 60 samples per second, then it takes one viewport screenshot at each step.
- AC-2: Given the screenshots, when the test computes per-pixel relative luminance and red saturation, then it counts general flashes and red flashes (a flash is a pair of opposing changes in relative luminance of at least 10% of the maximum, with the darker state below 0.80; the red-flash rule likewise for saturated red, as in WCAG 2.3.1) in every one-second window of the run. The count of a window is the highest count of any single pixel in it, and the windows slide one sample at a time (analyst default 9).
- AC-3: Given the counts, when the largest count in any one-second window is at most 3 for general flashes and at most 3 for red flashes, then the test passes; otherwise it fails and reports the window and the count.
- AC-4: Given the whole viewport, when the flashes are counted, then the small-area exemption of WCAG 2.3.1 is not applied (the stricter reading).
- AC-5: Given the test, when it runs in chromium and in webkit, then the animation is paused and stepped the same way in both (as AC-1), and the test passes in both.
- AC-6: Given the sampled run, when the test finishes, then it records the number of samples, the run length covered and the highest general and red flash counts, for the summary.

### Run, evidence and open items

## REQ-A11Y-010 What the tests run against (M)

Source: clarifications Q2, Q3, Q4 (audit-only).

- AC-1: Given the new tests, when I look at the repository after this change, then they are in `web/e2e/`, run under the main Playwright configuration (the SDD §11 run: API and Vite dev server), and `pnpm -C web e2e` includes them.
- AC-2: Given the new tests, when they run, then they run in chromium and webkit (the projects in `tests.requiredProjects`) and pass in both; they do not run in firefox (Q3 a).
- AC-3: Given this change's diff, when I review it, then it adds or changes only tests, test support, evidence and documentation: no file of the web app's or the API's product code (outside `web/e2e/` and its support files) is changed. Changes to `web/playwright.config.ts` are allowed only to keep the new tests out of firefox and to switch video on for the REQ-A11Y-008 tests; no new devDependency is added and `web/package.json` and the lockfile are unchanged (analyst default 10).
- AC-4: Given the history of this change, when I read it, then every `test(...)` commit that adds a new test precedes any other non-documentation commit of the change; each new test's first run against develop is recorded in the summary as passed or failed; no failure is made up (no stub or artificial failing state is added to get a red commit); a test that passes on its first run is recorded as such; and a test that fails on its first run is handled by REQ-A11Y-012 (analyst default 11).
- AC-5: Given the history of this change, when I read it, then no test is weakened, skipped or deleted to get green, and none uses `test.fail()` or `test.skip()` to hide a failure.
- AC-6: Given a new test, when I read its name, then it starts with the requirement ID and AC it proves.

## REQ-A11Y-011 Evidence (M)

Source: clarifications Q1.

- AC-1: Given the runs that include chromium and webkit, when the evidence is attached, then the Playwright JUnit XML file or files those runs wrote (the main configuration's JUnit file for a run including both projects, or one file per project run) are attached as kind `test-report` under `.harness/changes/CHG-0004/evidence/` and committed (analyst default 12).
- AC-2: Given REQ-A11Y-008, when the evidence is attached, then the chromium and the webkit videos of the not-found animation are attached as kind `other` and committed.
- AC-3: Given the audit, when the evidence is attached, then one Markdown summary (kind `other`) is attached and committed that holds: the contrast ratio table (each control and state, its ratio, its threshold, asserted or not; disabled states marked "measured, not asserted"), the flash-check result (REQ-A11Y-009 AC-6), the announcement and state results (REQ-A11Y-001 to REQ-A11Y-005), the first-run result of each new test (REQ-A11Y-010 AC-4), every spec-wording difference (REQ-A11Y-012 AC-5), and the open screen-reader note of REQ-A11Y-013.
- AC-4: Given the Playwright HTML report, when I list the evidence, then it is not among the attached files (it stays local).

## REQ-A11Y-012 A real WCAG failure, and spec-wording differences (M)

Source: clarifications Q4.

- AC-1: Given a new test fails against develop because develop's application has a real WCAG failure, when this change is at its verification stage, then the failing test stays failing, the summary records the failure (screen, control or state, criterion and measured value), and this change does not merge.
- AC-2: Given such a failure, when it is followed up, then it is fixed by a separate change (test-first, with this test as its red); this change merges only after that fix is on develop and the test passes.
- AC-3: Given such a failure, when I read this change, then it has not fixed the product code itself, and has not marked the test as an expected failure or skipped it (REQ-A11Y-010 AC-3, AC-5).
- AC-4: Given all new tests pass against develop, when the summary is written, then it states that no failure was found.
- AC-5: Given the CHG-0001 spec or Q10 wording differs from what the app renders and the difference is not a WCAG failure (analyst defaults 1, 2, 3, 6), when the test checks the app's rendered text or role, then the test passes, the difference is not counted as a WCAG failure and does not block the merge, and the summary lists it with the spec wording, the app wording and the `file:line` of the app (analyst default 13).

## REQ-A11Y-013 Manual screen-reader pass: recorded as open (M)

Evidences the manual pass that CHG-0001 SDD §14 (NFR-001 row) names and that is not done. Source: clarifications Q5, Q6.

- AC-1: Given the summary, when I read it, then it has an open note for the manual screen-reader pass naming the person (@alokkulkarni), the screen reader and browser (VoiceOver and Safari on macOS), and the eight audited screens.
- AC-2: Given the note, when I read it, then it states that the pass is not done; it is nowhere written as done, passed or waived, in the summary, the knowledge bundle or any other file of this change.
- AC-3: Given the note, when this change is verified and merged, then the open note does not block the merge; the pass is recorded later in a change of its own.

## REQ-A11Y-014 Knowledge-bundle open-gap notes (M)

Source: clarifications Q9.

- AC-1: Given this change's docs node has run, when I read `docs/knowledge/requirements/nfr-001.md`, `docs/knowledge/requirements/req-nav-001.md` and `docs/knowledge/tests/accessibility-tests.md`, then each says that the automated accessibility-tree, contrast, video and flash checks are recorded in CHG-0004.
- AC-2: Given the same three notes, when I read them, then each still says the human screen-reader pass is open and not done (REQ-A11Y-013 AC-2).
- AC-3: Given the same three notes, when I read them, then none still says the flash review or the contrast pass is unrecorded, unless a failure of REQ-A11Y-012 leaves it open, in which case it names that failure.

## Non-functional requirements

## NFR-101 Reproducible measurements (M)

Evidences CHG-0001 NFR-001. Source: clarifications Q7, Q8.

- AC-1: Given two runs of the new tests on the same browser and the same code, when they finish, then the contrast ratios in the table agree to two decimal places (analyst default 14) and the flash counts are equal.
- AC-2: Given the flash check, when it runs, then it steps the animation by the Web Animations API rather than by wall-clock waits, so its result does not depend on the machine's speed.

## NFR-102 Suite impact (S)

Source: clarifications Q2.

- AC-1: Given the new tests are in `pnpm -C web e2e`, when the suite runs, then the existing tests still pass unchanged.
- AC-2: Given the new spec files run alone in one browser on the build host, when I time the run wall-clock, then it takes no more than 5 minutes (analyst default 15).

## NFR-103 Security and data (M)

Source: CLAUDE.md rules 9 and 10.

- AC-1: Given the new tests, the videos and the summary, when I review them, then they hold no secret or real credential; test accounts use the generated test identifiers and passwords of the existing test support only.

## Out of scope

- Fixing any product code, including a real WCAG failure the tests find (a separate change, REQ-A11Y-012; Q4).
- Firefox for the new tests (Q3).
- The manual screen-reader pass itself, any NVDA or Windows pass, and any other human review (Q5, Q6).
- A production build (`vite build`, `vite preview`) as the target of the tests (Q2 c).
- A separate on-demand Playwright configuration for the new tests (Q2 b).
- Frame extraction from the recorded videos or `ffmpeg` (Q8 b).
- Screenshots taken during real playback without pausing (Q8 c).
- Putting the Playwright HTML report in evidence (Q1).
- Disabled states as assertions (they are measured and listed only, Q7).
- Updating the knowledge-bundle notes in a later separate docs change (Q9 b); this change's docs node does it (REQ-A11Y-014).
- WCAG criteria beyond those above (the existing axe checks keep covering the rest); a level above WCAG 2.1 AA.
- Changing CHG-0001's frozen spec or its verdicts.

## Analyst defaults (for the approver to confirm at freeze)

Each is tagged inline in the AC it affects as "(analyst default N)".

1. Login "account created" text (REQ-A11Y-001 AC-3): the test checks the app's "Account created. Log in." (`web/src/auth/LoginPage.tsx:L14`, `role="status"` at L89), not Q10 item 2's and CHG-0001's "Account created — log in".
2. Result-count text (REQ-A11Y-002 AC-1): the test checks "Showing 1-20 of 45" with a hyphen-minus (`web/src/todos/TodoListPage.tsx:L314`), not the en dash of Q10 item 4 and `docs/specs/CHG-0001/sdd.md:L558`.
3. Polite live region: a `role="status"` or `aria-live="polite"` element, explicit or implied, because the list result text is a `<p role="status">` without an `aria-live` attribute (`web/src/todos/TodoListPage.tsx:L314`), while Q10 item 4 and CHG-0001 SDD §9 name `aria-live="polite"`.
4. Session-ended message (REQ-A11Y-001 AC-4): a live region of role `status` or `alert`; the answer gives the text, not the role.
5. Empty states (REQ-A11Y-002 AC-2): checked as level-2 headings "You have no to-dos yet" and "No matching to-dos" (`web/src/todos/TodoListPage.tsx:L295`, L305); the filter group's accessible name is not fixed.
6. Dialog focus on confirm (REQ-A11Y-003 AC-3 to AC-5): Cancel and Escape return focus to the opener; a confirmed delete or confirmed clear-completed moves focus to the list heading, as the app does (`TodoListPage.tsx:L244`, L268).
7. Contrast terms (REQ-A11Y-006): "every kind and state" means each kind that exists on an audited screen; "adjacent colour" is the colour the control sits against; a control identified by its fill is measured by that fill.
8. Video size (REQ-A11Y-008 AC-4): at most 1 MB per video; the requester's wording is "a few hundred KB each".
9. Flash counting (REQ-A11Y-009 AC-2): the count of a window is the highest count of any single pixel; windows slide one sample at a time; the run covered is the whole animation run.
10. Test configuration (REQ-A11Y-010 AC-3): `web/playwright.config.ts` may change only to exclude firefox for the new tests and switch video on for the REQ-A11Y-008 tests; no new devDependency, no lockfile change.
11. Test-first order in an audit-only change (REQ-A11Y-010 AC-4): tests are committed in `test(...)` commits before any other non-documentation commit; a test that passes on its first run against develop is recorded as such, with no artificial red state; a test that fails on its first run is handled by REQ-A11Y-012.
12. JUnit evidence (REQ-A11Y-011 AC-1): the JUnit file or files written by the runs that include chromium and webkit.
13. Spec-wording differences (REQ-A11Y-012 AC-5): a difference between spec wording and the app that is not a WCAG failure is checked against the app, recorded in the summary and does not block the merge.
14. Reproducibility (NFR-101 AC-1): contrast ratios agree to two decimal places.
15. Suite time (NFR-102 AC-2): the new spec files alone take at most 5 minutes in one browser, wall-clock, on the build host.

## Open questions

None.
