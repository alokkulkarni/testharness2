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

Glossary: "audited screens" are the eight screens of CHG-0001 SDD §14 (sign-up, login, list, create/edit form, delete
dialog, clear-completed dialog, to-do detail, not-found page). "Evidence" is what `harness evidence attach` copies into
`.harness/changes/CHG-0004/evidence/`. "Summary" is the one Markdown evidence file of REQ-A11Y-011.

## Functional requirements

### Announcements and states (accessibility tree)

Playwright cannot hear a screen reader. These checks read the accessibility tree: each element's role, accessible name
and state, the live region a message appears in, and where focus lands. Source: clarifications Q10 (and Q2, Q3 for the
run).

## REQ-A11Y-001 Sign-up, login and form announcements (M)

Evidences CHG-0001 NFR-001 AC-3 and AC-1. Source: clarifications Q10 items 1-3.

- AC-1: Given the sign-up or login form, when I submit it with invalid or missing fields, then an error summary with `role="alert"` is shown and holds focus, and it contains a link to each field in error.
- AC-2: Given a refused submit on sign-up or login, when I read the accessibility tree, then each field in error has `aria-invalid="true"` and an accessible description (`aria-describedby`) that is the text of its field error.
- AC-3: Given I have just created an account, when the login page is shown, then the text "Account created — log in" is in a `role="status"` region.
- AC-4: Given my session has ended, when the login page is shown, then the text "Your session has ended. Please log in again." is announced in a live region of role `status` or `alert`.
- AC-5: Given a login with a wrong identifier or password, when it is refused, then the text "Invalid identifier or password. Check them and try again." is in a `role="alert"` element.
- AC-6: Given the create form and the edit form, when I submit them with an empty title, then the error summary, link and field-error checks of AC-1 and AC-2 hold for them.
- AC-7: Given I open the create route or the edit route, when the page has rendered, then the `<h1>` "Add a to-do" or "Edit to-do" holds focus.

## REQ-A11Y-002 List announcements and focus (M)

Evidences CHG-0001 NFR-001 AC-2 and AC-3. Source: clarifications Q10 items 4-5.

- AC-1: Given the list has 45 to-dos, when page 1 is shown, then a polite live region (`aria-live="polite"`) holds the text "Showing 1–20 of 45".
- AC-2: Given a user with no to-dos, when the list is shown, then the first-use empty state is in the accessibility tree; and given a search with no match, then the text "No matching to-dos" is.
- AC-3: Given the list, when I read the accessibility tree, then the filter is a group with an accessible name containing radio inputs, exactly one of them checked.
- AC-4: Given more than one page, when I read the accessibility tree, then a navigation landmark named "Pagination" exists and the current page's link has `aria-current="page"`.
- AC-5: Given the list, when I choose a pagination link, then focus lands on the `<h1>` "My to-dos".
- AC-6: Given the list, when I change the filter or run a search, then focus is on the control I used.
- AC-7: Given the list has no completed to-dos at the moment I choose to clear completed (for example after a clear in another tab), when the action is refused, then the polite live region holds "There are no completed to-dos to clear." and no dialog opens.
- AC-8: Given a to-do that no longer exists when I act on it from the list, when the action is refused, then an element with `role="alert"` holds "That to-do no longer exists, and the list has been refreshed."

## REQ-A11Y-003 Confirmation dialogs (M)

Evidences CHG-0001 NFR-001 AC-2. Source: clarifications Q10 item 6.

- AC-1: Given the delete dialog or the clear-completed dialog is open, when I read the accessibility tree, then it has role `dialog` and its accessible name is its heading.
- AC-2: Given either dialog has just opened, when I check focus, then focus is on its Cancel button.
- AC-3: Given either dialog is open, when I close it (Cancel or Escape), then focus returns to the control that opened it, or to the list heading when that control no longer exists.

## REQ-A11Y-004 To-do detail and not-found pages (M)

Evidences CHG-0001 NFR-001 AC-1 and REQ-NAV-001 AC-6 and AC-7. Source: clarifications Q10 items 7-8.

- AC-1: Given I open a to-do's detail page, when it has rendered, then the `<h1>` holds focus and `document.title` is set (not empty, and different from the sign-up page's title).
- AC-2: Given I open a detail address whose to-do does not exist, when the to-do not-found state has rendered, then the `<h1>` holds focus and `document.title` is set.
- AC-3: Given I am signed in and open an address the web app does not define, when the not-found page first renders, then its `<h1>`, its sentence and its link back to home are in the accessibility tree.
- AC-4: Given the not-found page, when I read the accessibility tree, then the animated element is hidden from it (`aria-hidden="true"`) and contributes no name or text.

## REQ-A11Y-005 Start-up alert (M)

