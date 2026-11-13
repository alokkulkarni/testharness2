---
type: decision
title: ADR-0013 Injected clock and a test-only clock for end-to-end expiry
summary: All time comes from an injected Clock, and only a separate e2e profile in the test source set can move it, so the 8-hour expiry can be tested without changing the normal run.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0013-injected-clock-and-e2e-test-clock.md
  - api/src/main/java/app/todo/common/ClockConfig.java
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0013 Injected clock and a test-only clock for end-to-end expiry

Frozen record: [adr/0013-injected-clock-and-e2e-test-clock.md](../../specs/CHG-0001/adr/0013-injected-clock-and-e2e-test-clock.md).

**Applied in.** `ClockConfig` in the [shared kernel](../components/shared-kernel.md) provides the system UTC `Clock`. The
test-only clock classes (`OffsetFileClock`, `E2eClockConfiguration`) live in the test source set and start only with
`spring-boot:test-run` under the `e2e` profile ([sdd.md section 11](../../specs/CHG-0001/sdd.md#11-local-run-nfr-002-req-data-001-ac-3)).
Default-profile tests assert the system clock (`api/src/test/java/app/todo/config/ModuleWiringTest.java`).
Serves [REQ-AUTH-004](../requirements/req-auth-004.md) AC-4.
