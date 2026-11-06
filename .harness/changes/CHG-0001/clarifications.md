# Clarifications — CHG-0001

Change: "Create a to-do list". Source: `docs/specs/CHG-0001/intent.md` (title only; the Problem, Who has it,
Outcome wanted, Out of scope and How success is observed sections are empty). The repository has no product code
yet (`.harness/config.yaml` has `stacks: []`; `.harness/platform.yaml` lists the repo stack as `other`).

## Q1. Is this change a to-do list app, as the intent file says?

The intent file's title and body describe "a new todo list app where user can create, list, delete and update
todo's", but the commit that added it (`68b3f47`) is titled "customers can save payment methods". (a) It is the
to-do list app only; the commit title is a mistake. (b) Something else, or payment methods are also in scope.
Recommended: (a), because the file body and the change title both say to-do list. Please also say, in a sentence
each if you can, who uses it and what outcome you want, since those intent sections are empty.

**Answer:** @alokkulkarni: "yes todo app only" — (a) the to-do list app only. (Who uses it and the outcome wanted are still open: Q11.)

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

**Answer:** @alokkulkarni: "a" — (a) React + TypeScript UI with a Java/Spring Boot API. (Adding the engineer role on `develop` is still open: Q12.)

## Q4. Where are to-dos stored, and must they survive a restart?

(a) In memory only (lost on restart). (b) An embedded file database such as SQLite or H2 file mode. (c) A server
database such as PostgreSQL. (d) Browser local storage (only with Q2 (c)). Recommended: (b), because data survives a
restart without needing a database server to run or deploy.

**Answer:** @alokkulkarni: "b" — (b) an embedded file database. (Which one, SQLite or H2, is still open: Q13.)

## Q5. Are there user accounts, or is it a single shared list?

(a) One list, no login (a single anonymous user). (b) Each user signs in and sees only their own to-dos.
Recommended: (a) for the first release, because the intent mentions no accounts, and (b) adds authentication,
password storage and permissions.

**Answer:** @alokkulkarni: "b" — (b) each user signs in and sees only their own to-dos. (Sign-up and login, and an admin role, are still open: Q14, Q15.)

## Q6. What fields does a to-do have?

(a) Title and done/not-done status only. (b) Title, optional description, done status, created and updated
timestamps. (c) As (b) plus a due date and/or priority. Recommended: (b). Please also give the title's limits:
required, trimmed, and a maximum length (suggested 200 characters); and the description's maximum (suggested 2000).

**Answer:** @alokkulkarni: "c" — (c) title, optional description, done status, created and updated timestamps, plus a due date and/or priority. (Which of due date and priority, and the length limits, are still open: Q16, Q17.)

## Q7. What does "update" cover?

(a) Editing the title/description and marking done or not done. (b) Only marking done or not done. (c) Only editing
the text. Recommended: (a). Also, if two edits to the same to-do arrive at once, is "last write wins" acceptable
(recommended), or must the second be rejected as a conflict?

**Answer:** @alokkulkarni: "a" — (a) editing the title/description and marking done or not done. (Concurrent edits are still open: Q18.)

## Q8. How does delete behave?

(a) Permanent delete, with a confirmation prompt in the UI. (b) Permanent delete, no prompt. (c) Soft delete
(hidden, recoverable or with undo). Also: should there be a "clear all completed" action? Recommended: (a), with no
"clear completed" in the first release.

**Answer:** @alokkulkarni: "a" — (a) permanent delete, with a confirmation prompt in the UI. ("Clear completed" is still open: Q19.)

## Q9. How is the list shown?

(a) All to-dos, newest first, no filtering. (b) As (a) plus filters for All / Active / Completed. (c) As (b) plus
search and pagination. Recommended: (b), with no pagination and an empty-state message when there are no to-dos.
If you expect many to-dos (more than a few hundred), say so and we will add pagination.

**Answer:** @alokkulkarni: "c" — (c) all to-dos newest first, filters for All / Active / Completed, plus search and pagination. (What search covers and how pagination works are still open: Q20, Q21.)

## Q10. What non-functional bar applies, and how is the app run?

