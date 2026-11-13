---
stage: sdd
changeId: CHG-0004
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0004
    agent: spec-author
    agentRunId: run-chg4-n03-1
upstream:
  - path: docs/specs/CHG-0004/requirements.md
    sha256: c67cab9d65db8c4d5eed647062b13def283addb1bd930d1aace77c8979dc5f43
---

# SDD — CHG-0004 Automated accessibility audit of CHG-0001

## 1. Context and goals

CHG-0001's SDD §14 (NFR-001 and REQ-NAV-001 rows) asks for accessibility evidence its pack lacks. This change adds it as
automated Playwright tests in `web/e2e/`, run under the main configuration against develop's web app, and attaches the
results as evidence (`docs/specs/CHG-0004/requirements.md`). It is audit-only: no product code changes (REQ-A11Y-010
AC-3). Goals, each tied to its requirements:

- Accessibility-tree, announcement and focus checks for the nine items of Q10: REQ-A11Y-001 to REQ-A11Y-005.
- Measured contrast, asserted and tabulated: non-text 3:1 (REQ-A11Y-006) and text 4.5:1 or 3:1 for large text
  (REQ-A11Y-007).
- A video of the not-found animation in chromium and webkit (REQ-A11Y-008) and an automated WCAG 2.3.1 flash count
  (REQ-A11Y-009).
- The run, the test-first order and honest first-run records (REQ-A11Y-010, REQ-A11Y-012), the evidence and the summary
  (REQ-A11Y-011), the open screen-reader note (REQ-A11Y-013) and the knowledge-bundle notes (REQ-A11Y-014).
- Reproducible measurements, a bounded suite time, and no secrets (NFR-101, NFR-102, NFR-103).

Audit level: WCAG 2.1 AA (CHG-0001 Q44). Out of scope: as listed in the requirements.

## 2. Scope, module layout and architecture conformance

**Module layout** (one stack; tests and test support only, no build of product code):

- react: web/ (web/package.json)

**Files this change adds or changes:**

- New spec files and support modules under `web/e2e/` (§3).
- `web/playwright.config.ts`: the firefox project only (§4).
- `web/e2e/a11y-audit/CHG-0004-summary.md`: the summary's source (§10). It lives in `web/`, the one module of this
  change, so the same web-qa nodes that run the tests write it.
- The three knowledge-bundle notes of §11.
- Evidence under `.harness/changes/CHG-0004/evidence/`, attached by `harness evidence attach`.

**Files left unchanged:** `web/src/**`, `api/**`, `web/package.json` and the lockfile (no new dependency), and
`web/e2e/a11y.spec.ts`, `keyboard.spec.ts` and every other existing spec (REQ-A11Y-007 AC-4, NFR-102 AC-1).

**Architecture conformance.** `docs/architecture/solution.md` is a placeholder. It says this change adds or changes no
container, integration, store, deployment, security boundary or observability element. This design adds none: the
tests drive the existing API and Vite dev server that the main configuration already starts (CHG-0001 SDD §11).

**No contract, no ADR.** The change adds no interface, so `contracts/` is empty. Every decision below is local to test
code and can be undone by editing the tests; none adds a dependency or changes a deployable. So no decision is hard to
reverse and `adr/` is empty.

## 3. Test files and support modules

All new spec files are in `web/e2e/` with the prefix `a11y-audit-`, so a single glob identifies them (§4). Test names
start with the requirement ID and the ACs they prove (REQ-A11Y-010 AC-6).

| File | Responsibility | Requirements |
|---|---|---|
| `web/e2e/a11y-audit-announcements.spec.ts` | Accessibility-tree, live-region and focus checks | REQ-A11Y-001 to REQ-A11Y-005 |
| `web/e2e/a11y-audit-contrast.spec.ts` | Per-screen non-text and text contrast measurement, ratio-table fragments, the measurement calibration test | REQ-A11Y-006, REQ-A11Y-007, NFR-101 |
| `web/e2e/a11y-audit-not-found-video.spec.ts` | The video test; the only file that switches video on | REQ-A11Y-008 |
| `web/e2e/a11y-audit-flash.spec.ts` | Stepped sampling of the not-found animation, flash counting, the counter calibration test | REQ-A11Y-009, NFR-101 |
| `web/e2e/support/a11y-audit/colour.ts` | Pure colour arithmetic: parse, composite, relative luminance, contrast ratio, large-text rule, ratio formatting | REQ-A11Y-006, REQ-A11Y-007, REQ-A11Y-009 |
| `web/e2e/support/a11y-audit/analysis-page.ts` | A blank helper page in its own browser context. It decodes PNG screenshots with canvas and runs per-pixel work there, returning only small results to Node | REQ-A11Y-006, REQ-A11Y-009 (F-24) |
| `web/e2e/support/a11y-audit/contrast.ts` | Puts a control into a state; reads its colours, its adjacent colour and its painted boundary; returns measurements and failure messages | REQ-A11Y-006, REQ-A11Y-007 |
| `web/e2e/support/a11y-audit/flash.ts` | Pauses and steps the page's animations, takes the screenshots and feeds the analysis page | REQ-A11Y-009 |
| `web/e2e/support/a11y-audit/focus.ts` | Gives a control keyboard focus so that `:focus-visible` applies | REQ-A11Y-006 AC-1, REQ-A11Y-007 AC-1 |
| `web/e2e/support/a11y-audit/results.ts` | Writes result fragments (ratio tables, flash figures) for the summary | REQ-A11Y-011 AC-3 |

Existing support is reused unchanged: `newAccount`, `signedInUser`, `apiSignUp`, `apiCreateTodo`, `apiMarkDone`,
`apiSeedMany`, `pressTab`, `listHeading`, `rows` and `rowFor` from `web/e2e/support/app.ts`.

