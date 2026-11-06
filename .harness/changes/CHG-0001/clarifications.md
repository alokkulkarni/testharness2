# Clarifications — CHG-0001

Change: "Create a to-do list". Source: `docs/specs/CHG-0001/intent.md` (title only; the Problem, Who has it,
Outcome wanted, Out of scope and How success is observed sections are empty). The repository has no product code
yet (`.harness/config.yaml` has `stacks: []`; `.harness/platform.yaml` lists the repo stack as `other`).

Note (security baseline, not a requester answer): passwords are stored only as a strong one-way salted hash. The
spec treats this as a non-negotiable security requirement for the sign-up and login chosen in Q14; it was not put
to the requester as a question.

Note (answer mapping): the requester answered Q18 and Q23–Q29 against a five-point list the coordinator sent them,
not against these question numbers; and Q31–Q34 against a four-point list (1 = Q31, 2 = Q32, 3 = Q33, 4 = Q34);
and Q36–Q44 against a nine-point list relayed in chat on 2026-11-06 (1 = Q36 … 9 = Q44, in order).
Each answer below quotes their words and names which point of that list it came from.

## Q1. Is this change a to-do list app, as the intent file says?

The intent file's title and body describe "a new todo list app where user can create, list, delete and update
todo's", but the commit that added it (`68b3f47`) is titled "customers can save payment methods". (a) It is the
to-do list app only; the commit title is a mistake. (b) Something else, or payment methods are also in scope.
Recommended: (a), because the file body and the change title both say to-do list. Please also say, in a sentence
each if you can, who uses it and what outcome you want, since those intent sections are empty.

**Answer:** @alokkulkarni: "yes todo app only" — (a) the to-do list app only. (Who uses it and the outcome wanted: see Q11.)

## Q2. What shape should the app take?

(a) A web UI backed by an HTTP API. (b) An HTTP API only (no UI). (c) A browser-only UI that stores data locally
(no backend). (d) A command-line tool. Recommended: (a), because "user can create, list, delete and update" suggests
a person using a screen, and an API keeps the data behind a contract the tests can check.

**Answer:** @alokkulkarni: "a" — (a) a web UI backed by an HTTP API.

## Q3. Which technology stack should it be built in?

The harness supports React/TypeScript for web UI, and Java/Spring Boot or C#/.NET for a backend. (a) React +
TypeScript UI with a Java/Spring Boot API. (b) React + TypeScript UI with a .NET API. (c) React + TypeScript only
(if Q2 is (c)). (d) Another stack you name. Recommended: (a) if Q2 is (a). Note: the squad roster read from
`develop` (`harness squad status --json`) has no coder role today (only lead, analyst, tester, documenter and
reviewer), so whichever stack you choose, a person will need to add the matching engineer role to the squad on
`develop` before the build phase can run. Can you confirm you will do that, or should the build phase wait?

**Answer:** @alokkulkarni: "a" — (a) React + TypeScript UI with a Java/Spring Boot API. (Adding the engineer role on `develop`: see Q12.)

## Q4. Where are to-dos stored, and must they survive a restart?

(a) In memory only (lost on restart). (b) An embedded file database such as SQLite or H2 file mode. (c) A server
database such as PostgreSQL. (d) Browser local storage (only with Q2 (c)). Recommended: (b), because data survives a
restart without needing a database server to run or deploy.

**Answer:** @alokkulkarni: "b" — (b) an embedded file database. (Which one, SQLite or H2: see Q13.)

## Q5. Are there user accounts, or is it a single shared list?

(a) One list, no login (a single anonymous user). (b) Each user signs in and sees only their own to-dos.
Recommended: (a) for the first release, because the intent mentions no accounts, and (b) adds authentication,
password storage and permissions.

**Answer:** @alokkulkarni: "b" — (b) each user signs in and sees only their own to-dos. (Sign-up and login, and an admin role: see Q14, Q15.)

## Q6. What fields does a to-do have?

(a) Title and done/not-done status only. (b) Title, optional description, done status, created and updated
timestamps. (c) As (b) plus a due date and/or priority. Recommended: (b). Please also give the title's limits:
required, trimmed, and a maximum length (suggested 200 characters); and the description's maximum (suggested 2000).

**Answer:** @alokkulkarni: "c" — (c) title, optional description, done status, created and updated timestamps, plus a due date and/or priority. (Which of due date and priority, and the length limits: see Q16, Q17.)

## Q7. What does "update" cover?

(a) Editing the title/description and marking done or not done. (b) Only marking done or not done. (c) Only editing
the text. Recommended: (a). Also, if two edits to the same to-do arrive at once, is "last write wins" acceptable
(recommended), or must the second be rejected as a conflict?

**Answer:** @alokkulkarni: "a" — (a) editing the title/description and marking done or not done. (Concurrent edits: see Q18.)

