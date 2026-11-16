---
title: Solution architecture (placeholder)
stage: solution-architecture
changeId: CHG-0004
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0004
    agent: drawio-architect
    agentRunId: run-chg4-n05-1
upstream:
  - path: docs/specs/CHG-0004/requirements.md
    sha256: c67cab9d65db8c4d5eed647062b13def283addb1bd930d1aace77c8979dc5f43
---

# Solution architecture (placeholder)

## 1. Placeholder written by CHG-0004

This file is a placeholder. CHG-0004 wrote it. CHG-0004 is an audit-only change: it adds accessibility tests and
evidence and no product code (`docs/specs/CHG-0004/requirements.md`, REQ-A11Y-010 AC-3). It changes no architecture. It
adds or changes no container, integration, store, deployment, security boundary or observability element. This file
describes no design and has no diagrams.

## 2. Where the as-built architecture is

The as-built architecture is CHG-0001's. Read it there; nothing is copied here.

- `docs/specs/CHG-0001/design/architecture/README.md`: the index of its views, each a `.drawio` source with a PNG and a
  PDF export beside it: C4 context, C4 container, C4 components (API modules and error path), sequences (sign-up and
  login, create/edit/list, clear completed), data model, local deployment (`deployment-local`) and screen flow.
- `docs/specs/CHG-0001/sdd.md` §2 (components) and §11 (local run).

## 3. Pending: replace with CHG-0005's whole-app architecture

CHG-0005 (PR #7, frozen, parked until harness M22) writes the full solution architecture, with native-run and Compose
views. Whichever of CHG-0004 and CHG-0005 merges into `develop` second must reconcile `docs/architecture/solution.md`.
CHG-0005's version is the intended content. Do not extend this stub in the meantime.