Support signatures (bodies are the coder's):

```ts
// colour.ts — channels 0–255, alpha 0–1
export interface Rgba { r: number; g: number; b: number; a: number }
export function parseCssColour(value: string): Rgba;          // rgb()/rgba() only; anything else throws
export function compositeOver(top: Rgba, bottom: Rgba): Rgba;   // source-over
export function relativeLuminance(colour: Rgba): number;        // WCAG 2.1 definition, opaque input
export function contrastRatio(a: Rgba, b: Rgba): number;        // (L1 + 0.05) / (L2 + 0.05), unrounded
export function formatRatio(ratio: number): string;             // truncated to two decimals, e.g. "4.54"
export function toHex(colour: Rgba): string;                    // "#RRGGBB" of an opaque colour
export function isLargeText(fontSizePx: number, fontWeight: number): boolean;

// analysis-page.ts
export interface AnalysisPage {
  maxContrastPixel(png: Buffer, region: PixelRegion, adjacent: Rgba): Promise<{ colour: Rgba; ratio: number }>;
  flashBegin(width: number, height: number): Promise<void>;
  flashAddFrame(png: Buffer, sample: number): Promise<void>;
  flashResult(samplesPerWindow: number): Promise<FlashCounts>;
  close(): Promise<void>;
}
export type PixelRegion = 'whole' | { bandDevicePx: number };
export interface FlashCounts {
  general: { max: number; windowStart: number };
  red: { max: number; windowStart: number };
  pixelsWithGeneralTransition: number;   // non-vacuity: frames really changed
}
export function openAnalysisPage(browser: Browser): Promise<AnalysisPage>;

// contrast.ts
export type ContrastState = 'normal' | 'focus' | 'hover' | 'selected' | 'invalid' | 'disabled';
export type Measure = 'focus outline' | 'boundary' | 'text';
export interface ControlUnderTest {
  screen: string; control: string; locator: Locator;
  native: boolean;              // UA-painted checkbox or radio
  boundary: boolean;            // false for text links (identified by their text)
  states: ContrastState[];
}
export interface Measurement {
  screen: string; control: string; state: ContrastState; measure: Measure;
  foreground: string; adjacent: string; ratio: number; threshold: number;
  asserted: boolean; textSize?: 'normal' | 'large';
}
export function measureNonText(page: Page, analysis: AnalysisPage, control: ControlUnderTest): Promise<Measurement[]>;
export function measureText(page: Page, control: ControlUnderTest): Promise<Measurement[]>;
export function failuresOf(rows: Measurement[]): string[];

// flash.ts
export interface FlashRun extends FlashCounts { samples: number; endTimeMs: number; runLengthMs: number }
export function sampleAnimations(page: Page, analysis: AnalysisPage, samplesPerSecond: number): Promise<FlashRun>;

// focus.ts
export function focusByKeyboard(page: Page, target: Locator): Promise<void>;

// results.ts
export function writeFragment(testInfo: TestInfo, name: string, markdown: string): Promise<string>;
export function ratioTable(rows: Measurement[], browserVersion: string): string;
export function flashFragment(run: FlashRun, browserVersion: string): string;
```

## 4. Configuration: firefox excluded, video only for REQ-A11Y-008

Analyst default 10 allows the configuration to change only for these two purposes.

**Firefox (REQ-A11Y-010 AC-2, Q3 a).** In `web/playwright.config.ts`, the `firefox` project gets its own `testIgnore`:
`['**/session-expiry.spec.ts', '**/restart.spec.ts', '**/a11y-audit-*.spec.ts']`.

- The design takes a project-level `testIgnore` to replace the top-level one rather than merge with it. Playwright's
  documentation of the project option says only "Use testConfig.testIgnore to change this option for all projects"
  (`playwright/types/test.d.ts` L726-L735).
- The firefox entry therefore repeats the two existing patterns. That is correct whether the two values replace or
  merge. If they replace and the patterns were missing, firefox would start running the restart and session-expiry
  specs.
- Nothing else in the file changes. The chromium and webkit projects keep the top-level `testIgnore`, so they run the
  new files.
- The restart and session-expiry configurations match only their own spec file
  (`web/e2e/playwright.restart.config.ts` L12, `playwright.session-expiry.config.ts` L11), so they never pick up the
  new files.
- No `test.skip()` is used for the browser choice (REQ-A11Y-010 AC-5).

**Video (REQ-A11Y-008 AC-3, Q2).**

- `video` is a worker-scoped option (`PlaywrightWorkerOptions`, `playwright/types/test.d.ts` L7348-L7631). The design
  takes Playwright to accept a worker option only at the top level of a spec file or in the configuration, because one
  inside a describe group would need a new worker. A dedicated file is correct either way.
- `web/e2e/a11y-audit-not-found-video.spec.ts` sets it at its top level, with
  `test.use({ video: { mode: 'on', size: { width: 800, height: 450 } } })`. That file holds only the REQ-A11Y-008
  test.
- The configuration's `use` keeps no `video` key, so video stays off (Playwright's default) for every other test. That
  includes the REQ-A11Y-004 and REQ-A11Y-009 tests that open the not-found page.
- This needs no configuration change.

## 5. Accessibility-tree checks (REQ-A11Y-001 to REQ-A11Y-005)

These checks read roles, accessible names and descriptions (`toHaveAccessibleName`, `toHaveAccessibleDescription`),
attributes, live-region membership and focus (`toBeFocused`).

**Rules shared by these checks:**

- **"Holds the text"** is checked as `toContainText` on the element found by role.
- **A polite live region** is an element matched by `[role="status"], [aria-live="polite"]` whose `aria-live`, if
  present, is `polite` (analyst default 3).
- **Session set-up.** Each test signs in through the API with a fresh generated account (`signedInUser`), except where
  the sign-up or log-in UI is under test.
- **Opening a control by keyboard.** WebKit on macOS does not focus a button or radio on a mouse click. A check that
  depends on the focused or opening control therefore uses the keyboard: `locator.focus()` then `Enter`, Tab via
  `pressTab`, and arrow keys for radios.

| AC | Set-up and action | Assertion |
|---|---|---|
| 001 AC-1, AC-2 (sign-up; log-in) | Open `/signup` (and, in a second test, `/login`) signed out; submit the empty form with the submit button | The `role="alert"` element holding "There is a problem" is focused. It has one link per field in error, and each link's `href` is `#<id>` of a field. That field has `aria-invalid="true"` and an accessible description containing the link's text, the field error (`web/src/app/ui.tsx` L95-L113) |
| 001 AC-3 | Sign up through the UI with a generated account | On `/login`, a `role="status"` element holds "Account created. Log in." (analyst default 1) |
| 001 AC-4 (F-19) | Sign in through the API, open `/todos` and wait for the list heading. End the session server-side with the `logOut` operation (`POST /api/v1/auth/logout`) through `page.request`, which shares the page's cookies. Then, without reloading, choose the "Active" radio by keyboard, which makes the SPA send a list request | The URL is `/login?returnTo=…`; an element of role `status` or `alert` holds "Your session has ended. Please log in again." (analyst default 4) |
| 001 AC-5 | On `/login`, a generated account's identifier with a wrong password | A `role="alert"` element holds "Invalid identifier or password. Check them and try again." |
| 001 AC-6 | `/todos/new` submitted empty; and the edit route of an API-created to-do with its title cleared, then submitted | The AC-1/AC-2 assertions, for every field in error (title, and due date on the empty create form) |
| 001 AC-7 | Open `/todos/new` directly; open `/todos/<id>/edit` directly | The `<h1>` "Add a to-do" (then "Edit to-do") is focused |
| 002 AC-1 | 45 to-dos seeded by API; open `/todos` | A polite live region holds "Showing 1-20 of 45" (hyphen-minus, analyst default 2) |
| 002 AC-2 | New account, open `/todos`; then search "no-such-text" | A level-2 heading "You have no to-dos yet"; then a level-2 heading "No matching to-dos" (analyst default 5) |
| 002 AC-3 | 1 to-do; open `/todos` | A `group` role with a non-empty accessible name contains radios, and exactly one radio in it is checked |
| 002 AC-4 | 45 to-dos; open `/todos` | A `navigation` named "Pagination" exists. Inside it, exactly one element has `aria-current="page"`, and that element holds "1". The element for the current page is checked whatever its role (Q12 a): the app's is a `<span>`, not a link (`web/src/todos/Pagination.tsx` L49-L53) |
| 002 AC-5 | 45 to-dos; open `/todos`; activate the "Page 2" link | URL has `page=2`; the `<h1>` "My to-dos" is focused |
| 002 AC-6 | 3 to-dos; open `/todos`. Filter: focus the checked radio "All" with Tab and press ArrowRight. Search: type a first text in the search box and press Enter. Then replace it with a different text, so that a new query runs, focus the Search button and press Enter. After each, wait for the URL's `q=` to change and the result to render | After each re-query has rendered, focus is on "Active", on the search box and on the Search button respectively |
| 002 AC-7 | One active and one done to-do; open `/todos` (Clear all completed enabled). Delete the completed ones with the `clearCompletedTodos` operation (`DELETE /api/v1/todos/completed`) through `page.request`, standing in for another tab. Then activate "Clear all completed" by keyboard | The shell region (`[aria-live="polite"]`, `web/src/app/AppShell.tsx` L69) holds "There are no completed to-dos to clear.", checked before the shell clears it after 10 s (L27). The count of `dialog` role elements is 0 |
| 002 AC-8 | 2 to-dos; open `/todos`; delete one by API; toggle that row's checkbox by keyboard | A `role="alert"` element holds "That to-do no longer exists, and the list has been refreshed." |
| 003 AC-1 to AC-3 (delete dialog; clear-completed dialog) | Open each dialog from the list by keyboard (focus its opener, press Enter). Close once with Cancel, once with Escape | Role `dialog` whose accessible name equals its `<h2>` text ("Delete this to-do?", "Clear completed to-dos?"). Cancel is focused on opening. After each close the dialog is gone and the opener is focused |
| 003 AC-4 | Delete dialog opened by keyboard on a row; confirm with the dialog's Delete button | The row and its opener have count 0; the `<h1>` "My to-dos" is focused (analyst default 6) |
| 003 AC-5 | Clear-completed dialog opened by keyboard; confirm | The `<h1>` "My to-dos" is focused (analyst default 6) |
| 004 AC-1 | Signed out, open `/signup` and read `document.title`; then sign in by API and open `/todos/<id>` of an API-created to-do | The `<h1>` (the title) is focused; `document.title` is not empty and differs from the sign-up title |
| 004 AC-2 | Open `/todos/<random lower-case UUID>`, so the API answers 404 | The `<h1>` "To-do not found" is focused; `document.title` is not empty and differs from the sign-up title |
| 004 AC-3 | Before navigation, an init script (`page.addInitScript`) records, at the first `animationstart` event, whether the `<h1>`, the sentence and the home link are in the DOM outside any `aria-hidden` subtree. Then open `/nope` signed in | The record says all three were present at first render. By role: heading level 1 "Page not found", the sentence text, link "Go to home" |
| 004 AC-4 | Same page | `.nf-illustration` has `aria-hidden="true"`. Every animation target (`document.getAnimations()`, effect target) is inside it. The aria snapshot of `main` is exactly the heading, the sentence and the link: `toMatchAriaSnapshot` with `/children: deep-equal` in the template. Matching is partial by default (`playwright/types/test.d.ts` L312-L317). So the illustration adds no name or text |
| 005 AC-1 | Route `**/api/v1/auth/session` to `abort()` before opening `/todos`. Then remove the route, move focus to Retry with Tab and press Enter | A `role="alert"` element holds "The app could not reach the server." and contains a button named "Retry". After Enter the alert is gone and the app has moved on (the log-in heading for a signed-out browser), so Retry worked by keyboard |