Evidences CHG-0001 NFR-001 AC-3 (SDD §4.1). Source: clarifications Q10 item 9.

- AC-1: Given the web app cannot reach the server at start-up, when it opens, then an element with `role="alert"` holds "The app could not reach the server." and a button named "Retry" is in the accessibility tree and operable by keyboard.

### Contrast

## REQ-A11Y-006 Non-text contrast, WCAG 1.4.11 (M)

Evidences CHG-0001 NFR-001 AC-1. Source: clarifications Q7.

- AC-1: Given each audited screen, when the test measures the visible focus outline of every kind of focusable control against its adjacent colour, then each ratio is at least 3:1.
- AC-2: Given each audited screen, when the test measures the border of every input, checkbox, radio and button against its adjacent colour, then each ratio is at least 3:1.
- AC-3: Given each audited screen, when the test measures the hover state and the selected state of each control that has one against its adjacent colour, then each ratio is at least 3:1.
- AC-4: Given a control in a disabled state, when the test measures it, then its ratio is listed in the ratio table (REQ-A11Y-011 AC-3) and is not asserted.
- AC-5: Given any measurement in AC-1 to AC-3 is under 3:1, when the test runs, then it fails and names the control, the state and the measured ratio.

## REQ-A11Y-007 Text contrast, WCAG 1.4.3 (M)

Evidences CHG-0001 NFR-001 AC-1 and SDD §9 ("text contrast ≥ 4.5:1"). Source: clarifications Q7.

- AC-1: Given each audited screen, when the test measures the text of every control in each of its states (normal, focus, hover, selected), then each ratio against its background is at least 4.5:1, or at least 3:1 when the text is large text as WCAG 2.1 defines it.
- AC-2: Given a control in a disabled state, when the test measures its text, then its ratio is listed in the ratio table and not asserted.
- AC-3: Given any measurement in AC-1 is under its threshold, when the test runs, then it fails and names the control, the state and the measured ratio.
- AC-4: Given the existing axe contrast rule in `web/e2e/a11y.spec.ts`, when the new tests run, then that rule and its tests are unchanged and still pass; the ratio table adds measured values to them.

### Not-found animation

## REQ-A11Y-008 Video of the not-found animation (M)

Evidences CHG-0001 REQ-NAV-001 AC-6 (SDD §14 row: "a screen recording of its full run"). Source: clarifications Q1, Q2, Q3.

- AC-1: Given a signed-in user opening an address the web app does not define, when the not-found test runs in chromium, then Playwright records a video of the page covering the whole run of the animation, and the video file is produced.
- AC-2: Given the same test in webkit, when it runs, then the same video is produced.
- AC-3: Given the other new tests and the existing tests, when they run, then they record no video; video is on only for the not-found test(s) (Q2).
- AC-4: Given the recorded videos, when the evidence is assembled, then each video is a `.webm` file small enough to commit (a few hundred KB each, as the requester stated).

## REQ-A11Y-009 Automated flash check, WCAG 2.3.1 (M)

Evidences CHG-0001 REQ-NAV-001 AC-6 (no more than three flashes in any one second). Source: clarifications Q8, Q3.

- AC-1: Given the not-found page, when the test pauses the page's animations and steps them through their whole run with the Web Animations API at 60 samples per second, then it takes one viewport screenshot at each step.
- AC-2: Given the screenshots, when the test computes per-pixel relative luminance and red saturation, then it counts general flashes and red flashes (a flash is a pair of opposing changes in relative luminance of at least 10% of the maximum, with the darker state below 0.80; the red-flash rule likewise for saturated red, as in WCAG 2.3.1) in every one-second window of the run.
- AC-3: Given the counts, when the largest count in any one-second window is at most 3 for general flashes and at most 3 for red flashes, then the test passes; otherwise it fails and reports the window and the count.
- AC-4: Given the whole viewport, when the flashes are counted, then the small-area exemption of WCAG 2.3.1 is not applied (the stricter reading).
- AC-5: Given the test, when it runs in chromium and in webkit, then it runs and passes in both, with the animation stepped the same way, without pausing real playback.
- AC-6: Given the sampled run, when the test finishes, then it records the number of samples, the run length covered and the highest general and red flash counts, for the summary.

### Run, evidence and open items

## REQ-A11Y-010 What the tests run against (M)

Source: clarifications Q2, Q3, Q4 (audit-only).

