# Clarifications — CHG-0004

Request (requester @alokkulkarni): add the accessibility evidence that CHG-0001's pack lacks (`docs/specs/CHG-0001/sdd.md`
§14, NFR-001 row and REQ-NAV-001 row). Settled by the requester and not asked again here: every check is automated with
Playwright tests written test-first by web-qa (accessibility-tree and ARIA announcement checks, non-text contrast
measured with a ratio table, video of the not-found animation in chromium and webkit, an automated frame-sampling check
of WCAG 2.3.1); the manual screen-reader pass stays a human task, recorded as an open note, neither waived nor faked;
the plan is greenfield.

## Q1. What goes into this change's evidence, and is it committed to git?

`harness evidence attach` copies a file from the repository into `.harness/changes/CHG-0004/evidence/<kind>/`, so
whatever is attached is committed. (a) The Playwright JUnit XML of each run (`test-report`), the not-found animation
videos for chromium and webkit (`other`, a few hundred KB each as `.webm`) and one Markdown summary (`other`) with the
ratio table, the flash-check result, the announcement results and the open screen-reader note; the Playwright HTML
report stays local (it is a folder of many assets). (b) As (a), plus the Playwright HTML report. (c) The JUnit XML and
the summary only; the videos are kept local and described in the summary. Recommended: (a), because the videos are
the evidence the SDD's REQ-NAV-001 row asks for ("a screen recording of its full run") and stay small, while the HTML
report adds bulk and nothing the JUnit and summary do not.

**Answer:**

## Q2. What exactly do the new tests run against?

The proposal is "develop run locally per SDD §11". (a) New spec files in `web/e2e/`, run by the main Playwright
configuration (`web/playwright.config.ts`: API by `./mvnw spring-boot:run`, SPA by the Vite dev server), so they audit
develop's application code as it is on this change's branch (this change adds tests and evidence only) and stay in
`pnpm -C web e2e` from then on; video is switched on only for the not-found tests (`test.use({ video: 'on' })`). (b) A
separate Playwright configuration run on demand (as `playwright.session-expiry.config.ts` is), so the main suite does
not grow. (c) A production build (`vite build` and `vite preview`) instead of the dev server. Recommended: (a), because
it is the §11 run the SDD names, keeps the checks running on every later change, and needs no new configuration; (c)
departs from §11.

**Answer:**

## Q3. Which browsers do the new tests run in?

`.harness/config.yaml` requires `chromium` and `webkit` (`tests.requiredProjects`); `web/playwright.config.ts` also
lists `firefox`, which did not launch on the build host during CHG-0001 (validator findings F-9 and F-19 in
`.harness/changes/CHG-0001/verdicts/N12-validate.validator.json`). (a) chromium and webkit for every new test, firefox
excluded from them; (b) all three, with firefox installed first; (c) chromium and webkit for the videos and the flash
check, chromium only for the accessibility-tree and contrast checks. Recommended: (a), because it matches the
required projects and the videos you asked for, and avoids the firefox launch problem.

**Answer:**

## Q4. What happens when a new test finds a real WCAG failure on develop?

Proposed: the failure is recorded and fixed in a separate change. CLAUDE.md rule 6 forbids weakening, skipping or
deleting a failing test to get green, and this change's gates and `harness merge` need its tests to pass, so a
recorded failure cannot simply stay red here. (a) This change waits: the failing test and the summary record the
failure, a separate change fixes the code (test-first, with this test as its red), and this change merges only after
that fix is on develop and the test passes. (b) This change takes the fix itself (a React build node makes the failing
test pass), so it is no longer audit-only. (c) The test is marked as an expected failure (`test.fail()`) naming the
follow-up change, so the suite is green while the failure stays visible and the test turns red when the fix lands;
this is close to what rule 6 forbids, so it needs your explicit choice. Recommended: (a), because it keeps your
"separate change" decision without weakening any test.

**Answer:**

## Q5. Who does the manual screen-reader pass, with which screen reader and browser?

The open note will name the person, the screen reader and browser, and the eight screens of SDD §14 (sign-up, login,
list, create/edit form, delete dialog, clear-completed dialog, to-do detail, not-found page). (a) @alokkulkarni, with
VoiceOver and Safari on macOS; (b) @alokkulkarni, with VoiceOver and Safari, and NVDA with Firefox or Chrome on
Windows; (c) someone else (please name them and the reader). Recommended: (a), because VoiceOver and Safari are on the
build host (webkit is a required project); say if you want NVDA as well.

**Answer:**

## Q6. Does the open screen-reader note block anything?

(a) It blocks nothing: this change merges with the note open in its summary, and the pass is recorded later in a
change of its own; (b) it blocks this change's merge until the pass is done and recorded here; (c) it blocks only the
product owner's acceptance of CHG-0001's accessibility, not this change's merge. Recommended: (a), which matches
"leave a note for this to be human", while the summary and the knowledge bundle keep saying the pass is not done.

