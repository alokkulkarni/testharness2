---
type: decision
title: ADR-0001 React and TypeScript single-page app
summary: The web UI is a React and TypeScript single-page app built with Vite, with API types generated from the contracts.
owners: [alokkulkarni]
status: current
changeId: CHG-0001
sources:
  - docs/specs/CHG-0001/adr/0001-react-typescript-spa.md
  - docs/specs/CHG-0001/sdd.md
---

# ADR-0001 React and TypeScript single-page app

Frozen record: [adr/0001-react-typescript-spa.md](../../specs/CHG-0001/adr/0001-react-typescript-spa.md).

**Applied in.** [web SPA](../components/web-spa.md); generated types per module in `web/src/api/identity.ts` and
`web/src/api/todos.ts` ([sdd.md section 12.1 item 17](../../specs/CHG-0001/sdd.md#121-for-the-approver-to-confirm-at-freeze)).
Serves [NFR-005](../requirements/nfr-005.md).