(a) Runs locally only (developer machine), WCAG 2.1 AA accessibility for the UI, no performance target beyond
"feels instant" for a few hundred items. (b) As (a), plus it must deploy to the `dev`/`test` environments (please
name the hosting, e.g. a container platform). Recommended: (a) for the first release; the repository has no
deployment workflows yet. Please also say how you will judge success (for example: "I can add, see, edit, complete
and delete a to-do, and it is still there after a restart").

**Answer:** @alokkulkarni: "a" — (a) runs locally only, WCAG 2.1 AA for the UI, no performance target beyond "feels instant" for a few hundred items. (The success measure is still open: Q22.)

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

**Answer:** @alokkulkarni: "a" — (a) self sign-up with email (or username) and password, then a session or token login. (Email verification and password reset are still open: Q27, Q28.)

## Q15. Is there an admin role?

Follow-up to Q5. (a) No admin; every user sees and manages only their own to-dos. (b) An admin can list and
disable users (but not read their to-dos). (c) An admin can also see all to-dos. Recommended: (a) for the first
release.

**Answer:** @alokkulkarni: "a" — (a) no admin; every user sees and manages only their own to-dos.

## Q16. Which extra fields: due date, priority, or both?

Follow-up to Q6. (a) Due date only (date, no time). (b) Priority only (Low / Medium / High). (c) Both. Both are
optional on a to-do. Recommended: (c), with due date as a date without time and priority Low / Medium / High,
default Medium.

**Answer:** @alokkulkarni: "both" — (c) both a due date and a priority. (Whether they are optional, whether the due date has a time, and the priority levels and default are still open: Q24, Q25, Q26.)

## Q17. What are the length limits for title and description?

Follow-up to Q6. Recommended: title required, trimmed, 1–200 characters; description optional, up to 2000
characters. Please confirm or give other limits.

**Answer:** @alokkulkarni: "2000" (What the 2000 applies to, and whether the title is still required, are still open: Q23.)

## Q18. When two edits to the same to-do arrive at once, is "last write wins" acceptable?

Follow-up to Q7. (a) Last write wins. (b) The second edit is rejected as a conflict and the user reloads.
Recommended: (a), because each list belongs to one user (Q5), so simultaneous edits are rare.

**Answer:**

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

**Answer:** @alokkulkarni: "a" — (a) numbered pages with previous/next. (The page size is still open: Q29.)

## Q22. How will you judge that the change succeeded?

Follow-up to Q10. Recommended: "A signed-in user can add, see, search, filter, edit, complete and delete their own
to-dos; another user cannot see them; and the to-dos are still there after a restart."

**Answer:** @alokkulkarni: "correct as mentioned" — accepts the proposed statement: "A signed-in user can add, see, search, filter, edit, complete and delete their own to-dos; another user cannot see them; and the to-dos are still there after a restart."

## Q23. Does the 2000-character limit apply to the title, the description, or both, and is the title still required?

Follow-up to Q17 ("2000"). (a) Description up to 2000; title required, up to 200. (b) Both title and description up
to 2000; title required. (c) Something else. Recommended: (a), because a 2000-character title would not fit a list
row.

**Answer:**

## Q24. Are the due date and the priority both optional on a to-do?

Follow-up to Q16 ("both"). (a) Both optional. (b) Due date optional, priority always set (with a default).
(c) Both required. Recommended: (b), so every to-do can be sorted and shown by priority.

**Answer:**

## Q25. Does the due date include a time of day?

Follow-up to Q16. (a) Date only. (b) Date and time (stored in UTC, shown in the user's local time zone).
Recommended: (a), because it avoids time-zone handling.

**Answer:**

## Q26. What are the priority levels and the default?

Follow-up to Q16. (a) Low / Medium / High, default Medium. (b) Low / Medium / High, no default (only if Q24 makes it
optional). (c) Other levels you name. Recommended: (a).

**Answer:**

## Q27. Must a new user verify their email address before using the app?

Follow-up to Q14. (a) No verification in the first release. (b) Yes, by an emailed link (this needs an email
service, even locally). Recommended: (a), because the app runs locally only (Q10).

**Answer:**

## Q28. Is "forgot password" (password reset) in scope?

Follow-up to Q14. (a) Not in the first release. (b) Yes, by an emailed reset link (needs an email service).
Recommended: (a), for the same reason as Q27.

**Answer:**

## Q29. How many to-dos are shown per page?

Follow-up to Q21. (a) 20 per page. (b) 10 per page. (c) Another fixed number. (d) The user chooses from a few
sizes. Recommended: (a).

**Answer:**
