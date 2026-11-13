---
type: rule
title: Identifier rules (sign-up and login)
summary: An identifier is trimmed to 3 to 254 characters; one containing "@" must be an ASCII email address, and any other must be a username of letters, digits, dot, underscore and hyphen; uniqueness ignores letter case.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/sdd.md
  - api/src/main/java/app/todo/auth/IdentifierRules.java
  - docs/specs/CHG-0001/requirements.md
---

# Identifier rules

Applies to: [REQ-AUTH-001](../requirements/req-auth-001.md) (sign-up) and [REQ-AUTH-002](../requirements/req-auth-002.md) (login trims the same way, with no format check).

1. Trim with the [whitespace set](whitespace-trimming.md). The trimmed length is 3 to 254 characters.
2. If it contains "@": it must be an email address. The local part is 1 to 64 ASCII characters from the listed set, with no leading or trailing dot and no "..". The domain is 1 to 253 characters with at least two labels, each 1 to 63 characters, and the last label is letters only and at least 2 long. Quoted local parts, IP literals and non-ASCII addresses are refused.
3. Otherwise it is a username of `A-Z a-z 0-9 . _ -` only.
4. Uniqueness uses the lower-cased identifier (`users.identifier_key`, [users](../data/users.md)).

Source: [sdd.md section 5](../../specs/CHG-0001/sdd.md#5-validation-server-authoritative-spa-mirrors-nfr-004-ac-2-adr-0010) and
[section 12.1 items 1 and 4](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze). Items 1 to 3 are
analyst defaults ([requirements.md, analyst defaults](../../specs/CHG-0001/requirements.md#analyst-defaults-for-the-approver-to-confirm-at-freeze)).
Implemented in [identity module](../components/identity-module.md) (`IdentifierRules`).