Request interception in 005 is test-side only. CHG-0001 uses the same technique (CHG-0001 SDD §14, NFR-006 row).
`logOut` in 001 AC-4 reaches the same SPA path as an expired session: a signed-in user's request gets a 401
(`web/src/auth/SessionProvider.tsx` L61-L67, `web/src/auth/RequireAuth.tsx` L25-L34). The real 8-hour expiry stays
covered by CHG-0001's session-expiry configuration.

## 6. Contrast measurement (REQ-A11Y-006, REQ-A11Y-007)

### 6.1 Colours: reading, compositing, ratio

**Reading colours.**

- Colours come from `getComputedStyle`: `color`, `background-color`, `border-*-color` and `outline-color`.
- `parseCssColour` accepts `rgb()`/`rgba()`. Any other form throws, naming the property. The test then stops as a test
  defect instead of guessing.
- The effective colour of an element's paint is its colour with alpha multiplied by the product of `opacity` over the
  element and its ancestors. That colour is composited (source-over) on the colour behind it.

**Effective background behind a point or element.**

- Start at the element and walk up its ancestors, collecting `background-color` until the first opaque one. Composite
  the collected colours from the bottom up. If no ancestor is opaque, the bottom colour is white, the canvas.
- An ancestor with `background-image` other than `none` throws. The app has none.
- The walk passes through a `<dialog>`, whose background is opaque.

**Relative luminance and contrast ratio.** The WCAG 2.1 definitions: sRGB channel / 255, linearised (≤ 0.04045 → /12.92,
else ((c + 0.055) / 1.055)^2.4), then L = 0.2126 R + 0.7152 G + 0.0722 B. The ratio is (L_lighter + 0.05) /
(L_darker + 0.05).

**Assertion and display (NFR-101).**

- Assertions use the unrounded ratio.
- The table shows it truncated to two decimals, so a shown "3.00" never hides a value under 3.
- Two runs on the same browser and code give the same table, so the fragments are byte-identical.

### 6.2 Adjacent colour (analyst default 7, F-18)

The adjacent colour of a control is the colour it sits against: the colour immediately outside its border box.

- **How it is read.** Take four points 1 CSS px outside the middle of each side of the border box. At each point, take
  the topmost element that is not the control or a descendant (`document.elementsFromPoint`), and read that element's
  effective background (§6.1).
- **Why points rather than the DOM parent.** This also handles positioned elements, such as the skip link over the
  header (`web/src/styles/app.css` L64-L74), where the DOM parent is not what is behind.
- **Points outside the viewport** are skipped. The control is first scrolled into view.
- **The result.** The control's ratio is the lowest ratio over the sides measured.

For a filled button with a transparent border (`.btn.primary`, `.btn.danger`, `app.css` L118, L127, L144), the measured
colour is the fill. Its adjacent colour is the page, row or dialog colour outside the button by the rule above (F-18).

### 6.3 The three measures

**Focus outline (REQ-A11Y-006 AC-1).**

- **Reaching focus.** The control gets keyboard focus with `focusByKeyboard`. It first focuses the control by script,
  then presses back-Tab and Tab (`Shift+Tab` and `Tab`, or `Alt+Shift+Tab` and `Alt+Tab` in webkit, as `pressTab`
  does). Focus therefore arrives by keyboard.
- **The first tab stop.** For the skip link, which has no stop before it, the helper moves focus to `<body>` and
  presses Tab once.
- **Precondition.** The control must be focused and match `:focus-visible`. If not, the failure message reads
  "keyboard focus not visible".
- **What is measured.** The computed `outline-color` against two adjacent colours:
  - outside the ring: the effective background at points 1 CSS px beyond `outline-offset + outline-width`;
  - inside the ring, if `outline-offset` ≥ 1 px: the gap, read at points inside it (the colour behind the control);
    otherwise the control's own boundary colour (§6.3, boundary).
- **The ratio** is the lowest over these. With the app's tokens the ring is 2 px `#1D4ED8` at offset 2 px (`app.css`
  L27-L30, `tokens.css` L27, L67-L68).
- **No indicator.** A focused control with `outline-style: none` or zero width, and `box-shadow: none`, has no
  indicator. It is recorded with ratio 1.00 and fails.
- **Radio groups.** Tab enters a group on its checked radio, so a radio's outline is measured on the checked radio.

**Boundary (REQ-A11Y-006 AC-2, AC-3; analyst default 7).**

- **Read from painted pixels.** Native checkboxes and radios are painted by the browser, and `getComputedStyle` does
  not describe what is painted. So the boundary is read from pixels for every control:
  - take an element screenshot with `locator.screenshot({ scale: 'device', animations: 'disabled', caret: 'hide' })`;
  - `'disabled'` finishes the 120 ms `.btn` background transition (`app.css` L125) before capture;
  - `'device'` avoids the down-sampling that would blend the 1 px borders under webkit's device scale factor 2.