## Q8. How does delete behave?

(a) Permanent delete, with a confirmation prompt in the UI. (b) Permanent delete, no prompt. (c) Soft delete
(hidden, recoverable or with undo). Also: should there be a "clear all completed" action? Recommended: (a), with no
"clear completed" in the first release.

**Answer:** @alokkulkarni: "a" — (a) permanent delete, with a confirmation prompt in the UI. ("Clear completed": see Q19.)

## Q9. How is the list shown?

(a) All to-dos, newest first, no filtering. (b) As (a) plus filters for All / Active / Completed. (c) As (b) plus
search and pagination. Recommended: (b), with no pagination and an empty-state message when there are no to-dos.
If you expect many to-dos (more than a few hundred), say so and we will add pagination.

**Answer:** @alokkulkarni: "c" — (c) all to-dos newest first, filters for All / Active / Completed, plus search and pagination. (What search covers and how pagination works: see Q20, Q21.)

## Q10. What non-functional bar applies, and how is the app run?

(a) Runs locally only (developer machine), WCAG 2.1 AA accessibility for the UI, no performance target beyond
"feels instant" for a few hundred items. (b) As (a), plus it must deploy to the `dev`/`test` environments (please
name the hosting, e.g. a container platform). Recommended: (a) for the first release; the repository has no
deployment workflows yet. Please also say how you will judge success (for example: "I can add, see, edit, complete
and delete a to-do, and it is still there after a restart").

**Answer:** @alokkulkarni: "a" — (a) runs locally only, WCAG 2.1 AA for the UI, no performance target beyond "feels instant" for a few hundred items. (The success measure: see Q22.)

## Q11. Who uses the app, and what outcome do you want from it?

Follow-up to Q1; the intent's "Who has it" and "Outcome wanted" sections are empty. Recommended reading to confirm
or correct: "Individual people who want to keep track of their own tasks; the outcome is that each person can
capture, review and finish their tasks in one place."

**Answer:** @alokkulkarni: "users and should be able to use it successfully to create and manage todos"

## Q12. Will you add a Java and a React engineer role to the squad on `develop`, or should the build phase wait?

Follow-up to Q3. The squad roster on `develop` has no coder role, so build nodes cannot be claimed. Only a person
changes the squad file. (a) You add `java-engineer` and `react-engineer` to the squad on `develop` before the build
phase. (b) The change stops after the spec freeze until the roster changes. Recommended: (a); spec work can go on
while the role is added.

**Answer:** @alokkulkarni: "Yes + qa + BA" — (a) the requester adds `java-engineer` and `react-engineer`, plus QA and BA roles, to the squad on `develop` themselves before the build phase.

## Q13. Which embedded database: SQLite or H2 (file mode)?

Follow-up to Q4. (a) H2 in file mode: native to Java/Spring, no extra driver work. (b) SQLite: a single file other
tools can open, but a less common fit for Spring Boot and JPA. Recommended: (a), because the backend is Java/Spring
Boot (Q3).

**Answer:** @alokkulkarni: "sqlite" — (b) SQLite.

## Q14. How do users sign up and log in?

Follow-up to Q5. (a) Self sign-up with email (or username) and password, then a session or token login.
(b) Accounts created only by an admin, then login with username and password. (c) Login through an external
identity provider (e.g. GitHub or Google OAuth). Recommended: (a) with passwords stored as a strong one-way hash, no
email verification and no password reset in the first release.

**Answer:** @alokkulkarni: "a" — (a) self sign-up with email (or username) and password, then a session or token login. (Email verification and password reset: see Q27, Q28. Password hashing: see the security-baseline note at the top. Which identifier: see Q31.)

## Q15. Is there an admin role?

Follow-up to Q5. (a) No admin; every user sees and manages only their own to-dos. (b) An admin can list and
disable users (but not read their to-dos). (c) An admin can also see all to-dos. Recommended: (a) for the first
release.

**Answer:** @alokkulkarni: "a" — (a) no admin; every user sees and manages only their own to-dos.

## Q16. Which extra fields: due date, priority, or both?

Follow-up to Q6. (a) Due date only (date, no time). (b) Priority only (Low / Medium / High). (c) Both. Both are
optional on a to-do. Recommended: (c), with due date as a date without time and priority Low / Medium / High,
default Medium.

**Answer:** @alokkulkarni: "both" — (c) both a due date and a priority. (Details: Q24, Q25, Q26, Q30.)

## Q17. What are the length limits for title and description?

Follow-up to Q6. Recommended: title required, trimmed, 1–200 characters; description optional, up to 2000
characters. Please confirm or give other limits.

**Answer:** @alokkulkarni: "2000" (Clarified in Q23.)

## Q18. When two edits to the same to-do arrive at once, is "last write wins" acceptable?

Follow-up to Q7. (a) Last write wins. (b) The second edit is rejected as a conflict and the user reloads.
Recommended: (a), because each list belongs to one user (Q5), so simultaneous edits are rare.

**Answer:** @alokkulkarni (point 2): "yes last write wins" — (a) last write wins.

## Q19. Should there be a "clear all completed" action?

Follow-up to Q8. (a) No. (b) Yes, with a confirmation prompt like single delete. Recommended: (a) for the first
release.

**Answer:** @alokkulkarni: "yes" — (b) yes, a "clear all completed" action, with a confirmation prompt like single delete.

## Q20. Which fields does search look in, and how does it match?

Follow-up to Q9. (a) Title only. (b) Title and description. Matching is case-insensitive "contains". Search
combines with the active filter. Recommended: (b).

**Answer:** @alokkulkarni: "b" — (b) title and description; case-insensitive "contains", combined with the active filter.

## Q21. How does pagination work, and what is the page size?

Follow-up to Q9. (a) Numbered pages with previous/next, 20 to-dos per page. (b) "Load more" (infinite scroll),
20 at a time. (c) Another style or size. Recommended: (a), 20 per page, which keeps the API simple
(page and size parameters).

**Answer:** @alokkulkarni: "a" — (a) numbered pages with previous/next. (Page size: Q29.)

## Q22. How will you judge that the change succeeded?

Follow-up to Q10. Recommended: "A signed-in user can add, see, search, filter, edit, complete and delete their own
to-dos; another user cannot see them; and the to-dos are still there after a restart."

**Answer:** @alokkulkarni: "correct as mentioned" — accepts the proposed statement: "A signed-in user can add, see, search, filter, edit, complete and delete their own to-dos; another user cannot see them; and the to-dos are still there after a restart."

## Q23. Does the 2000-character limit apply to the title, the description, or both, and is the title still required?

Follow-up to Q17 ("2000"). (a) Description up to 2000; title required, up to 200. (b) Both title and description up
to 2000; title required. (c) Something else. Recommended: (a), because a 2000-character title would not fit a list
row.

**Answer:** @alokkulkarni (point 1): "title 200 and description 2000" — (a) title up to 200 characters, description up to 2000; the title stays required, as in Q6 ("title, optional description").

## Q24. Are the due date and the priority both optional on a to-do?

Follow-up to Q16 ("both"). (a) Both optional. (b) Due date optional, priority always set (with a default).
(c) Both required. Recommended: (b), so every to-do can be sorted and shown by priority.

**Answer:** @alokkulkarni (point 3): "due date is date only and priority is correct with default as medium"; and (to Q30): "no due date is not optional" — (c) both required: the due date is required (date only), and the priority is always set, with default Medium.

## Q25. Does the due date include a time of day?

Follow-up to Q16. (a) Date only. (b) Date and time (stored in UTC, shown in the user's local time zone).
Recommended: (a), because it avoids time-zone handling.

**Answer:** @alokkulkarni (point 3): "due date is date only" — (a) date only, no time of day.

## Q26. What are the priority levels and the default?

Follow-up to Q16. (a) Low / Medium / High, default Medium. (b) Low / Medium / High, no default (only if Q24 makes it
optional). (c) Other levels you name. Recommended: (a).

**Answer:** @alokkulkarni (point 3): "priority is correct with default as medium" — (a) Low / Medium / High, default Medium.

## Q27. Must a new user verify their email address before using the app?

Follow-up to Q14. (a) No verification in the first release. (b) Yes, by an emailed link (this needs an email
service, even locally). Recommended: (a), because the app runs locally only (Q10).

**Answer:** @alokkulkarni (point 5, to "is it OK to have no email verification and no password reset in the first release?"): "yes" — (a) no email verification in the first release.

## Q28. Is "forgot password" (password reset) in scope?

Follow-up to Q14. (a) Not in the first release. (b) Yes, by an emailed reset link (needs an email service).
Recommended: (a), for the same reason as Q27.

**Answer:** @alokkulkarni (point 5, to "is it OK to have no email verification and no password reset in the first release?"): "yes" — (a) password reset is not in the first release.

## Q29. How many to-dos are shown per page?

Follow-up to Q21. (a) 20 per page. (b) 10 per page. (c) Another fixed number. (d) The user chooses from a few
sizes. Recommended: (a).

**Answer:** @alokkulkarni (point 4): "20" — (a) 20 per page.

## Q30. Is the due date optional on a to-do?

Follow-up to Q24: the answer settled that priority is always set (default Medium) but not whether a due date must
be given. (a) Optional: a to-do may have no due date. (b) Required on every to-do. Recommended: (a), because many
tasks have no deadline.

**Answer:** @alokkulkarni: "no due date is not optional" — (b) the due date is required on every to-do.

## Q31. What does a user sign up and log in with: an email address, a username, or either?

Raised by the critic's requirements review (N02, finding F-1): Q14 says "email (or username)", which leaves this
open. (a) Email address only: it must be a valid email format. (b) Username only: for example 3–50 characters of
letters, digits, `.`, `_` and `-`. (c) One field that may hold either an email address or a username.
(d) Both: a required email and a separate required username, and login with either. Recommended: (a), because it
gives one clear format to validate and leaves room for email verification and password reset later.

**Answer:** @alokkulkarni (point 1): "c" — (c) one identifier field that may hold either an email address or a username.

## Q32. Are identifiers unique regardless of letter case?

Raised by the critic's requirements review (N02, finding F-13 item 2). (a) Yes: `Alice@Example.com` and
`alice@example.com` are the same account and the second sign-up is refused. (b) No: they are two different accounts.
Recommended: (a), because people do not expect letter case to matter in an email address or username.

**Answer:** @alokkulkarni (point 2): "yes, cases insensitive" — (a) identifiers are unique regardless of letter case.

## Q33. Do you accept the items the first requirements draft marked "(proposed)"?

Raised by the requirements draft (N02, round 1), relayed as one item. The items: password at least 8 characters;
a login failure message that does not reveal whether the identifier exists; a logout control and an 8-hour session;
past due dates accepted; "clear all completed" acts on all of the user's completed to-dos regardless of filter,
search or page, and its confirmation shows the count; search treats `%`, `_` and quotes literally, and
whitespace-only search counts as empty; changing the filter or search resets to page 1, and a page past the last one
shows the last valid page; the database file is created on first start; 95% of actions within 1 second for up to
500 to-dos. (a) Accept all. (b) Change some (say which).

**Answer:** @alokkulkarni (point 3): "accept all of them" — (a) all of the listed items are accepted.

## Q34. How should the three ambiguous acceptance criteria resolve?

Raised by the critic's requirements review (N02, findings F-2 to F-4), relayed as one item: another user's to-do
requested by identifier returns "not found" (REQ-ISO-001 AC-2); "clear all completed" is disabled when nothing is
completed (REQ-TODO-007 AC-4); a page number past the last page shows the last valid page (REQ-LIST-003 AC-6).
(a) Yes to all. (b) Change some (say which).

**Answer:** @alokkulkarni (point 4): "yes to all. cross user todos should not be acessible." — (a) all three as listed: another user's to-do is not found (and not accessible), the clear-completed control is disabled when nothing is completed, and an out-of-range page shows the last valid page.

## Q35. Do you accept the choices listed for the approver to confirm at freeze?

Raised at the spec-freeze review (PR #3), relayed by the coordinator: the requirements' "Analyst defaults" section;
SDD §12.1 "For the approver to confirm at freeze" (ASCII-only email rule, 8-hour session counted from login, the
analyst defaults, the trimmed whitespace set); the spec critic's F-11 (after login the user returns to the to-do page
first opened) and F-12 (a reload after the session expired shows the plain login page, with no "session ended"
message); and the three wording points left open by the requirements critic (round 2). (a) Accept all, no spec
edits. (b) Change some (say which). The freeze approval itself is the requester's Comment review "Approved" on PR #3
at commit `ebd6cea`, recorded with `harness gate record --human --pr 3`; this answer records only the chat reply.

**Answer:** @alokkulkarni (in chat, relayed by the coordinator): "accept all and approved pull request" — (a) all listed choices are accepted, with no spec edits.

## Q36. Do you accept the essential-timeout exception for the fixed 8-hour session, or want a warning before expiry?

Raised by the UX design (N06, Q-UX-1; REQ-AUTH-002 AC-4, REQ-AUTH-004 AC-4, NFR-001 AC-1), relayed by the
coordinator: the fixed 8-hour session ends with no warning or extension, and typed form input is lost when it
expires. Under WCAG 2.2.1 Timing Adjustable this passes only under the "essential" timeout exception. (a) Accept the
exception in writing. (b) Require a warning before expiry (needs a re-freeze).

**Answer:** @alokkulkarni (point 1): "accept the exception." — (a) the essential-timeout exception is accepted. Not
yet written into the frozen spec; that needs a re-freeze.

## Q37. What happens at `/` and at unknown routes?

Raised by the UX design (N06, Q-UX-7; sdd §8, REQ-AUTH-004 AC-2), relayed by the coordinator: the spec lists only six
routes and says nothing about `/` or unknown routes, so neither was designed. Suggested: `/` goes to `/todos`, plus a
"not found" view. Either choice needs a spec change.

**Answer:** @alokkulkarni (point 2): "for / send as u suggested , not found, create an animated notfound page and link
to go back to home." — `/` goes to `/todos`; any unknown route shows an animated "not found" page with a link back to
home. This adds a screen and behaviour with no requirement ID yet, so it needs a re-freeze (not done).

## Q38. Should the clear-completed confirmation use a singular form when one to-do is completed?

Raised by the UX design (N06, Q-UX-6; REQ-TODO-007 AC-1), relayed by the coordinator: the spec sentence is used word
for word, so one item reads "Delete all 1 completed to-dos?". (a) Approve a singular form. (b) Keep the exact
wording.

**Answer:** @alokkulkarni (point 3): "approve as singular form" — (a) a singular form is approved for N = 1. The exact
singular sentence is not yet fixed. It changes a frozen acceptance criterion's wording, so it needs a re-freeze (not
done).

## Q39. Do you approve the message wording list in the UX design §4?

Raised by the UX design (N06, Q-UX-2; REQ-AUTH-001 AC-2..AC-11, REQ-TODO-002 AC-10, NFR-001 AC-3), relayed by the
coordinator: the contract fixes very few message strings. (a) Approve the wording list in
`docs/specs/CHG-0001/design/ux/ux-design.md` §4, which the server and web app must then share. (b) Provide the
wording.

**Answer:** @alokkulkarni (point 4): "approved for the wording. make sure web shows user frendly errors / exceptions
and next steps." — (a) the §4 wording list is approved. The requester also asks that the web app show user-friendly
errors and exceptions with next steps. That second part is a new non-functional requirement and needs a re-freeze
(not done).

## Q40. Should Delete and Edit appear on every row and on the detail page?

Raised by the UX design (N06, Q-UX-3; REQ-TODO-006 AC-1, REQ-TODO-004), relayed by the coordinator: the spec says
"choose Delete" but not on which screen. As designed: Delete and Edit on every row and on the detail page; after a
delete on the detail page the user goes to `/todos`. (a) Approve. (b) Change it.

**Answer:** @alokkulkarni (point 5): "yes" — (a) approved as designed.

## Q41. After saving an edit, should the user land on `/todos/:id`?

Raised by the UX design (N06, Q-UX-4; REQ-TODO-004 AC-1), relayed by the coordinator: the destination after a
successful edit is not specified. As designed: the to-do's detail page, `/todos/:id`. (a) Approve. (b) Change it.

**Answer:** @alokkulkarni (point 6): "yes" — (a) approved as designed.

## Q42. Should generic failures show an error alert with a Retry button?

Raised by the UX design (N06, Q-UX-5; NFR-004 AC-2, REQ-TODO-003), relayed by the coordinator: the UI for network
errors, 403 responses and load failures is not specified beyond "never show internals". As designed: generic error
alerts with a Retry button, using the copy in §4. (a) Approve. (b) Change it.

**Answer:** @alokkulkarni (point 7): "yes" — (a) approved as designed.

## Q43. If the refresh finds nothing completed to clear, should the button be disabled with no dialog?

Raised by the UX design (N06, Q-UX-9; REQ-TODO-007 AC-4), relayed by the coordinator: when "clear all completed"
re-fetches and finds no completed to-dos (they were cleared elsewhere), no acceptance criterion says what happens. As
designed: no dialog opens, the button is disabled, and a status message is shown. (a) Approve. (b) Change it.

**Answer:** @alokkulkarni (point 8): "yes" — (a) approved as designed.

## Q44. Which WCAG level should the accessibility audit cite: 2.1 AA (the spec) or 2.2 AA (the design)?

Raised by the UX design (N06, Q-UX-8; NFR-001 AC-1), relayed by the coordinator: NFR-001 says WCAG 2.1 AA; the
2.2 AA target came from the ux-designer's role and the coordinator's brief, not from the spec. The design meets
both. The UX design phrased it as "confirm 2.2 is acceptable as the audit target".

**Answer:** @alokkulkarni (point 9): "confirmed." — The reply does not name a level. Read against the UX design's
wording, it confirms that 2.2 AA is acceptable as the audit target; the coordinator's later summary read it as 2.1 AA.
Follow-up answer: @alokkulkarni (in chat): "2.1 AA" — the audit cites WCAG 2.1 AA, as NFR-001 says.

## Q45. Should the spec-author agent draft the spec changes for Q36–Q39, or will you edit the spec yourself?

Raised by the coordinator after Q36–Q44: the answers to Q36, Q37, Q38 and the second part of Q39 change the frozen
spec, so they need `harness freeze --supersede`, approved by the requester. (a) The spec-author agent drafts the
changes for the requester to review. (b) The requester edits the spec in their own session. Either way the approval
and freeze stay with the requester.

**Answer:** @alokkulkarni (in chat): "not sure which specs.. but go wiht your view." — left to the coordinator, who
chose (a): the spec-author agent drafts the changes (in `requirements.md`, `sdd.md` and, if needed, the contract)
for the requester to review; the approval and the freeze stay with the requester.

## Q46. Does the not-found page's home link go to `/`, which then sends the user on to `/todos`?

Raised by the critic's round-3 review of the supersede draft (N-4; REQ-NAV-001), relayed by the coordinator: Q37
says "a link to go back to home" but not what "home" is. The draft reads it as `/`, which redirects to `/todos`.
(a) Yes, keep that reading. (b) Another destination.

**Answer:** @alokkulkarni (relayed by the coordinator, decision 1): "yes thats correct" — (a) the home link goes to
`/`, which then sends the user on to `/todos`.

## Q47. Do signed-out visitors who open an unknown address see the not-found page directly, without being sent to login?

Raised by the critic's round-3 review of the supersede draft (N-4; REQ-NAV-001 AC-3), relayed by the coordinator: the
draft shows the not-found page to everyone, signed in or not, without a login redirect. (a) Yes, as drafted. (b) No,
signed-out visitors are sent to the login page.

**Answer:** @alokkulkarni (relayed by the coordinator, decision 2): "no they should be sent to login / signin page."
— (b) signed-out visitors opening an unknown address are sent to the login page, not shown the not-found page. Where
they land after logging in is not stated in the answer.

## Q48. On the list screen, when a to-do vanished before a done/undone toggle, is the refreshed list the next step, with no "Back to my to-dos" link?

Raised by the critic's round-3 review of the supersede draft (N-5; NFR-006 AC-1), relayed by the coordinator:
(a) the list screen also shows a "Back to my to-dos" link (which clears filter and search). (b) The refreshed list,
with "…the list has been refreshed", is the next step on the list screen; the link stays only on the detail page.
Recommended: (b).

**Answer:** @alokkulkarni (relayed by the coordinator, decision 3): "b is recommended." — (b) on the list screen the
refreshed list is the next step and no "Back to my to-dos" link is shown; the link stays only on the detail page.

## Q49. Should the critic's optional round-3 notes be folded into the supersede draft?

Raised by the critic's round-3 review of the supersede draft, relayed by the coordinator: N-1 (error page home link
is a full page load or the error boundary resets on navigation), N-2 (where errors thrown in event handlers are
shown), N-3 (a form 400 with no field it can show falls back to the try-again message), N-7 (an NFR-006 note on the
contract's `FieldError.message`, or the SDD rule stated as binding), N-8 (ADR-0001 wording "eight audited screens
(SDD §13)"), and N-6 (the `upstream.sha256` of requirements.md in sdd.md goes stale once applied). (a) Yes to all.
(b) Some (say which).

**Answer:** @alokkulkarni (relayed by the coordinator, decision 4): "yes to all" — (a) N-1, N-2, N-3, N-7 and N-8 are
applied, and N-6 is handled as far as it applies to the draft.

## Q50. Does "ai contract" in the revalidation request mean the API contract?

Raised by the requester after the N05 architecture (in chat, relayed by the coordinator). The request, verbatim:
"also now that we have the architecture, also revalidate the contract , ai contract and the spec as right now we have
one api servicng all end points, where we might need different components built as per functionality they serve."
The change has one interface file, `docs/specs/CHG-0001/contracts/todo-api-v1.yaml`, and no AI contract. (a) The API
contract (a typo). (b) A separate AI contract.

**Answer:** @alokkulkarni (relayed by the coordinator, multiple-choice pick): "API contract (typo)" — (a) it means
`docs/specs/CHG-0001/contracts/todo-api-v1.yaml`; there is no separate AI contract.

## Q51. How far should the API be split into components by the functionality they serve?

Follow-up to Q50. (a) Contracts only: split the contract, keep the API design as it is. (b) Modular monolith: one
Spring Boot deployable split into strict modules (for example identity/auth and to-dos) with enforced boundaries (for
example Spring Modulith, or package-per-module plus ArchUnit), possibly one contract per module, still one process and
one SQLite file. (c) Separate services, each its own process.

**Answer:** @alokkulkarni (relayed by the coordinator, multiple-choice pick): "Modular monolith" — (b) one Spring Boot
deployable split into strict modules with enforced boundaries, possibly one contract per module, still one process and
one SQLite file; not separate services (c) and not a contracts-only split (a). The answer leaves open whether the
contract is physically split into one file per module.

## Q52. Is the split drafted as its own supersede, or folded into supersede-1?

Follow-up to Q51. (a) A separate supersede-2, drafted against the re-frozen spec (supersede-1, frozen at `2a0d38e`).
(b) Reopen supersede-1.

**Answer:** @alokkulkarni (relayed by the coordinator, multiple-choice pick): "Separate supersede-2" — (a) a separate
supersede-2, drafted against the re-frozen spec.

## Q53. Does the edit page keep its "Back to my to-dos" link (SDD §12.1 item 7(d))?

Raised by the critic in its pre-re-freeze review of N03-spec (finding F-2; NFR-006; `docs/specs/CHG-0001/sdd.md`
L565-L568): Q48 keeps the link "only on the detail page", and the frozen SDD reads that as removing it from the list
screen only, so the edit page keeps it in its to-do-not-found state. (a) Keep it on the edit page too, as frozen.
(b) Remove it from the edit page (changes two UX design states).

**Answer:** @alokkulkarni (relayed by the coordinator, multiple-choice pick): "Keep on edit page too" — (a) the edit
page keeps "Back to my to-dos" in its to-do-not-found state, as frozen. An explicit confirmation with no spec change;
it closes F-2 for item 7(d).

## Q54. Do you confirm SDD §12.1 items 7(a), 7(b), 7(c), 9 and 10 as written?

Raised by the critic in its pre-re-freeze review of N03-spec (finding F-2; NFR-006, REQ-NAV-001;
`docs/specs/CHG-0001/sdd.md` L555-L586): choices the requester did not make. 7(a) the sentence "That to-do no longer
exists, and the list has been refreshed.", shown only when the re-query after the 404 succeeds; 7(b) the same
list-screen rule for a 404 on a Delete from a list row; 7(c) the refused-login alert "Invalid identifier or password.
Check them and try again."; 9 after login from an unknown address the user lands on `/todos` (analyst default 12);
10 the home link of the error page resets the error boundary on a pathname change. (a) Accept all, no spec change.
(b) Change some (say which).

**Answer:** @alokkulkarni (relayed by the coordinator, multiple-choice pick): "Accept all" — (a) items 7(a), 7(b),
7(c), 9 and 10 are confirmed as written, an explicit confirmation like Q35, with no spec change. With Q53 this closes
F-2.

## Q55. Should the critic findings F-1 and F-3 be folded into supersede-2?

Raised by the critic in its pre-re-freeze review of N03-spec
(`.harness/changes/CHG-0001/verdicts/N03-spec.critic-verdict.json`): F-1, `sdd.md` still calls itself "the draft"
or "this supersede" (about L306, L323, L567, L577, L582); F-3, the clear-completed rule says "For any N other than 1"
(about L462-L469), which includes N = 0, while Q43 says no dialog opens when nothing is completed (REQ-TODO-007).
(a) Fold both into supersede-2: write "this SDD", reword L306, write "N of 2 or more", state the N = 0 case and add a
§14 component test for it. (b) Leave them.

**Answer:** @alokkulkarni (relayed by the coordinator, multiple-choice pick): "Fold into supersede-2" — (a) F-1 and
F-3 are fixed in supersede-2 as described.

## Q56. Should the API contract be split into one file per module?

Raised by the supersede-2 revalidation (spec-author and drawio-architect, read-only, 2026-11-06; Q51 leaves this
open; NFR-004, NFR-007 proposed). The contract already tags its operations `auth` and `todos`
(`docs/specs/CHG-0001/contracts/todo-api-v1.yaml` L41-L43). (a) Keep one file, `todo-api-v1.yaml`: each operation
gets an `x-module: identity|todos` marker, `info.version` goes to 1.0.1 with no wire change, and the endpoint-inventory
contract test (ADR-0012) also checks that each operation is served by a controller of its module; the generated
`web/src/api/schema.ts` and the validator set-up stay as they are. (b) One file per module, `identity-api-v1.yaml` and
`todos-api-v1.yaml`, plus a shared `common-problem-v1.yaml` for `Problem`, `FieldError`, the error responses, the
client header and the session cookie, referenced by `$ref`: the `ProblemCode` list stays one closed list in the shared
file, versions stay coupled through it, six frozen places that name the file change (SDD, ADR-0001, ADR-0012), and two
tool behaviours (the request validator and the TypeScript generator reading `$ref` across files) must be checked
before the freeze. Recommended: (a), because one web app calls both modules of one deployable at one version, so a
second file adds tooling risk and gains nothing.

**Answer:** @alokkulkarni (relayed by the coordinator; one reply to Q56-Q59 in order, verbatim: "1 - b\n2 - accept\n3 -
a\n4 - a"), point 1: "b" — (b) split the contract into `identity-api-v1.yaml`, `todos-api-v1.yaml` and a shared
`common-problem-v1.yaml` linked by `$ref` (not the recommended (a)).

## Q57. Do you accept a new requirement NFR-007 "Modular API", worded as below?

Raised by the supersede-2 revalidation. Without a requirement, the module design and its test map to no requirement
ID (CLAUDE.md rule 5); NFR-005 covers the stack, not the structure. Proposed wording (analyst default):
"NFR-007 Modular API (M). The API is one deployable split into modules by the functionality they serve, with
boundaries an automated test enforces. Source: clarifications Q51. AC-1: Given the delivered API, when it is built and
started, then it is one Spring Boot application in one process using one SQLite file and one connection pool.
AC-2: Given the API source, when the build runs its tests, then a test fails if code in one module uses a type another
module does not publish as its interface, or if module dependencies form a cycle. AC-3: Given the API, when its
modules are listed, then there is an identity module (sign-up, login, logout, session) and a to-dos module (to-do and
list operations), and every contract operation belongs to exactly one of them." A constraint C-2 "modular monolith,
not separate services (Q51)" would be added beside C-1. (a) Accept as worded. (b) Change it (say how).
Recommended: (a).

**Answer:** @alokkulkarni (relayed by the coordinator), point 2 of the same reply: "accept" — (a) NFR-007 "Modular
API" with AC-1 to AC-3 as worded, and constraint C-2, are accepted.

## Q58. Which tool enforces the module boundaries?

Raised by the supersede-2 revalidation (NFR-007 AC-2 proposed; a new ADR-0018 either way). Both are Apache-2.0 and
add a dependency under C-1. (a) ArchUnit: a test-only library with hand-written rules (no cycles; to-dos may use only
its own code, the shared kernel and the published identity type; identity never uses to-dos; only the shared
database configuration may create a connection pool), plus package-private classes; no runtime effect.
(b) Spring Modulith: `ApplicationModules.verify()` in a test checks cycles and access to module internals out of the
box; its version follows the Spring Boot line, its test-support classes must be excluded from module detection, and
its event-publication starter (extra tables outside Flyway) is not used. The two reviewers differed: drawio-architect
recommends (a), spec-author recommends (b). Recommended: (a), because two modules and a small shared kernel do not
need a runtime framework, and a test-only library leaves the running app unchanged; (b) is the better pick if you
want module documentation and Spring-native module tests.

**Answer:** @alokkulkarni (relayed by the coordinator), point 3 of the same reply: "a" — (a) ArchUnit enforces the
module boundaries.

## Q59. Should SDD §12.1 items 7 and 9-10 now cite your confirmations in Q53 and Q54?

Raised by the supersede-2 revalidation (spec-author; NFR-006, REQ-NAV-001). Q53 and Q54 confirmed those items "with no
spec change", so `docs/specs/CHG-0001/sdd.md` L557-L558 still says "Still for the approver, because the requester's
words do not settle them" and L586 "The choice between the two is the spec author's". (a) In supersede-2, change only
those sentences to say the items were confirmed in Q53 and Q54 (no behaviour change). (b) Leave the text as frozen.
Recommended: (a), so the frozen SDD does not call settled choices open.

**Answer:** @alokkulkarni (relayed by the coordinator), point 4 of the same reply: "a" — (a) in supersede-2, SDD §12.1
cites the confirmations in Q53 and Q54 (no behaviour change).

## Q60. Does the API target the latest Spring Boot 4.x line?

Raised by the critic in round 3 of the supersede-2 draft review (finding F-3; NFR-005, NFR-007): the request
validator's v3 MockMvc adapter is documented as compatible with Spring 7+ / Spring Boot 4+ (ADR-0012 draft), while the
frozen ADR-0002 says only "the current OSS-supported Spring Boot major line", so the two pinning rules could diverge.
(a) Accept that the backend targets Spring Boot 4.x, because of the validator v3 adapter. (b) Another line.

**Answer:** @alokkulkarni (relayed by the coordinator): "Yes boot 4.x latest of springboot"; later, to the numbered
questions, "1 a" — (a) the backend targets the latest Spring Boot 4.x release.

## Q61. Should the open round-3 minors be fixed in one more spec-author pass before the draft is applied?

Raised by the critic in round 3 of the supersede-2 draft review (findings F-1 to F-3, minor). (a) Apply the draft at
`b8037ee` as reviewed and let the post-apply critic raise them again. (b) One more spec-author pass first, then a fresh
critic review.

**Answer:** @alokkulkarni (relayed by the coordinator), to the numbered questions: "2 b" — (b) one more spec-author
pass on the open round-3 minors before applying.

## Q62. Which round-3 findings does that pass fix?

Follow-up to Q61 (multiple-choice question from the coordinator): the Spring Boot 4.x wording (F-3, Q60), F-1 (the
request id and status lost from the log line when a filter throws), F-2 ("outermost filter" wording) and F-6 (cite
`tool-spikes.md` by path, not by commit).

**Answer:** @alokkulkarni (relayed by the coordinator, multiple-choice pick): "Fix all in one pass" — the Boot 4.x
wording plus round-3 F-1, F-2 and F-6, in one pass.
