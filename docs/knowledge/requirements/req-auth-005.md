---
type: requirement
title: REQ-AUTH-005 No admin role
summary: The system has one role with identical permissions for every account, and no operation lists users or reads another user's to-dos.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-AUTH-005 No admin role (M)

Spec: [requirements.md, REQ-AUTH-005](../../specs/CHG-0001/requirements.md#req-auth-005-no-admin-role-m).

**Acceptance criteria in brief.** AC-1 no operation lists users or reads another user's to-dos; AC-2 every account has the
same single role.

**Implemented by.** Absence of any such operation: [identity API](../apis/identity-api.md) and [to-do API](../apis/todo-api.md)
define none; the [users table](../data/users.md) has no role column.

**Verified by.** [contract tests](../tests/contract-tests.md) (`EndpointInventoryTest`), [API integration tests](../tests/api-integration-tests.md)
(`TodoIsolationApiTest`, `PasswordStorageTest`).