- **Decoding.** The PNG is decoded on the analysis page (§8.3).
- **The identifying colour** is the pixel with the highest contrast against the adjacent colour (§6.2) within a region:
  - for a control with a CSS border or fill: the outer band of the border box, `ceil(max(2, border-width) ×
    devicePixelRatio)` device pixels wide. The band holds the border, or the fill when the border is transparent, but
    not the text, which sits behind the padding;
  - for a native checkbox or radio (`native: true`): the whole element box. There the UA-painted ring, fill or check is
    the only paint.
- **Native checkboxes and radios are asserted at 3:1** like every other boundary (Q13 a; REQ-A11Y-006 AC-2, AC-3,
  AC-5). If a browser paints one under 3:1, the test fails, web-qa stops and reports it, and this change waits for a
  CSS fix in a separate change (Q4).
- **Text links** (no border, no fill) are identified by their text. They have no boundary row and are covered by the
  text table.

**Text (REQ-A11Y-007).**

- **What is measured.** The control's computed `color` against the effective background behind its text: from the
  element that holds the text, walking up (§6.1). For an input, textarea, date input or select, the measured text is a
  value the test enters first.
- **Large text** (WCAG 2.1): computed `font-size` ≥ 24 px (18 pt), or ≥ 56/3 px (14 pt, about 18.67 px) with
  computed `font-weight` ≥ 700. Its threshold is 3:1; all other text is 4.5:1.
- **Transitions.** Before reading styles in a state, the test finishes running transitions on the element and its
  ancestors (`getAnimations().finish()`), so hover readings are final values (NFR-101).

### 6.4 States and how each is reached

| State | How |
|---|---|
| normal | Mouse parked at (0, 0), on the non-interactive header; focus moved to `<body>` by script |
| focus | `focusByKeyboard` (§6.3) |
| hover | Focus moved to `<body>`, then `locator.hover()` |
| selected | A checked radio (the filter's current value); a checked checkbox (a done to-do's row) |
| invalid | After a refused empty submit: inputs with `aria-invalid="true"` (border `--color-border-danger`, `app.css` L257-L261) |
| disabled | `:disabled` controls ("Clear all completed" with no completed to-dos, `TodoListPage.tsx` L347) and the `aria-disabled="true"` Previous/Next markers (`Pagination.tsx` L41, L60). Measured and listed "measured, not asserted" (REQ-A11Y-006 AC-4, REQ-A11Y-007 AC-2) |

**Not measured, and why.**

- **Busy states.** `aria-disabled` set on a button only while a request is in flight (`.btn[aria-disabled='true']`,
  `app.css` L163-L166) is transient, not a disabled state, and is not reached.
- **Programmatic focus targets.** `<h1 tabindex="-1">`, `<main tabindex="-1">` and the error summary
  `[role=alert][tabindex=-1]` are focus targets, not controls; they have `outline: none` (`app.css` L32-L36).
- **The pagination current-page marker** is a `<span aria-current="page">`, not focusable and not operable
  (`Pagination.tsx` L49-L53). Its state is carried by text: bold weight, underline and `aria-current`. Its fill
  (`#EFF6FF`, about 1.04:1 against the page `#F9FAFB`) is not a control's selected state and is not asserted (Q14 a;
  REQ-A11Y-006 AC-3). Its text is measured and asserted in the REQ-A11Y-007 text table (§6.5, list screen).

### 6.5 Controls and states per audited screen

Every screen is opened signed in through the API, except sign-up and log-in. Legend: **O** focus outline, **B**
boundary, **T** text.

| Screen and set-up | Control (measures: states) |
|---|---|
| Sign-up `/signup`, signed out; then after an empty submit | Skip link (O, T: focus). Identifier and password inputs (O: focus; B: normal, hover, invalid; T: normal, hover, focus with a typed value). "Sign up" primary button (O; B: normal, hover; T: normal, hover, focus). "Already have an account? Log in" text link (O; T: normal, hover, focus). Error-summary links (O; T: normal, hover, focus) |
| Log-in `/login`, signed out; then after an empty submit | As sign-up, with the "Log in" button and the "No account yet? Sign up" link |
| List `/todos`: 21 to-dos, one of them done (two pages); then the completed one deleted by API and the page reloaded (disabled state) | Skip link (O, T: focus). "Log out" secondary button in the header (O; B: normal, hover; T: normal, hover, focus). "Add a to-do" primary link (O; B, T as a button). "Clear all completed" danger-outline button (O; B: normal, hover, disabled; T: normal, hover, focus, disabled). Search box (O; B: normal, hover; T with a typed value). "Search" secondary button (O; B; T). Filter radio, native (O on the checked one; B: normal on "Active", hover, selected on "All"). Filter option label (B: normal, hover, selected; T: normal, selected). Row checkbox, native (O; B: normal, hover, selected on the done row). Row title link (O; T: normal, hover, focus). Row "Edit" secondary link (O; B; T). Row "Delete" danger-outline button (O; B; T). Pagination links "Page 2" and "Next" (O; T: normal, hover, focus). "Previous" marker (T: disabled). Current-page marker `<span aria-current="page">` (T: normal, asserted; its fill not asserted, Q14) |
| Create/edit form: `/todos/new`, then after an empty submit, then `/todos/<id>/edit` | Skip link, "Log out". Title input, description textarea, due-date input, priority select (O; B: normal, hover, invalid where the field can be invalid; T with a value). "Add to-do" and "Save changes" primary buttons (O; B; T). "Cancel" secondary link (O; B; T). Error-summary links (O; T) |
| Delete dialog: from the list, opened by keyboard | "Cancel" secondary button and "Delete" danger button on the dialog surface (O; B: normal, hover; T: normal, hover, focus) |
| Clear-completed dialog: from the list with one done to-do, opened by keyboard | "Cancel" and "Delete 1 completed" (as the delete dialog) |
| Detail `/todos/<id>` | Skip link, "Log out". "Edit" primary link (O; B; T). "Delete" danger-outline button (O; B; T). "Back to my to-dos" text link (O; T) |
| Not-found `/nope`, after its animations have finished | Skip link, "Log out". "Go to home" primary link (O; B; T) |

Controls the shell repeats on several screens ("Log out", skip link) are measured on each screen, because their
surroundings can differ.

### 6.6 Assertions, failures and the calibration test

**Per-screen tests.** Each screen has one REQ-A11Y-006 test and one REQ-A11Y-007 test in
`a11y-audit-contrast.spec.ts`, 16 in all.

- Each measures every row of §6.5 first, writes the fragment (§6.7), then asserts `expect(failuresOf(rows)).toEqual([])`.
- A failure message reads `<screen> / <control> / <state> / <measure>: <ratio> < <threshold>`, so a failing run names
  the control, the state and the measured ratio (REQ-A11Y-006 AC-5, REQ-A11Y-007 AC-3).
- Rows marked disabled are never in the failure list.

**Calibration test.** `REQ-A11Y-006 AC-5 REQ-A11Y-007 AC-3 NFR-101 AC-1 the contrast measurement reproduces reference
ratios and reports a ratio under its threshold` runs the same helpers on a fixture loaded with `page.setContent` (no
app). It asserts:

- text `#000000` on `#FFFFFF` measures 21.00;
- a 1 px `#767676` border on `#FFFFFF` measures 4.54 (boundary);
- a 1 px `#AAAAAA` border on `#FFFFFF` (2.32) appears in `failuresOf` with its control name, state and "2.32".

This checks the failure path and the PNG decode without making up a failure in the app.

