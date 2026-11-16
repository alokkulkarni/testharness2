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

**Answer:** (a) JUnit XML + chromium/webkit videos + one Markdown summary, committed; HTML report stays local. (@alokkulkarni: all as suggested, 2026-11-13)

## Q2. What exactly do the new tests run against?

The proposal is "develop run locally per SDD §11". (a) New spec files in `web/e2e/`, run by the main Playwright
configuration (`web/playwright.config.ts`: API by `./mvnw spring-boot:run`, SPA by the Vite dev server), so they audit
develop's application code as it is on this change's branch (this change adds tests and evidence only) and stay in
`pnpm -C web e2e` from then on; video is switched on only for the not-found tests (`test.use({ video: 'on' })`). (b) A
separate Playwright configuration run on demand (as `playwright.session-expiry.config.ts` is), so the main suite does
not grow. (c) A production build (`vite build` and `vite preview`) instead of the dev server. Recommended: (a), because
it is the §11 run the SDD names, keeps the checks running on every later change, and needs no new configuration; (c)
departs from §11.

**Answer:** (a) new specs in web/e2e/ under the main config (SDD §11 run), part of `pnpm -C web e2e`; video only on the not-found tests. (@alokkulkarni: all as suggested, 2026-11-13)

## Q3. Which browsers do the new tests run in?

`.harness/config.yaml` requires `chromium` and `webkit` (`tests.requiredProjects`); `web/playwright.config.ts` also
lists `firefox`, which did not launch on the build host during CHG-0001 (validator findings F-9 and F-19 in
`.harness/changes/CHG-0001/verdicts/N12-validate.validator.json`). (a) chromium and webkit for every new test, firefox
excluded from them; (b) all three, with firefox installed first; (c) chromium and webkit for the videos and the flash
check, chromium only for the accessibility-tree and contrast checks. Recommended: (a), because it matches the
required projects and the videos you asked for, and avoids the firefox launch problem.

**Answer:** (a) chromium + webkit (tests.requiredProjects). (@alokkulkarni: all as suggested, 2026-11-13)

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

**Answer:** (a) a real WCAG failure: this change waits for a separate fix change to merge. (@alokkulkarni: all as suggested, 2026-11-13)

## Q5. Who does the manual screen-reader pass, with which screen reader and browser?

The open note will name the person, the screen reader and browser, and the eight screens of SDD §14 (sign-up, login,
list, create/edit form, delete dialog, clear-completed dialog, to-do detail, not-found page). (a) @alokkulkarni, with
VoiceOver and Safari on macOS; (b) @alokkulkarni, with VoiceOver and Safari, and NVDA with Firefox or Chrome on
Windows; (c) someone else (please name them and the reader). Recommended: (a), because VoiceOver and Safari are on the
build host (webkit is a required project); say if you want NVDA as well.

**Answer:** (a) @alokkulkarni, VoiceOver + Safari, as an open human note. (@alokkulkarni: all as suggested, 2026-11-13)

## Q6. Does the open screen-reader note block anything?

(a) It blocks nothing: this change merges with the note open in its summary, and the pass is recorded later in a
change of its own; (b) it blocks this change's merge until the pass is done and recorded here; (c) it blocks only the
product owner's acceptance of CHG-0001's accessibility, not this change's merge. Recommended: (a), which matches
"leave a note for this to be human", while the summary and the knowledge bundle keep saying the pass is not done.

**Answer:** (a) the open screen-reader note blocks nothing; it gets its own change later. (@alokkulkarni: all as suggested, 2026-11-13)

## Q7. Which contrast checks are measured and asserted?

axe already checks text contrast (1.4.3) as pass or fail but gives no ratio table, and SDD §14 asks for a contrast pass.
WCAG 1.4.11 and 1.4.3 both exempt inactive (disabled) components. (a) Non-text contrast (1.4.11) only: focus outlines,
control borders (inputs, checkboxes, radios, buttons), and hover and selected states, each measured against its
adjacent colour and asserted at least 3:1; disabled states measured and listed in the table but not asserted.
(b) As (a), plus a text-contrast (1.4.3) ratio table for the text of each control and state, asserted at least 4.5:1
(3:1 for large text); disabled states measured and listed, not asserted. (c) As (b), but disabled states also asserted
at least 3:1. Recommended: (b), because SDD §9 states "text contrast ≥ 4.5:1" and a measured table is the evidence the
manual contrast pass would have given.

**Answer:** (b) 1.4.11 non-text ≥3:1 plus a 1.4.3 text-contrast table asserted ≥4.5:1 (3:1 large); disabled states measured, not asserted. (@alokkulkarni: all as suggested, 2026-11-13)

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

**Answer:** (a) Web Animations API stepping at 60 samples/s, screenshot per step, count general and red flashes per WCAG 2.3.1 in every 1-second window, ≤3 passes, no small-area exemption. (@alokkulkarni: all as suggested, 2026-11-13)

## Q9. When is the knowledge bundle's open-gap note updated?

`docs/knowledge/requirements/nfr-001.md`, `req-nav-001.md` and `tests/accessibility-tests.md` say the manual pass and
the flash review are not recorded. Proposed: a later docs follow-up. A greenfield plan has its own docs nodes
(drawio-architect, then okf-documenter) that run before this change's verification, so the PR would ship them.
(a) This change's docs node updates those notes: the automated checks are recorded in CHG-0004, the human
screen-reader pass is still open; (b) a separate docs change after this one merges, as proposed, and this change's
docs node changes nothing in those notes. Recommended: (a), because the node runs anyway and the bundle would
otherwise state a gap this change closes.

