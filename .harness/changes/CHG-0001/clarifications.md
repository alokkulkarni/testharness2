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

**Answer:**

## Q2. What shape should the app take?

(a) A web UI backed by an HTTP API. (b) An HTTP API only (no UI). (c) A browser-only UI that stores data locally
(no backend). (d) A command-line tool. Recommended: (a), because "user can create, list, delete and update" suggests
a person using a screen, and an API keeps the data behind a contract the tests can check.

**Answer:**

## Q3. Which technology stack should it be built in?

The harness supports React/TypeScript for web UI, and Java/Spring Boot or C#/.NET for a backend. (a) React +
TypeScript UI with a Java/Spring Boot API. (b) React + TypeScript UI with a .NET API. (c) React + TypeScript only
(if Q2 is (c)). (d) Another stack you name. Recommended: (a) if Q2 is (a). Note: the squad roster read from
`develop` (`harness squad status --json`) has no coder role today (only lead, analyst, tester, documenter and
reviewer), so whichever stack you choose, a person will need to add the matching engineer role to the squad on
`develop` before the build phase can run. Can you confirm you will do that, or should the build phase wait?

**Answer:**

## Q4. Where are to-dos stored, and must they survive a restart?

(a) In memory only (lost on restart). (b) An embedded file database such as SQLite or H2 file mode. (c) A server
database such as PostgreSQL. (d) Browser local storage (only with Q2 (c)). Recommended: (b), because data survives a
restart without needing a database server to run or deploy.

**Answer:**

## Q5. Are there user accounts, or is it a single shared list?

(a) One list, no login (a single anonymous user). (b) Each user signs in and sees only their own to-dos.
Recommended: (a) for the first release, because the intent mentions no accounts, and (b) adds authentication,
password storage and permissions.

**Answer:**

## Q6. What fields does a to-do have?

(a) Title and done/not-done status only. (b) Title, optional description, done status, created and updated
timestamps. (c) As (b) plus a due date and/or priority. Recommended: (b). Please also give the title's limits:
required, trimmed, and a maximum length (suggested 200 characters); and the description's maximum (suggested 2000).

**Answer:**

## Q7. What does "update" cover?

(a) Editing the title/description and marking done or not done. (b) Only marking done or not done. (c) Only editing
the text. Recommended: (a). Also, if two edits to the same to-do arrive at once, is "last write wins" acceptable
(recommended), or must the second be rejected as a conflict?

**Answer:**

## Q8. How does delete behave?

(a) Permanent delete, with a confirmation prompt in the UI. (b) Permanent delete, no prompt. (c) Soft delete
(hidden, recoverable or with undo). Also: should there be a "clear all completed" action? Recommended: (a), with no
"clear completed" in the first release.

**Answer:**

## Q9. How is the list shown?

(a) All to-dos, newest first, no filtering. (b) As (a) plus filters for All / Active / Completed. (c) As (b) plus
search and pagination. Recommended: (b), with no pagination and an empty-state message when there are no to-dos.
If you expect many to-dos (more than a few hundred), say so and we will add pagination.

**Answer:**

## Q10. What non-functional bar applies, and how is the app run?

(a) Runs locally only (developer machine), WCAG 2.1 AA accessibility for the UI, no performance target beyond
"feels instant" for a few hundred items. (b) As (a), plus it must deploy to the `dev`/`test` environments (please
name the hosting, e.g. a container platform). Recommended: (a) for the first release; the repository has no
deployment workflows yet. Please also say how you will judge success (for example: "I can add, see, edit, complete
and delete a to-do, and it is still there after a restart").

**Answer:**