### 6.7 The ratio table

Each test writes a Markdown fragment to `web/test-results/a11y-audit/<project>/contrast-<nontext|text>-<screen>.md`,
replacing any earlier file of the same name. Its first line names the project and `browser.version()`. Its table has
these columns:

| Screen | Control | State | Measure | Foreground | Adjacent / background | Ratio | Threshold | Text size | Asserted | Result |
|---|---|---|---|---|---|---|---|---|---|---|

- **Foreground and Adjacent / background** are `#RRGGBB`, the opaque composited colours.
- **Ratio** is truncated to two decimals.
- **Threshold** is 3.00 or 4.50.
- **Text size** is filled for text rows only: normal or large.
- **Asserted** is "yes", or "measured, not asserted" for disabled.
- **Result** is pass, fail, or "—" for rows that are not asserted.

The summary copies these fragments verbatim, one table per browser (§10).

## 7. Video of the not-found animation (REQ-A11Y-008)

`a11y-audit-not-found-video.spec.ts` has one test, `REQ-A11Y-008 AC-1 AC-2 AC-4 the not-found animation is recorded on
video for its whole run, as a WebM of at most 1 MB`. It runs in chromium (AC-1) and webkit (AC-2); video is switched on
at the file's top level (§4).

1. Sign in through the API (`signedInUser`, no navigation). Register an init script that records
   `document.getAnimations().length` at the first `animationstart`.
2. Open `/nope` and wait for the `<h1>` "Page not found".
3. Wait until every animation's `finished` promise has resolved (`Promise.all` over `document.getAnimations()`). Then
   keep the page open for 500 ms, so the encoder captures the final state.
   - The recording starts when the page is created, before navigation. It therefore covers the animation from its
     first frame to its end (AC-1).
   - The init-script record must show at least one animation, so the video is not of a page without the animation.
4. Close the page. Then `page.video().saveAs()` to `web/test-results/a11y-audit/<project>/not-found-animation.webm`.
   `saveAs` "waits until the page is closed and the video is fully saved" (`playwright-core/types/types.d.ts`
   L23801-L23806).
5. Assert the file:
   - it exists;
   - it starts with the EBML magic bytes `1A 45 DF A3` (a WebM file);
   - it is more than 0 and at most 1,048,576 bytes (AC-4, analyst default 8).

The video shows only the not-found page and the header, never an identifier or password (NFR-103). The sign-in goes
through the API.

## 8. Flash check (REQ-A11Y-009, Q8 a, analyst default 9)

`a11y-audit-flash.spec.ts`: `REQ-A11Y-009 AC-1 AC-2 AC-3 AC-4 AC-5 AC-6 the not-found animation, stepped at 60 samples
per second, has at most 3 general and 3 red flashes in any one-second window`. It runs in chromium and webkit with the
same steps (AC-5).

### 8.1 Stepping (NFR-101 AC-2)

1. Sign in through the API and open `/nope`. Wait for the `<h1>`.
2. In the page, take `document.getAnimations()` (at least one, else the test fails).
3. **Start offsets.** Record each animation's offset, its `startTime` minus the earliest `startTime`, before pausing.
   A paused animation's `startTime` becomes unresolved, so it cannot be read afterwards.
4. **Pause.** Call `pause()` on each.
5. **Run end.** `endTimeMs` is the largest `effect.getComputedTiming().endTime` among them; with today's tokens that is
   1200 ms (`tokens.css` L72-L76). It is read, not hard-coded.
6. **Samples.** N = ceil(endTimeMs × 60 / 1000). Samples k = 0…N are taken at t_k = k × 1000 / 60 ms (73 samples for
   1200 ms). The run length covered is N / 60 s.
7. **Setting the time.** For each sample, each animation's `currentTime` is set to t_k minus its recorded offset,
   clamped at 0. The page's animations start in the same frame, so this is t_k for all of them.
8. Wait two `requestAnimationFrame` callbacks so style and paint reflect the new time.
9. Take `page.screenshot({ scale: 'device', animations: 'allow', caret: 'hide' })` of the viewport, not the full
   page. `'allow'` is required: `'disabled'` would fast-forward the animations to their end.

No wall-clock wait drives the sampling, so the result does not depend on machine speed.

### 8.2 Per-pixel quantities

For each pixel of each sample, linearise sRGB as in §6.1 to R, G, B in 0–1:

- **Relative luminance:** L = 0.2126 R + 0.7152 G + 0.0722 B.
- **Saturated:** R / (R + G + B) ≥ 0.8. A black pixel (R + G + B = 0) is not saturated.
- **Red value:** max(0, (R − G − B) × 320).

These follow the working definition of "pair of opposing transitions involving a saturated red" in the notes to WCAG
2.1's definition "general flash and red flash thresholds". Whether the 2.1 notes give this formula or the older CIE
1976 UCS chromaticity form could not be checked offline while this design was written. The test author confirms it
against the WCAG 2.1 text and cites it in the spec file; if 2.1 gives the other form, that form is used.

### 8.3 Decoding without a new dependency (F-24)

Screenshots are decoded and analysed on the analysis page:

- **The page.** `openAnalysisPage(browser)` opens `about:blank` in a new context of the same browser (`browser.newPage()`).
  This keeps it apart from the audited page's context: no cookies, no video, and no effect on the audited page's focus
  or DOM.
- **Decoding.** Each PNG travels as base64. It is decoded with `createImageBitmap(blob, { colorSpaceConversion:
  'none', premultiplyAlpha: 'none' })`, drawn on a canvas that is never attached to a document, and read with
  `getImageData`.
- **What stays in the page.** Per-pixel state is kept there in typed arrays. Only the counts return to Node, so 73 full
  viewports of pixels never cross the protocol (NFR-102).
- **Dependencies.** Canvas and `createImageBitmap` are in both engines. No package is added (REQ-A11Y-010 AC-3).
- **Shared use.** The contrast boundary measurement (§6.3) uses the same decoder.
- **Code.** The arithmetic of `colour.ts` is written as self-contained functions, so they can be passed to the
  analysis page by their source.

### 8.4 Transitions, flashes and windows

**General transitions.** Each pixel's luminance series is scanned with hysteresis:

- Track the extreme reached since the last transition and the current direction.
- At the start the direction is unknown, and both the minimum and the maximum since the start are tracked.
- A general transition is recorded at sample f when the luminance has moved, against the current direction or from an
  unknown direction, by ≥ 0.1 from the tracked extreme, with the darker of the two states below 0.80.
- The direction then flips and the extreme restarts at the new value. A movement in the current direction only extends
  the extreme.

**Red transitions.** The same scan on the red value, with these thresholds: a change > 20, and at least one of the two
states saturated.

**Flashes in a window.**

- A flash is a pair of opposing transitions. The flash count of a pixel in a window is floor(transitions in the window
  / 2).
- A one-second window spans 60 sample intervals: window w covers samples w…w + 60 and counts the transitions at samples
  f with w < f ≤ w + 60.
- Windows slide one sample at a time, w = 0…max(0, N − 60). A run shorter than one second is one window.
- A window's count is the highest count of any single pixel in it (analyst default 9).
- The whole viewport counts, with no small-area exemption (AC-4).

**Storage.** Transitions are kept as a sparse list of (pixel, sample). Only pixels whose luminance or red value changes
by the threshold produce entries, so memory follows the animated area.

### 8.5 Verdict and recorded figures