**Answer:** (a) this change's own docs node updates the knowledge-bundle open-gap notes. (@alokkulkarni: all as suggested, 2026-11-13)

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

**Answer:** (a) the 9-item announcement/state list as written. (@alokkulkarni: all as suggested, 2026-11-13)

## Q11. How should test-first work for tests of behaviour that already exists?

Raised in phase 1 by the critic (finding F-21 of `.harness/changes/CHG-0004/verdicts/history/N02-requirements.critic-verdict.attempt-2.json`). You said the tests are written test-first by web-qa and that no red commit may be faked. These audit tests check behaviour develop already has, so they may pass on their first run. In CHG-0001 the web-qa tests node carried the `tests-red-first` gate, which web-qa attests from a run that failed (there: the app modules did not exist yet, `.harness/changes/CHG-0001/verdicts/N08-tests-web.tests-red-first.json`). How the harness treats an honest all-green first run is only known once the plan is extended after the freeze. (a) Keep the requirement as drafted (REQ-A11Y-010 AC-4): tests are committed before any other non-documentation commit, each first-run result is recorded honestly, no failing state is made up; if the harness cannot complete the tests node honestly, the change stops at planning and I report exactly what the CLI does for your decision. (b) A genuine red from missing test support: the specs are committed first, calling measuring helpers (contrast, flash counting) that do not exist yet, so their first run fails for that reason; a later node adds the helpers and the run turns green. The red is real but proves only that the helpers were missing, not anything about WCAG. (c) No test-first order for this change: tests and helpers are committed together, and the `tests-red-first` mismatch is reported to the harness maintainers. Recommended: (a), because it follows your "never fake a red commit" and leaves the gate decision to the point where the CLI behaviour is known.

**Answer:** (a) Keep REQ-A11Y-010 AC-4: tests committed first, each first-run result recorded honestly, no made-up red. If the harness can't complete the tests node honestly, stop at planning and report what the CLI does. (@alokkulkarni: '1 -a', 2026-11-13)

## Q12. REQ-A11Y-002 AC-4 says the current page's link has `aria-current="page"`, but the app marks it on a `<span>`: how should the requirement read?

Raised in phase 2 by the spec author (`docs/specs/CHG-0004/sdd.md` §14 item 1). The app marks the current page with `<span aria-current="page">`, not a link (`web/src/todos/Pagination.tsx` L49–L53); CHG-0001 SDD §9 and Q10 item 4 say only `aria-current="page"`. It is not one of the listed wording differences (analyst defaults 1, 2, 3, 6), so REQ-A11Y-012 AC-5 makes it a question. The test is the same under (a) and (b). (a) Reword REQ-A11Y-002 AC-4 to "the element for the current page has `aria-current="page"`". (b) Keep the wording and add it as a listed spec-wording difference (a new analyst default), recorded in the summary. Recommended: (a), because the requirement then says what is checked and no difference needs recording. Either way `requirements.md` changes after its review passed, so N02-requirements has to be reopened, which only a person can do: `harness plan reopen N02-requirements --reason "Q12: REQ-A11Y-002 AC-4 wording"` in your own terminal on this branch.

**Answer:** (a) Reword REQ-A11Y-002 AC-4 to "the element for the current page". (@alokkulkarni: "1 - a", 2026-11-13)

## Q13. Are native checkboxes and radios asserted at 3:1 (REQ-A11Y-006 AC-2, AC-3, AC-5)?

Raised in phase 2 by the spec author (`docs/specs/CHG-0004/sdd.md` §13 R1, §14 item 2). The filter radios and the done checkboxes are painted by the browser. WCAG 1.4.11 exempts appearance "determined by the user agent and not modified by the author". (a) Assert them at 3:1 as REQ-A11Y-006 says (the SDD's default); if a browser paints one under 3:1 the test fails, web-qa stops and reports, and this change waits for a CSS fix in a separate change (Q4). (b) Measure them and list them in the ratio table as "browser appearance, not asserted", like disabled states. Recommended: (a), because it is what REQ-A11Y-006 says and needs no requirement change; (b) would change `requirements.md` (see Q12 on reopening).

**Answer:** (a) Assert native checkboxes and radios at 3:1 per REQ-A11Y-006; if one fails, the change waits for a separate CSS fix (Q4). (@alokkulkarni: "2 - a", 2026-11-13)

## Q14. Is the pagination current-page marker's fill left out of the non-text contrast assertions?

Raised in phase 2 by the spec author (`docs/specs/CHG-0004/sdd.md` §6.4, §14 item 3). The marker's fill (`#EFF6FF`) against the page (`#F9FAFB`) is about 1.04:1. The SDD treats the marker as not a control, so its fill is not a "selected state" under REQ-A11Y-006 AC-3; its bold, underlined text is measured and asserted in the text-contrast table (REQ-A11Y-007). (a) Yes, as the SDD says. (b) No: assert the fill at 3:1, which fails on develop today and makes this change wait for a separate fix (Q4). Recommended: (a), because the current page is shown by text weight, underline and `aria-current`, not by the fill alone, so the fill is not what identifies it.

**Answer:** (a) Leave the pagination current-page marker's fill out of the non-text assertions; its text is asserted. (@alokkulkarni: "3 - a", 2026-11-13)
