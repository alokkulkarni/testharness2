---
stage: plan
changeId: CHG-0001
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0001
    agent: orchestrator
    agentRunId: run-plan-md-1
upstream:
  - path: docs/specs/CHG-0001/requirements.md
    sha256: 266b5b6d3d319d635b6fbe7e01d1750d017d704a0a24dada7243c8b6264241bb
  - path: docs/specs/CHG-0001/sdd.md
    sha256: 13262e8e00cf325ca29d7a40011bd6e88949bcd0c854029e8f79f3e9eee41d4b
---

# Implementation plan — CHG-0001: Create a to-do list

<!-- Written by harness plan md (SDD 02 §13). Each build node fills its own plan file before its first code commit. -->

| Node | Agent | Does | Plan |
|---|---|---|---|
| N09-build-java | java-engineer | Make the java tests pass in an isolated worktree. | [plan/N09-build-java.md](plan/N09-build-java.md) |
| N10-build-react | react-engineer | Make the react tests pass in an isolated worktree. | [plan/N10-build-react.md](plan/N10-build-react.md) |