- **Non-vacuity, asserted first.** `pixelsWithGeneralTransition` must be ≥ 1. The card's grey border and lines
  (`#6B7280`) appear over the page (`#F9FAFB`), a change of about 0.79 in luminance, so stepped frames that really
  repaint always produce one. If every frame were the same (stepping not repainted, R2), the test fails here and is not
  a silent 0.
- **Pass** (AC-3) when the largest general count and the largest red count over all windows are each ≤ 3.
- **Failure message** otherwise: `general|red flashes: <count> in window <w> (<t_w> ms to <t_w + 1000> ms)`.
- **Recorded figures** (AC-6). Before asserting, the test writes `web/test-results/a11y-audit/<project>/flash.md`:
  - browser version;
  - samples (N + 1);
  - `endTimeMs`, and the run length covered;
  - window size (60 intervals);
  - the number of pixels with at least one general transition;
  - the highest general count and its window;
  - the highest red count and its window;
  - the verdict.

### 8.6 Calibration test

`REQ-A11Y-009 AC-2 AC-3 NFR-101 AC-1 the flash counter counts known synthetic sequences` runs the same pipeline
(stepping excluded) on a 100 × 100 viewport page filled by `page.setContent`. Its colour is set per sample, and each
sample is screenshotted and decoded:

| Sequence (61 samples) | Expected maximum general flashes | Expected maximum red flashes |
|---|---|---|
| Black and white alternating every sample | 30, in window 0, reported over the limit | 0 |
| `#000000` and `#FF0000` alternating every sample | 30 | 30 |
| A linear fade from black to white | 0 | 0 |

This proves the counting and the over-limit report without making up a failure in the app.

## 9. Run, first run and test-first order (REQ-A11Y-010, REQ-A11Y-012)

**Where the tests run (REQ-A11Y-010 AC-1, AC-2).**

- The new files run under `web/playwright.config.ts`: its global setup starts the API with the §11 command, and its
  `webServer` starts the Vite dev server.
- `pnpm -C web e2e` runs `e2e:main` first, so it includes them. The files run in chromium and webkit; firefox ignores
  them (§4).
- The check is `pnpm -C web e2e:main --list`: the new test names appear under chromium and webkit only.

**Test-first order (Q11 a; REQ-A11Y-010 AC-4, AC-5; F-25).**

1. **Write first.** web-qa writes the spec files and the support modules, and the firefox `testIgnore` line, before any
   other non-documentation change of this change. There is no product code to follow; nothing is stubbed, and no
   failing state is made up.
2. **The first run of each new test** is its first execution against develop's app. web-qa takes it at the tests node
   with the files as first written, before the test commit:
   - command: `PLAYWRIGHT_JUNIT_OUTPUT_NAME=test-results/chg-0004-first-run-junit.xml pnpm -C web e2e:main
     --project=chromium --project=webkit a11y-audit-`;
   - its JUnit is attached as `test-report` evidence at once, so the first-run result of every test is kept, pass or
     fail, whatever happens next;
   - web-qa writes the first-run section of the summary source `web/e2e/a11y-audit/CHG-0004-summary.md` (§10) in the
     same node, inside `web/`.
3. **Handling a first-run failure** depends on its cause (REQ-A11Y-012):
   - **A real WCAG failure** (AC-1 to AC-3): the test stays as written and stays failing. The summary records the
     screen, control or state, criterion and measured value. This change waits for a separate fix change.
   - **A wording difference listed in defaults 1, 2, 3 or 6**: the tests already check the app's text (§5). A
     difference found that is not in that list makes web-qa stop and report a spec question (AC-5).
   - **A test defect** (AC-6), for example video missing because the file's `test.use` was not yet in place, or a wrong
     locator: web-qa fixes the test or its support without weakening any assertion and runs again. The rerun's JUnit
     is attached as a further `test-report`. The fix, and any change to `web/playwright.config.ts`, land in the same
     test commit, because the first run precedes that commit.
4. **What the history shows.** The commit records the tests as fixed; the evidence and the summary keep the failure.
   That is the reading REQ-A11Y-012 AC-6 gives ("lands in the test commit").
5. **A test that passes on its first run** is recorded as passed. No `test.fail()`, `test.skip()` or `test.fixme()` is
   used (REQ-A11Y-010 AC-5).

How the harness completes a tests node whose tests pass on their first run is settled at planning (Q11 a, analyst
default 11), not in this design.

## 10. Evidence and the summary (REQ-A11Y-011, REQ-A11Y-012, REQ-A11Y-013)

**Evidence run** (web-qa, after the tests node, on this change's branch, which holds develop's app unchanged):

1. Delete `web/test-results/a11y-audit/` and `web/test-results/a11y-audit-run1/`.
2. Run `PLAYWRIGHT_JUNIT_OUTPUT_NAME=test-results/chg-0004-e2e-main-junit.xml pnpm -C web e2e:main --project=chromium
   --project=webkit`. This is the whole main suite, existing and new tests, in the two required projects (NFR-102
   AC-1). Firefox is not run: it does not launch on the build host (CHG-0001 N12 validator F-9, F-19), and the new
   tests exclude it anyway.
3. **Keep run 1.** Write the list of `.webm` files under `web/test-results/e2e-artifacts/` to
   `web/test-results/a11y-audit/webm-listing.txt`. Then copy `web/test-results/a11y-audit/` to
   `web/test-results/a11y-audit-run1/`. The summary's tables, figures and listing, the attached videos and the JUnit
   come from this run 1. The later runs overwrite `web/test-results/a11y-audit/` and clean `e2e-artifacts/`.
4. **Reproducibility (NFR-101 AC-1).** Run `pnpm -C web e2e:main --project=chromium --project=webkit
   a11y-audit-contrast a11y-audit-flash` again. Compare its `contrast-*.md` and `flash.md` with run 1's, byte for byte,
   and record the result in the summary.
5. **Suite time (NFR-102 AC-2).** Time `pnpm -C web e2e:main --project=chromium a11y-audit-` wall-clock on the build
   host, including server start-up. Record it (limit 5 minutes).

**Attached with `harness evidence attach`:**

- `web/test-results/chg-0004-first-run-junit.xml` (and any rerun file from §9) and `chg-0004-e2e-main-junit.xml`, as
  `test-report` (AC-1, analyst default 12);
- `web/test-results/a11y-audit-run1/chromium/not-found-animation.webm` and `…/webkit/not-found-animation.webm`, as
  `other` (AC-2);
- `web/e2e/a11y-audit/CHG-0004-summary.md`, as `other` (AC-3);
- not the Playwright HTML report (`web/playwright-report/`), which stays local (AC-4).

**Summary source.** The summary's committed source is `web/e2e/a11y-audit/CHG-0004-summary.md`:

- **Location.** It is inside `web/`, the module the web-qa nodes are given, and outside `docs/specs/CHG-0004/`, which
  may hold only spec files. It is not a test file, because Playwright matches only `*.spec.ts`.
- **Who writes it.** web-qa starts it at the tests node (section 3 below) and completes it after the evidence run.

Its sections, in order:

1. **Audit context:** develop's commit audited, Playwright version, chromium and webkit versions, WCAG 2.1 AA, the date.
2. **Outcome:** either "No failure was found" (REQ-A11Y-012 AC-4), or each real WCAG failure with screen, control or
   state, criterion and measured value, and the statement that this change waits for its fix (AC-1, AC-2).
3. **First-run results** (REQ-A11Y-010 AC-4):
   - one row per new test, with its chromium and webkit results from the first-run JUnit;
   - for each failure: its cause (real WCAG failure, spec-wording difference or test defect), what was done, and the
     rerun result;
   - the evidence file names.