**Answer:**

## Q7. Which contrast checks are measured and asserted?

axe already checks text contrast (1.4.3) as pass or fail but gives no ratio table, and SDD §14 asks for a contrast pass.
WCAG 1.4.11 and 1.4.3 both exempt inactive (disabled) components. (a) Non-text contrast (1.4.11) only: focus outlines,
control borders (inputs, checkboxes, radios, buttons), and hover and selected states, each measured against its
adjacent colour and asserted at least 3:1; disabled states measured and listed in the table but not asserted.
(b) As (a), plus a text-contrast (1.4.3) ratio table for the text of each control and state, asserted at least 4.5:1
(3:1 for large text); disabled states measured and listed, not asserted. (c) As (b), but disabled states also asserted
at least 3:1. Recommended: (b), because SDD §9 states "text contrast ≥ 4.5:1" and a measured table is the evidence the
manual contrast pass would have given.

**Answer:**

## Q8. How does the automated flash check decide pass or fail?

WCAG 2.3.1: no more than three general flashes and no more than three red flashes in any one-second period; a flash is
a pair of opposing changes in relative luminance of at least 10% of the maximum, where the darker state is below 0.80
(the red-flash rule likewise for saturated red). The animation (`web/src/styles/app.css` L465–L509) changes only
opacity and transform. (a) The test pauses the page's animations and steps them through their whole run with the Web
Animations API at 60 samples per second, screenshots the viewport at each step, computes relative luminance and red
saturation per pixel, counts general and red flashes in every one-second window, and passes only when each count is at
most 3; the small-area exemption is not applied (stricter). (b) The same counting, on frames decoded from the recorded
videos (needs `ffmpeg`, which the project does not use today). (c) Screenshots taken during real playback, without
pausing. Recommended: (a), because stepping is deterministic in both browsers and needs no new tool; (c) cannot hold
60 samples per second reliably.

**Answer:**

## Q9. When is the knowledge bundle's open-gap note updated?

`docs/knowledge/requirements/nfr-001.md`, `req-nav-001.md` and `tests/accessibility-tests.md` say the manual pass and
the flash review are not recorded. Proposed: a later docs follow-up. A greenfield plan has its own docs nodes
(drawio-architect, then okf-documenter) that run before this change's verification, so the PR would ship them.
(a) This change's docs node updates those notes: the automated checks are recorded in CHG-0004, the human
screen-reader pass is still open; (b) a separate docs change after this one merges, as proposed, and this change's
docs node changes nothing in those notes. Recommended: (a), because the node runs anyway and the bundle would
otherwise state a gap this change closes.

**Answer:**

## Q10. Is this the list of announcements and states the accessibility-tree checks must cover?

Playwright cannot hear a screen reader; it can check the accessibility tree: each element's role, accessible name and
state, the live region (`role="alert"`, `role="status"` or `aria-live`) a message appears in, and where focus lands.
From SDD §4.1, §6.1, §8 and §9 and the states `web/e2e/a11y.spec.ts` opens, the list is:

1. Sign-up and login: the refused-submit error summary (`role="alert"`) takes focus and links to each field; each field
   error is set with `aria-invalid="true"` and named by `aria-describedby` (NFR-001 AC-3).
2. Login: "Account created — log in" in a status region; "Your session has ended. Please log in again."; the refused
   login alert "Invalid identifier or password. Check them and try again."
3. Create and edit form: the error summary and field errors as in 1; the page `<h1>` "Add a to-do" or "Edit to-do"
   takes focus on arrival.
4. List: the polite live region's result text ("Showing 1–20 of 45"); the first-use empty state and "No matching
   to-dos"; the filter as a named group of radios; the pagination landmark named "Pagination" with
   `aria-current="page"`; focus on the `<h1>` "My to-dos" after a pagination link, and on the used control after a
   filter or search change.
5. List: the polite announcement "There are no completed to-dos to clear." (REQ-TODO-007 AC-4) and the alert "That
   to-do no longer exists, and the list has been refreshed."
6. Delete and clear-completed dialogs: role `dialog` named by its heading, focus on Cancel when opened, focus returned
   to the invoking control (or the list heading) when closed.
7. To-do detail page and its to-do not-found state: the `<h1>` takes focus and `document.title` is set.
8. Not-found page: the `<h1>`, sentence and link are in the tree at first render and the animated element is hidden
   (`aria-hidden="true"`).
9. The start-up alert "The app could not reach the server." with its Retry button (§4.1).

(a) This list as written; (b) this list with changes (please say which); (c) only the states `a11y.spec.ts` opens
today (items 1–4, 6–8). Recommended: (a), because it covers the states and the announcements NFR-001 relies on.

**Answer:**
