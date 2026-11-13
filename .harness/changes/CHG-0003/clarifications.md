# Clarifications — CHG-0003

Request (@alokkulkarni, follow-up to the merged change CHG-0001 "to-do list"): record the accessibility evidence that
CHG-0001's SDD §14 requires but that is not in its evidence pack: (1) the manual screen-reader and contrast pass of the
to-do web app, and (2) the flash review for REQ-NAV-001 AC-6.

Sources read:

- `docs/specs/CHG-0001/sdd.md` L793 (a11y level includes a "manual screen-reader pass"), L814 (REQ-NAV-001 row: "review
  (web-qa, recorded with the NFR-001 audit): the animation's keyframes and a screen recording of its full run show no
  flash"), L818 (NFR-001 row: "manual screen-reader and contrast pass on the same eight screens recorded by web-qa";
  2.2.1 recorded as met by the Q36 exception), L759 (risk: axe alone cannot prove WCAG 2.1 AA).
- `docs/specs/CHG-0001/requirements.md` L223 (REQ-NAV-001 AC-6), L254-L262 (NFR-001).
- `docs/knowledge/tests/accessibility-tests.md` L21-L23 and `docs/knowledge/requirements/nfr-001.md` L27-L31 (the
  open-gap notes); PR #4's last follow-up.
- `web/e2e/a11y.spec.ts` L5-L105 (axe, WCAG 2.1 A/AA tags incl. colour contrast, on the eight screens in chromium and
  webkit) and L148-L154 (reduced motion); `web/src/styles/app.css` L465-L509 (the not-found animation: one fade-in from
  opacity 0 to 1 with a 16 px rise, and one strike line scaling from 0.2 to 1, each once).
- `docs/specs/CHG-0001/conformance.md` L135 and L149-L152: REQ-NAV-001 AC-6 and NFR-001 are `met` on automated tests
  only; the manual checks are not mentioned there.

Already settled by the frozen CHG-0001 spec, so not asked: the eight screens (sign-up, log in, list, create/edit form,
delete dialog, clear-completed dialog, to-do detail, not-found page while signed in); the level (WCAG 2.1 AA, Q44);
2.2.1 Timing Adjustable recorded as met under the "essential" exception (Q36); the flash review's inputs (the keyframes
and a screen recording of the full run); and that web-qa records the result.

This change is planned with the `smoke` template: no product code, tests or build nodes, only requirements, spec,
freeze, verify and PR. Some answers below (Q9 (b), Q10 (b)) would not fit that plan; they are flagged.

## Q1. Who performs the screen-reader pass, and who writes the record?

A screen-reader pass means listening to what VoiceOver or NVDA announces while operating each screen. No agent can run
a screen reader or hear its output; an agent can only read the browser's accessibility tree (for example a Playwright
ARIA snapshot), which is not a screen-reader pass.
(a) You, or a person you name, operate the screen reader on the eight screens following a written script that web-qa
prepares (steps and the expected announcement per step); you note what was announced and any problem; web-qa turns
your notes into the record, and you confirm the record.
(b) A person does the whole pass and writes the record; agents only attach it as evidence and review it.
(c) web-qa records an accessibility-tree snapshot per screen instead, and the manual screen-reader pass is waived (this
does not meet SDD §14 as written).
Recommended: (a), because it keeps the listening with a person, as it must, and the record in the shape SDD §14 asks of
web-qa. Please also say who the person is (name and GitHub login), so the record can name them.

**Answer:**

## Q2. Which screen reader and browser pairs should the pass use?

The work machine is macOS. The required browser projects are chromium and webkit (`.harness/config.yaml` L56-L57).
(a) VoiceOver with Safari only (the pair Apple supports; it matches the webkit project).
(b) VoiceOver with Safari, and VoiceOver with Chrome (both required engines, one machine).
(c) (a) or (b) plus NVDA with Firefox or Chrome on Windows (the most used desktop screen reader; needs a Windows machine
or VM).
Recommended: (a) as the minimum, (b) if you have the time. The record states the macOS, browser and screen-reader
versions either way.

**Answer:**

## Q3. Which states of each screen does the screen-reader pass cover?

SDD §14 names eight screens but not their states.
(a) Each screen in the states the axe test already opens (`web/e2e/a11y.spec.ts`: forms empty and after a refused
submit, the list empty, paged and with no matches, both dialogs open, the detail page and its to-do not-found state, the
not-found page), plus the announcements NFR-001 AC-2 and AC-3 rely on: the error summary and field errors, the polite
live region ("Showing 1–20 of 45"), focus moving into and out of each dialog, the page title and `<h1>` on each route
change, and the "Account created" and "Your session has ended" messages on log in.
(b) Only each screen's first state as it loads.
(c) A list you give.
Recommended: (a), because those states are where screen-reader problems usually appear and the axe test already
defines them, so the two checks line up.

**Answer:**

## Q4. What does the manual contrast pass check, and with which tool?