4. **Announcement and state results:** one row per AC of REQ-A11Y-001 to REQ-A11Y-005, with the test name and its
   chromium and webkit results from the evidence run.
5. **Contrast ratio tables:** the §6.7 fragments verbatim, non-text then text, per browser.
6. **Flash check:** the `flash.md` figures per browser (REQ-A11Y-009 AC-6).
7. **Videos:** file, browser and size; that video was on only for the REQ-A11Y-008 test (REQ-A11Y-008 AC-3). The proof
   is run 1's `webm-listing.txt`: every `.webm` under `web/test-results/e2e-artifacts/` is in that test's output
   folders.
8. **Spec-wording differences** (REQ-A11Y-012 AC-5). For each: the AC, the spec or Q10 wording, the app wording and the
   app's `file:line`. At least these:
   - default 1: `web/src/auth/LoginPage.tsx` L14, L89;
   - default 2: `web/src/todos/TodoListPage.tsx` L314;
   - default 3: `TodoListPage.tsx` L314;
   - default 6: `TodoListPage.tsx` L244, L268.
9. **Test defects** (REQ-A11Y-012 AC-6): each, with its fix.
10. **Reproducibility and suite time** (NFR-101, NFR-102).
11. **Open note: manual screen-reader pass** (REQ-A11Y-013):
    - "Open — not done."
    - Person: @alokkulkarni. Screen reader and browser: VoiceOver with Safari on macOS.
    - Screens: sign-up, login, list, create/edit form, delete dialog, clear-completed dialog, to-do detail, not-found
      page.
    - It does not block this change's merge. It will be recorded in a change of its own.
    - No other wording appears anywhere in the change: never done, passed or waived.
12. **Evidence list:** the attached files. The HTML report is not attached.

The summary and the result fragments hold no account identifier, password or cookie (NFR-103).

## 11. Knowledge-bundle notes (REQ-A11Y-014)

This change's docs node (okf-documenter) updates three notes. Each keeps its front matter and existing content apart
from the changes below.

Each of the three notes says that the automated accessibility-tree, contrast (ratio tables), video and flash checks are
recorded in CHG-0004, and links `web/e2e/a11y-audit/CHG-0004-summary.md` (AC-1). Each also says that the manual
screen-reader pass (@alokkulkarni, VoiceOver and Safari) is open and not done (AC-2). Where each note changes:

- **`docs/knowledge/requirements/nfr-001.md`:** the "Not recorded (open gap)" paragraph (L27-L31) is replaced by those
  two statements, and the `summary` front-matter line is changed to match.
- **`docs/knowledge/requirements/req-nav-001.md`:** L28-L29 are replaced by those two statements. The note adds that
  the video and the flash check are the AC-6 evidence.
- **`docs/knowledge/tests/accessibility-tests.md`:** the four `a11y-audit-*.spec.ts` files are added to `sources` and
  the list. The "Manual pass: not recorded" item (L21-L23) is replaced by those two statements.

None of the three says the flash review or the contrast pass is unrecorded. The exception is a REQ-A11Y-012 failure:
then the note names that failure and says the check stays open until its fix (AC-3). None calls the screen-reader pass
done, passed or waived (REQ-A11Y-013 AC-2).

## 12. Security, data and observability

- **Data (NFR-103).** Accounts come only from `newAccount()` (generated identifier, the existing fixed test password in
  `web/e2e/support/app.ts` L38-L40). There are no secrets or real credentials.
- **Videos.** They are recorded after an API sign-in, so they never show a typed identifier or password. The header
  shows no identifier.
- **Observability.** The change adds no observability element (solution architecture).

## 13. Risks and follow-ups

