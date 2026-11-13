---
type: decision
title: ADR-0008 Absolute 8-hour session lifetime
summary: A session lasts 8 hours from login, is not extended by activity, and ends with no warning, which the requester accepted under WCAG 2.2.1's essential exception.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0008-absolute-session-lifetime.md
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0008 Absolute 8-hour session lifetime

Frozen record: [adr/0008-absolute-session-lifetime.md](../../specs/CHG-0001/adr/0008-absolute-session-lifetime.md).

**Applied in.** [session lifetime rule](../rules/session-lifetime.md); `expires_at` in the [sessions table](../data/sessions.md).
The accessibility consequence (no warning, no extension, WCAG 2.2.1 "essential" exception, Q36) is in [NFR-001](../requirements/nfr-001.md)
and [sdd.md section 9](../../specs/CHG-0001/sdd.md#9-accessibility-nfr-001). Serves [REQ-AUTH-002](../requirements/req-auth-002.md) AC-4.