- AC-1: Given the new tests, when I look at the repository after this change, then they are in `web/e2e/`, run under the main Playwright configuration (the SDD §11 run: API and Vite dev server), and `pnpm -C web e2e` includes them.
- AC-2: Given the new tests, when they run, then they run in chromium and webkit (the projects in `tests.requiredProjects`) and pass in both; firefox is not required for them.
- AC-3: Given this change's diff, when I review it, then it adds or changes only tests, test support, evidence and documentation: no file of the web app's or the API's product code (outside `web/e2e/` and its support files) is changed.
- AC-4: Given the history of this change, when I read it, then each new test appears first as a failing test commit (`test(...)`) and the passing state follows; no test is weakened, skipped or deleted to get green, and none uses `test.fail()` or `test.skip()` to hide a failure.
- AC-5: Given a new test, when I read its name, then it starts with the requirement ID and AC it proves.

## REQ-A11Y-011 Evidence (M)

Source: clarifications Q1.

- AC-1: Given the runs in chromium and webkit, when the evidence is attached, then the Playwright JUnit XML of each run is attached as kind `test-report` under `.harness/changes/CHG-0004/evidence/` and committed.
- AC-2: Given REQ-A11Y-008, when the evidence is attached, then the chromium and the webkit videos of the not-found animation are attached as kind `other` and committed.
- AC-3: Given the audit, when the evidence is attached, then one Markdown summary (kind `other`) is attached and committed that holds: the contrast ratio table (each control and state, its ratio, its threshold, asserted or not; disabled states marked "measured, not asserted"), the flash-check result (REQ-A11Y-009 AC-6), the announcement and state results (REQ-A11Y-001 to REQ-A11Y-005), and the open screen-reader note of REQ-A11Y-013.
- AC-4: Given the Playwright HTML report, when I list the evidence, then it is not among the attached files (it stays local).

## REQ-A11Y-012 A real WCAG failure (M)

Source: clarifications Q4.

- AC-1: Given a new test fails because develop's application has a real WCAG failure, when this change is at its verification stage, then the failing test stays failing, the summary records the failure (screen, control or state, criterion and measured value), and this change does not merge.
- AC-2: Given such a failure, when it is followed up, then it is fixed by a separate change (test-first, with this test as its red); this change merges only after that fix is on develop and the test passes.
- AC-3: Given such a failure, when I read this change, then it has not fixed the product code itself, and has not marked the test as an expected failure or skipped it (REQ-A11Y-010 AC-3, AC-4).
- AC-4: Given all new tests pass on first run against develop, when the summary is written, then it states that no failure was found.

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

- AC-1: Given two runs of the new tests on the same browser and the same code, when they finish, then the contrast ratios in the table agree to two decimal places (proposed) and the flash counts are equal.
- AC-2: Given the flash check, when it runs, then it steps the animation by the Web Animations API rather than by wall-clock waits, so its result does not depend on the machine's speed.

## NFR-102 Suite impact (S)

Source: clarifications Q2.

- AC-1: Given the new tests are in `pnpm -C web e2e`, when the suite runs, then the existing tests still pass unchanged.
- AC-2: Given the new tests in one browser, when they run, then they add no more than 5 minutes (proposed) to the run on the build host.

## NFR-103 Security and data (M)

Source: CLAUDE.md rules 9 and 10.

- AC-1: Given the new tests, the videos and the summary, when I review them, then they hold no secret or real credential; test accounts use the generated test identifiers and passwords of the existing test support only.

## Out of scope

- Fixing any product code, including a real WCAG failure the tests find (a separate change, REQ-A11Y-012; Q4).
- Firefox for the new tests (Q3).
- The manual screen-reader pass itself, any NVDA or Windows pass, and any other human review (Q5, Q6).
- A production build (`vite build`, `vite preview`) as the target of the tests (Q2).
- Frame extraction from the recorded videos or `ffmpeg` (Q8).
- Putting the Playwright HTML report in evidence (Q1).
- Disabled states as assertions (they are measured and listed only, Q7).
- WCAG criteria beyond those above (the existing axe checks keep covering the rest); a level above WCAG 2.1 AA.
- Changing CHG-0001's frozen spec or its verdicts.

## Proposed (needs confirmation)

- REQ-A11Y-001 AC-4: the session-ended message is announced in a live region of role `status` or `alert` (the answer names the text, not the role).
- REQ-A11Y-002 AC-2, AC-3, AC-6: the exact wording of "first-use empty state" is left to the existing page; the filter group's accessible name is not fixed here.
- REQ-A11Y-006 and REQ-A11Y-007: "every kind of control and state" means each kind that exists on an audited screen; "adjacent colour" is the colour the control sits against.
- REQ-A11Y-008 AC-4: "small enough to commit" is the requester's "a few hundred KB each"; no hard limit is set.
- REQ-A11Y-009: the one-second windows slide one sample at a time; the run covered is the whole animation run.
- NFR-101 AC-1: agreement to two decimal places.
- NFR-102 AC-2: 5 minutes added per browser.

## Open questions

None.