| # | Risk or follow-up | Handling |
|---|---|---|
| R1 | A native checkbox or radio is painted under 3:1 by a browser. WCAG 1.4.11 exempts appearance "determined by the user agent and not modified by the author", but REQ-A11Y-006 AC-2 asserts it | Asserted at 3:1 as written (Q13 a; §6.3). If a browser paints one under 3:1, the test fails, web-qa stops and reports it, and this change waits for a CSS fix in a separate change (Q4) |
| R2 | WebKit may not paint a paused animation's `currentTime` in a screenshot, or may paint it a frame late | The two `requestAnimationFrame` waits. The non-vacuity assertion (§8.5) fails if frames never change. The calibration covers decoding, not stepping. A stepping problem is reported as a test defect, and the count is not loosened |
| R3 | Canvas decoding applies colour management to the PNG and shifts colours | `colorSpaceConversion: 'none'`. The calibration's 21.00 and 4.54 reference ratios would expose a shift |
| R4 | `:focus-visible` heuristics differ between engines | Focus reached by keyboard (`focusByKeyboard`), and `:focus-visible` is checked before measuring |
| R5 | Hover transitions are read mid-way | Transitions finished before reading; element screenshots use `animations: 'disabled'` |
| R6 | Stale fragments from older runs end up in the summary | The evidence run deletes `web/test-results/a11y-audit/` first. Each test overwrites its own file. One file per project and test, never a shared file (`fullyParallel`) |
| R7 | Suite time over 5 minutes (NFR-102 AC-2) | Device-scale screenshots in webkit (2560 × 1440) are the heaviest part; analysis stays in the browser (§8.3). If the limit is exceeded, report it; do not drop measurements |
| R8 | The firefox `testIgnore` override drops the existing patterns | §4 lists all three patterns; the `--list` check of §9 confirms it |
| R9 | The red-flash formula in WCAG 2.1 is not confirmed (§8.2) | The test author confirms it against WCAG 2.1 and cites it in the spec file |
| R10 | The shell's live region clears after 10 s (`AppShell.tsx` L27) | REQ-A11Y-002 AC-7 asserts right after the action, within Playwright's default 5 s timeout |
| R11 | The boundary measure takes the highest-contrast pixel in its region (§6.3), the most lenient statistic. A thin or partial dark edge, such as a UA shadow on a native control, could pass a control whose main boundary is lighter | The table shows the identifying colour found, so a reviewer can see which colour carried the ratio. If a reviewer finds a pass carried by such an edge, they report it as a finding against this design; the statistic is not changed during the build |
| F1 | **Follow-up (requester's decision, relayed from @alokkulkarni: "switch N05 to a minimal architecture stub and continue but make a note so its not lost and fixed after M22").** `docs/architecture/solution.md` is a placeholder that CHG-0004 wrote. CHG-0005 (PR #7, frozen, parked until harness M22) writes the whole-app solution architecture | Whichever of CHG-0004 and CHG-0005 merges second reconciles `docs/architecture/solution.md`. CHG-0005's version is the intended content |

## 14. Traceability

"e2e" means a Playwright test under the main configuration in chromium and webkit. "review" means a check by the
validator or reviewer on the diff, evidence or files. Contract element: none for every row (no interface).

| Requirement / AC | SDD § | Planned verification |
|---|---|---|
| REQ-A11Y-001 AC-1, AC-2 | §5 | e2e `a11y-audit-announcements.spec.ts`: "REQ-A11Y-001 AC-1 AC-2 sign-up: …", "REQ-A11Y-001 AC-1 AC-2 log in: …" |
| REQ-A11Y-001 AC-3 | §5 | e2e "REQ-A11Y-001 AC-3 after sign-up the log-in page shows Account created. Log in. in a status region" |
| REQ-A11Y-001 AC-4 | §5 (F-19) | e2e "REQ-A11Y-001 AC-4 after the session ends the log-in page announces the session-ended text in a status or alert region" |
| REQ-A11Y-001 AC-5 | §5 | e2e "REQ-A11Y-001 AC-5 a refused log in shows the invalid-credentials alert" |
| REQ-A11Y-001 AC-6 | §5 | e2e "REQ-A11Y-001 AC-6 create and edit forms: an empty title gives the error summary, links, aria-invalid and descriptions" |
| REQ-A11Y-001 AC-7 | §5 | e2e "REQ-A11Y-001 AC-7 the create and edit routes focus their h1" |
| REQ-A11Y-002 AC-1 | §5 | e2e "REQ-A11Y-002 AC-1 page 1 of 45 announces Showing 1-20 of 45 in a polite live region" |
| REQ-A11Y-002 AC-2 | §5 | e2e "REQ-A11Y-002 AC-2 the empty and no-match states are level-2 headings" |
| REQ-A11Y-002 AC-3 | §5 | e2e "REQ-A11Y-002 AC-3 the filter is a named group of radios with exactly one checked" |
| REQ-A11Y-002 AC-4 | §5 (Q12) | e2e "REQ-A11Y-002 AC-4 the Pagination landmark marks the current page with aria-current" |
| REQ-A11Y-002 AC-5 | §5 | e2e "REQ-A11Y-002 AC-5 a pagination link moves focus to the list h1" |
| REQ-A11Y-002 AC-6 | §5 | e2e "REQ-A11Y-002 AC-6 a filter change and a search keep focus on the control used" |
| REQ-A11Y-002 AC-7 | §5 | e2e "REQ-A11Y-002 AC-7 clearing with no completed to-dos left announces politely and opens no dialog" |
| REQ-A11Y-002 AC-8 | §5 | e2e "REQ-A11Y-002 AC-8 acting on a to-do that no longer exists shows the refreshed alert" |
| REQ-A11Y-003 AC-1 to AC-3 | §5 | e2e "REQ-A11Y-003 AC-1 AC-2 AC-3 the delete dialog: …" and "… the clear-completed dialog: …" |
| REQ-A11Y-003 AC-4 | §5 | e2e "REQ-A11Y-003 AC-4 confirming a delete moves focus to the list heading" |
| REQ-A11Y-003 AC-5 | §5 | e2e "REQ-A11Y-003 AC-5 confirming clear completed moves focus to the list heading" |
| REQ-A11Y-004 AC-1, AC-2 | §5 | e2e "REQ-A11Y-004 AC-1 the detail page focuses its h1 and sets its title", "REQ-A11Y-004 AC-2 the to-do not-found state focuses its h1 and sets its title" |
| REQ-A11Y-004 AC-3, AC-4 | §5 | e2e "REQ-A11Y-004 AC-3 the not-found page's heading, sentence and link are in the tree at first render", "REQ-A11Y-004 AC-4 the not-found animation is hidden and adds no name or text" |
| REQ-A11Y-005 AC-1 | §5 | e2e "REQ-A11Y-005 AC-1 the start-up alert names the failure and offers a keyboard-operable Retry" |
| REQ-A11Y-006 AC-1 to AC-4 | §6.1–§6.5, §6.7 | e2e `a11y-audit-contrast.spec.ts`, 8 tests "REQ-A11Y-006 AC-1 AC-2 AC-3 AC-4 AC-5 <screen>: focus outlines, boundaries, hover and selected states meet 3:1"; ratio-table fragments |
| REQ-A11Y-006 AC-5 | §6.6 | the per-screen tests' failure messages; e2e calibration "REQ-A11Y-006 AC-5 REQ-A11Y-007 AC-3 NFR-101 AC-1 …" (the under-threshold fixture is reported) |
| REQ-A11Y-007 AC-1, AC-2 | §6.1–§6.5, §6.7 | e2e, 8 tests "REQ-A11Y-007 AC-1 AC-2 AC-3 <screen>: control text meets 4.5:1 (3:1 large) in every state" |
| REQ-A11Y-007 AC-3 | §6.6 | as REQ-A11Y-006 AC-5 |
| REQ-A11Y-007 AC-4 | §2 | review: `web/e2e/a11y.spec.ts` unchanged in the change's diff; its tests pass in the evidence JUnit |
| REQ-A11Y-008 AC-1, AC-2, AC-4 | §7 | e2e `a11y-audit-not-found-video.spec.ts` "REQ-A11Y-008 AC-1 AC-2 AC-4 …" in chromium and webkit; videos attached |
| REQ-A11Y-008 AC-3 | §4, §10 | review: no `video` key in the configuration's `use`; `test.use` only in the video file; `.webm` listing after the evidence run (summary section 7) |
| REQ-A11Y-009 AC-1 to AC-6 | §8 | e2e `a11y-audit-flash.spec.ts` "REQ-A11Y-009 AC-1 … AC-6 …" in chromium and webkit; `flash.md` |
| REQ-A11Y-009 AC-2, AC-3 (counting and failure report) | §8.6 | e2e calibration "REQ-A11Y-009 AC-2 AC-3 NFR-101 AC-1 the flash counter counts known synthetic sequences" |
| REQ-A11Y-010 AC-1 | §3, §9 | review: files in `web/e2e/`; `pnpm -C web e2e:main --list` |
| REQ-A11Y-010 AC-2 | §4, §9 | review: `--list` shows the new tests under chromium and webkit only; evidence JUnit shows them passing in both |
| REQ-A11Y-010 AC-3 | §2, §4 | review: the change's diff touches only `web/e2e/**`, the firefox entry of `web/playwright.config.ts`, `docs/**` and `.harness/**`; `web/package.json` and the lockfile are unchanged |
| REQ-A11Y-010 AC-4, AC-5 | §9 | review: commit order; first-run JUnit evidence; summary section 3; no `test.fail`/`skip`/`fixme` in the new files |
| REQ-A11Y-010 AC-6 | §3, §5–§8 | review: every new test name starts with its requirement ID and AC |
| REQ-A11Y-011 AC-1 to AC-4 | §10 | review: `.harness/changes/CHG-0004/evidence/` holds the JUnit files (`test-report`), two videos and the summary (`other`), and no HTML report |
| REQ-A11Y-012 AC-1 to AC-3 | §9, §10 | review: on a real failure, the test unchanged and failing, the summary's outcome section, and no product-code diff |
| REQ-A11Y-012 AC-4 | §10 | review: the summary states "No failure was found" when all pass |
| REQ-A11Y-012 AC-5 | §5, §10 | e2e assertions use the app's text (defaults 1, 2, 3, 6); review: summary section 8 |
| REQ-A11Y-012 AC-6 | §9, §10 | review: rerun evidence and summary section 9 |
| REQ-A11Y-013 AC-1 to AC-3 | §10 | review: summary section 11; no "done", "passed" or "waived" for the pass in any file of the change |
| REQ-A11Y-014 AC-1 to AC-3 | §11 | review of the three notes after the docs node |
| NFR-101 AC-1 | §6.1, §8.5, §10 | review: byte-identical fragments across two runs (summary section 10); calibration tests |
| NFR-101 AC-2 | §8.1 | review of `flash.ts`: no wall-clock wait in the sampling |
| NFR-102 AC-1 | §2, §10 | evidence JUnit: existing tests pass; review: existing spec files unchanged |
| NFR-102 AC-2 | §10 | timed run recorded in summary section 10 |
| NFR-103 AC-1 | §7, §12 | review of the new tests, videos and summary; the repository's secret scan in the gates |