The axe test already checks text contrast (WCAG 1.4.3) in real chromium and webkit on the eight screens
(`web/e2e/a11y.spec.ts` L5-L11). It does not check non-text contrast (1.4.11) or states it never renders.
(a) Measure what axe does not: focus outlines, input, checkbox and radio borders, the selected filter option, disabled
controls, hover states, error borders, the pagination current-page marker and the not-found illustration, plus text on
hover and focus. Use a colour-picker contrast tool (for example TPGi Colour Contrast Analyser, free on macOS), and
record each element with its two colours, measured ratio, required ratio and result.
(b) (a) plus measuring all text contrast by hand again.
(c) Rely on axe for contrast and record only a statement that the manual check found nothing more.
Recommended: (a), because it covers the gap axe leaves without repeating it. Who does the measuring follows Q1 unless
you say otherwise; the colour values can also be read from `web/src/styles/tokens.css`.

**Answer:**

## Q5. Who captures the screen recording of the not-found animation for the flash review, and how?

SDD §14 asks for a recording of the full run. The animation runs once for about 1.2 s and changes only opacity and
position (`web/src/styles/app.css` L465-L509).
(a) web-qa records it with Playwright's video recording on the signed-in `/nope` page in chromium and webkit (repeatable,
and the commit and browser versions are known).
(b) You record it yourself with macOS screen recording (Cmd+Shift+5) in Safari and Chrome.
(c) Both.
Recommended: (a), because it is repeatable and an agent can do it; a person still watches the clip as part of Q6.

**Answer:**

## Q6. How is "no more than three flashes in any one second" (WCAG 2.3.1) judged?

(a) Review: web-qa writes up the keyframes (what changes, how often, how much) and the recording frame by frame, and
you watch the clip and confirm. The keyframes have one opacity rise and no repeated light-dark change, so a review can
show there is at most one transition.
(b) (a) plus a measured check: a script computes the relative luminance change between frames of the recording against
the WCAG general-flash and red-flash thresholds and attaches its output.
(c) A dedicated flash-analysis tool such as PEAT (Windows only).
Recommended: (a). (b) is extra cost for an animation with a single fade-in, but choose it if you want a measurement on
record.

**Answer:**

## Q7. What form does the recorded evidence take, and where does it live?

CHG-0001 is merged and its evidence pack is closed. `harness evidence attach` takes `--change`, but adding files to a
merged change's evidence pack changes that change's record after its merge.
(a) In this change's evidence (CHG-0003): one audit record (Markdown) listing, per screen and state, the screen
reader and browser, the steps, what was announced, each WCAG success criterion checked with its result, the contrast
table of Q4, the flash review of Q6, the audited commit (Q8), who did what and when; plus the video clip(s) committed
next to it. The record names the CHG-0001 requirement IDs it covers (NFR-001 AC-1..AC-3, REQ-NAV-001 AC-6).
(b) As (a), but attached to CHG-0001's evidence pack with `--change CHG-0001`.
(c) As (a), but the video clips are kept outside git, and the record carries their file hash and where they are
stored.
Recommended: (a), because CHG-0001's record stays as merged and the evidence is reviewed and approved in its own PR.
The clips are about a second long, so committing them costs little.

**Answer:**

## Q8. Which build is audited, and how is it run?

(a) `develop` at the commit this change starts from (`ca9b431`, which holds CHG-0001's code), run locally as SDD §11
says (API on :8080, Vite dev server on :5173), with that commit recorded in the audit.
(b) A production build of the SPA (`vite build`, then a static preview) against the same API, because that is closer to
what a user would get.
(c) Another commit or setup you name.
Recommended: (a), because the first release is local only (NFR-002) and that is the setup CHG-0001's tests ran on.

**Answer:**

## Q9. What happens if the pass finds a WCAG 2.1 AA failure or a flash?

(a) Record it in the audit as a failure, with its location and the success criterion, and fix it in a separate
follow-up change. This change stays evidence only.
(b) Fix it in this change. The `smoke` plan has no tests or build nodes, so this would need a different plan, or a new
change.
(c) Decide case by case once the findings are known.
Recommended: (a), because it keeps this change to what was asked and lets any fix go test first in its own change.

**Answer:**

## Q10. Should the open-gap notes in the OKF bundle be updated once the evidence is recorded?

`docs/knowledge/tests/accessibility-tests.md` L21-L23 and `docs/knowledge/requirements/nfr-001.md` L27-L31 say the
manual pass and flash review are "not recorded (open gap)". Once this change records them, those notes are out of
date.
(a) Yes, in a later docs follow-up after this change merges.
(b) Yes, in this change. The `smoke` plan has no docs (okf-documenter) node, so this would need a different plan.
(c) No.
Recommended: (a), because this plan cannot own `docs/knowledge/`. If you want the notes fixed in the same PR, choose (b)
and I will report what the plan needs before going on.

**Answer:**
