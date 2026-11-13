---
type: rule
title: Whitespace trimming
summary: Leading and trailing whitespace is removed using the 25 Unicode White_Space code points, identically in the API and the SPA, and zero-width characters are kept.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - api/src/main/java/app/todo/common/Whitespace.java
  - web/src/validation/whitespace.ts
  - docs/specs/CHG-0001/sdd.md
---

# Whitespace trimming

The set is the 25 code points of Unicode `White_Space`: U+0009 to U+000D, U+0020, U+0085, U+00A0, U+1680, U+2000 to U+200A,
U+2028, U+2029, U+202F, U+205F and U+3000. Java `strip()`/`trim()` and JavaScript `trim()` are not used, because each differs
from this set. Zero-width characters (U+200B to U+200D, U+2060, U+FEFF) are not whitespace and are kept
([sdd.md section 5](../../specs/CHG-0001/sdd.md#5-validation-server-authoritative-spa-mirrors-nfr-004-ac-2-adr-0010)).

Applies to the identifier, the title and the search text. A title or search made only of these characters is empty.
The description is stored as NULL when it is blank by this test.

Implemented in `api/src/main/java/app/todo/common/Whitespace.java` and `web/src/validation/whitespace.ts`. Both run the same table
of code points in their unit tests. Requirements: [REQ-TODO-002](../requirements/req-todo-002.md) AC-1, [REQ-LIST-002](../requirements/req-list-002.md) AC-7,
[REQ-AUTH-001](../requirements/req-auth-001.md) AC-8.
