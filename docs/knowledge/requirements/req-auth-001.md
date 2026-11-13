---
type: requirement
title: REQ-AUTH-001 Self sign-up
summary: A new person creates an account with one identifier, an email address or a username, and a password, with no email verification step.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/requirements.md
---

# REQ-AUTH-001 Self sign-up (M)

Spec: [requirements.md, REQ-AUTH-001](../../specs/CHG-0001/requirements.md#req-auth-001-self-sign-up-m) (AC-1 to AC-11).

**Acceptance criteria in brief.** AC-1 account created and loggable; AC-2 taken identifier refused; AC-3 empty fields refused;
AC-4 no verification step; AC-5 password under 8 refused; AC-6 case-insensitive duplicate; AC-7 one identifier field;
AC-8 trimmed length 3 to 254; AC-9 "@" means a valid email; AC-10 otherwise a username of `A-Z a-z 0-9 . _ -`; AC-11 password up to 128.

**Implemented by.** [identity module](../components/identity-module.md) (`AuthService`, `IdentifierRules`, `PasswordRules`),
[identity API](../apis/identity-api.md) `signUp`, [web SPA](../components/web-spa.md) sign-up screen [sign up](../screens/sign-up.md).

**Rules.** [identifier rules](../rules/identifier-rules.md), [password rules](../rules/password-rules.md), [whitespace trimming](../rules/whitespace-trimming.md).

**Verified by.** [API integration tests](../tests/api-integration-tests.md) (`SignUpApiTest`), [web component tests](../tests/web-component-tests.md) (`auth.test.tsx`),
[e2e tests](../tests/e2e-tests.md) (`auth.spec.ts`), [web unit tests](../tests/web-unit-tests.md) (`whitespace.test.ts`).
AC-level mapping: `.harness/changes/CHG-0001/traceability.json`.

**Decisions.** [ADR-0005](../decisions/adr-0005-argon2id-password-hashing.md), [ADR-0015](../decisions/adr-0015-derived-lowercase-columns.md).
