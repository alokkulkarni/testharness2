---
type: rule
title: Return-to after login and not-found routing
summary: After login the user goes only to the app's own to-do routes named in returnTo, otherwise to /todos, and an undefined address shows the not-found page only to a signed-in user.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - web/src/auth/returnTo.ts
  - web/src/app/AppRoutes.tsx
  - docs/specs/CHG-0001/sdd.md
---

# Return-to after login and not-found routing

**`safeReturnTo`** (`web/src/auth/returnTo.ts`). The decoded `returnTo` is honoured only if it matches the to-do route pattern, optionally with
a query string: `/todos`, `/todos/new`, `/todos/<uuid>` and `/todos/<uuid>/edit`. Anything else, including absent or empty values,
`//host`, `https://…`, `/\…`, `javascript:…`, `/login`, `/signup`, a non-UUID id and undefined addresses such as `/nope`, gives `/todos`
([sdd.md section 4.2](../../specs/CHG-0001/sdd.md#42-returnto-after-login-req-auth-004-ac-2-req-auth-002-ac-1)).

**Routes** ([sdd.md section 8](../../specs/CHG-0001/sdd.md#8-spa-behaviour)):
- `/` is replaced by `/todos` with the query not carried ([REQ-NAV-001](../requirements/req-nav-001.md) AC-1).
- A signed-out visitor to any to-do route, or to an undefined address, is sent to `/login?returnTo=<path>` with no message. After login they land on `/todos` for undefined addresses (analyst default; [sdd.md section 12.1 item 9](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)).
- A signed-in user on an undefined address sees the [not-found page](../screens/not-found.md). The catch-all sits inside `RequireAuth`, so it waits for the session probe first.
- `/todos/<id>` and `/todos/<id>/edit` never reach the not-found page. An id that is not a UUID is checked in the SPA (`isTodoId`) and shows the to-do not-found state without an API call (analyst default; [sdd.md section 12.1 item 6](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)).

Requirements: [REQ-NAV-001](../requirements/req-nav-001.md), [REQ-AUTH-004](../requirements/req-auth-004.md) AC-2.
