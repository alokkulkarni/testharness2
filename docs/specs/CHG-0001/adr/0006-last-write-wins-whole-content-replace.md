# ADR-0006 Last write wins: whole-content replace, with done status set separately

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-DATA-002, REQ-TODO-004, REQ-TODO-005.

## Context

Concurrent edits to the same to-do resolve as last write wins with no conflict error (Q18, REQ-DATA-002 AC-1). The
requirements do not say whether a write replaces the whole record or only the fields that changed (critic
finding 11). Editing covers title, description, due date and priority (REQ-TODO-004); marking done or not done is
a separate, frequent action from the list (REQ-TODO-005).

## Decision

- `PUT /api/v1/todos/{todoId}` replaces the **whole content** of the to-do: `title`, `description`, `dueDate` and
  `priority` are all taken from the request (`priority` required; an absent or blank `description` clears it). It
  never changes `done`.
- `PUT /api/v1/todos/{todoId}/done` sets only `done`.
- Neither carries a version; each is one `UPDATE ... WHERE public_id = ? AND user_id = ?`, so the later write's
  values are stored for the fields that operation owns. No conflict is ever reported.
- `updatedAt` is set on every accepted write to the later of "now" and "previous `updatedAt` + 1 ms", so it always
  advances (REQ-TODO-004 AC-1, REQ-TODO-005 AC-1, AC-2) even when two writes land in the same millisecond.

## Consequences

- REQ-DATA-002 AC-1 is testable without ambiguity: after two content writes the stored content equals the second.
- Toggling done in one tab does not undo a title edit saved in another tab, and the reverse.
- An edit form left open in a stale tab overwrites all content fields when saved; this is what last write wins means
  and is accepted by Q18.

## Alternatives

- `PATCH` with only changed fields (JSON Merge Patch): merges concurrent edits field by field, but makes "absent"
  versus "cleared" ambiguous for the description and gives a less predictable last-write outcome.
- One `PUT` that also carries `done`: a toggle from a list loaded earlier could silently revert another tab's edit.
- Optimistic locking with a version or `If-Match`: out of scope (Q18).
