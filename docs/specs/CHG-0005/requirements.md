---
stage: requirements
changeId: CHG-0005
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0005
    agent: requirements-analyst
    agentRunId: run-chg5-n02-1
upstream: []
---

# Requirements — CHG-0005

## Context and goal

CHG-0005 makes the to-do app that CHG-0001 merged (Spring Boot API in `api/`, React SPA in `web/`, SQLite) easy to run
and check on a fresh checkout: run steps in the README, a placeholders-only `.env.example`, container images for the
API and the SPA, and a Docker Compose file that brings both up with one command and keeps the SQLite file on a named
volume. `README.md` already exists at the root as a title only, so it is edited, not created. This change completes
CHG-0001 NFR-002 AC-1 ("developer machine with the documented prerequisites, the app is started and usable"), which
`docs/specs/CHG-0001/conformance.md` L153-L154 deferred because the README held no run steps and no `.env.example`
existed (the root files had no owning node; finding F-11, `.harness/changes/CHG-0001/conformance.json` L991). It keeps
CHG-0001 NFR-002 AC-2 (no cloud or hosted deployment). Source of every item is
`.harness/changes/CHG-0005/clarifications.md` (Q1–Q13, Q4 waived); the bracketed Q numbers and decision numbers trace
each item. ID families are new and do not collide with CHG-0001 (`REQ-AUTH/ISO/TODO/LIST/NAV/DATA/ACC`,
`NFR-001`–`NFR-007`): this change uses `REQ-DOC-`, `REQ-CMP-`, `REQ-SMK-` and `NFR-101` upward.

## Decisions after clarification

Made by the requester (@alokkulkarni) on 2026-11-13, after the clarification phase; quoted as given and applied below.

- D1: the Q9 (c) compose smoke-check script lives under `web/` (for example `web/scripts/compose-smoke.mjs`), owned by
  the React build node. The requester said: "3 - yes".
- D2: Q5 governs Q8 (b): the README describes the Playwright e2e on the native run only, never against compose. The
  requester said: "4 - yes". Q8 (b)'s "e2e run against each" is therefore narrowed: the e2e is documented for the
  native run only; the compose stack is for the person's manual UAT plus the scripted smoke check.
- D3: `README.md`, `.env.example` and `docker-compose.yml` stay at the repository root as the request specified;
  `api/Dockerfile` and `web/Dockerfile` sit in their modules.
- D4: the change's solution architecture covers the whole app with a native-run view and a Compose deployment view
  (Q10 (a)). It is not a requirements matter.

## Glossary

- **Native run**: running the API with `./mvnw spring-boot:run` and the SPA with `pnpm dev` on the host (CHG-0001
  sdd.md §11).
- **Compose stack**: the containers started by `docker compose up` from `docker-compose.yml`.
- **Person**: whoever follows the README on a fresh checkout.
- **Smoke check**: the script of REQ-SMK-001.
- **UAT**: the person's manual user acceptance testing of the running Compose stack in a browser (Q5).
- **Warm machine**: a machine on which the images of the stack are already built and their base layers cached.

## Run documentation

## REQ-DOC-001 README: prerequisites and native run (M)

Traces: request, Q8 (b), Q7, CHG-0001 sdd.md §11 L610-L611 (native cookie setting), CHG-0001 NFR-002 AC-1.

- AC-1: Given a fresh checkout and the machine prerequisites named in the README, when the person follows only the
  README's native-run steps, then the API answers on `http://localhost:8080` and the SPA on `http://localhost:5173`,
  and the person can sign up, log in and create a to-do (verified by the person's UAT; the steps are verified by
  inspection against CHG-0001 sdd.md §11).
- AC-2: Given the README, when read, then it lists the prerequisites: JDK 21, Node.js (current LTS, per `web/.nvmrc`),
  pnpm, the Playwright browser install step (needed only for the e2e of REQ-DOC-003) with its command, and, for the
  Compose run only, Docker with Compose.
- AC-3: Given the README, when read, then it states that the native run needs no cloud service, hosted database or
  email service and creates the SQLite file (default `api/data/todo.db`, because the API is started from `api/`) and
  runs its migration on first start.
- AC-4: Given the README, when read, then it does not describe the test-only clock, the `e2e` Spring profile or
  `TODO_E2E_CLOCK_OFFSET_FILE` (CHG-0001 sdd.md §11 L619-L620).

## REQ-DOC-002 `.env.example` documents the native run (M)

Traces: Q7 (a), Q13, CHG-0001 NFR-004 AC-3.

- AC-1: Given the repository root, when inspected, then `.env.example` exists and sets `TODO_DB_PATH`,
  `TODO_COOKIE_SECURE=false` and `SERVER_PORT=8080` with placeholder or default values only.
- AC-2: Given `.env.example`, when read, then a comment says the native run does not load a `.env` file (the person
  exports the variables in the shell) and that Docker Compose does not need it.
- AC-3: Given the repository, when scanned for credentials (inspection of `.env.example`, `docker-compose.yml`, both
  Dockerfiles and the README), then no secret or real credential is present and no real `.env` file is committed.
- AC-4: Given the root `.gitignore`, when inspected, then it ignores `.env` (and does not ignore `.env.example`), so a
  copied `.env` cannot be committed by accident (proposed).

## REQ-DOC-003 README: tests, wiping data and common failures (M)

Traces: Q8 (b), D2, Q5.

- AC-1: Given the README, when read, then it gives the commands for the API tests (run from the root, for example
  `api/mvnw -f api/pom.xml test`), the web unit and component tests (`pnpm -C web test`) and the Playwright e2e on the
  native run (`pnpm -C web e2e`), each one runnable from the repository root.
- AC-2: Given the README, when read, then it states that the Playwright e2e is run on the native run only and not
  against the Compose stack, and that the Compose stack is checked by the smoke check and by the person's UAT (D2, Q5).
- AC-3: Given the README, when read, then it says how to wipe the data for each run: for the native run, delete the
  SQLite file (default `api/data/todo.db`; a custom `TODO_DB_PATH` replaces that location); for the Compose stack,
  `docker compose down -v`, and it warns that this deletes the person's own to-dos.
- AC-4: Given the README, when read, then a troubleshooting section covers at least a port already in use (5173 or
  8080) and Docker not running, each with the symptom and the fix.
- AC-5: Given the README, when read, then it states that `pnpm -C web e2e` starts its own API and SPA on a temporary
  database, so ports 8080 and 5173 must be free: the person stops the native run and the Compose stack first (the
  e2e fails with "port 8080 is already in use" otherwise; `web/e2e/support/api-process.ts` L33).

## Compose stack

## REQ-CMP-001 Container images for the API and the SPA (M)

Traces: Q1 (a), Q3, Q11 (a).

- AC-1: Given `api/Dockerfile`, when built, then the image runs the API on JDK 21: it is built in a multi-stage build
  from an `eclipse-temurin` JDK 21 image using the repository's `./mvnw`, and runs on an `eclipse-temurin` JRE 21 image
  on the Ubuntu-based variant, not Alpine (the SQLite JDBC driver ships native code). Verified by inspection of the
  file and by the build.
- AC-2: Given `web/Dockerfile`, when built, then the SPA is a production build (`pnpm build`) from `web/` alone, made
  with Node's current LTS major and pnpm through corepack, and served by `nginx:stable-alpine`, as the requester named
  it. Verified by inspection and by the build.
- AC-3: Given the Dockerfiles, when inspected, then every base image reference carries an explicit tag that is not
  `latest`, names at least the major version line it pins (`eclipse-temurin` JDK and JRE 21, the Node LTS major), or,
  for nginx, is `stable-alpine` as named in Q11, and is an official multi-architecture image (proposed reading, P6).
- AC-4: Given a built image of each service, when its container runs, then the process does not run as root (user id
  other than 0), verified by `docker compose exec <service> id -u` or `docker run ... id -u`.

## REQ-CMP-002 One command brings up the stack (M)

Traces: request, Q1 (a), Q2 (a).

- AC-1: Given a fresh checkout with Docker running and ports 5173 and 8080 free, when the person runs
  `docker compose up` from the repository root (no `.env` file, no other setup), then both images build and both
  containers start, and `docker compose ps` shows both running (and the API healthy, REQ-CMP-005). Verified by the
  smoke check and by the person's UAT.
- AC-2: Given the running stack, when the person opens `http://localhost:5173` in a browser, then the to-do app loads
  and the person can sign up, log in, create a to-do and log out (verified by the person's UAT).
- AC-3: Given the running stack, when the person stops it with `docker compose down`, then a following
  `docker compose up` shows the to-dos the person created (see REQ-CMP-004; verified by the person's UAT).

## REQ-CMP-003 Ports and one origin (M)

Traces: Q1, Q2 (a), NFR-101.

- AC-1: Given the running stack, when `docker compose ps` or `docker compose port` is read, then the SPA container
  publishes host port 5173 and the API container publishes host port 8080, both bound to `127.0.0.1` only (not
  `0.0.0.0`).
- AC-2: Given the running stack, when the browser requests `http://localhost:5173/api/...`, then the SPA container's
  web server reverse-proxies the request to the API container and the browser sees one origin for the page and the
  API; no cross-origin (CORS) setting is needed. Verified by the smoke check (REQ-SMK-001).
- AC-3: Given the running stack, when the person logs in through the SPA on `http://localhost:5173`, then the session
  cookie set by the API is kept by the browser and sent on later `/api` requests (verified by the person's UAT: the
  to-do list stays available after a page reload).
- AC-4: Given the running stack, when `curl http://127.0.0.1:8080/api/...` is run on the host, then the API answers
  (for debugging; verified by the person or by the agents running the command).

## REQ-CMP-004 SQLite on a named volume (M)

Traces: request, CHG-0001 REQ-DATA-001, Q7 (a).

- AC-1: Given `docker-compose.yml`, when inspected, then the API container's `TODO_DB_PATH` points inside a directory
  mounted from a named volume declared in the same file (not a bind mount of the checkout), and the file sets
  `TODO_DB_PATH`, `TODO_COOKIE_SECURE` and `SERVER_PORT` as literal values with no variable interpolation, so a `.env`
  copied from `.env.example` cannot change the containers' values.
- AC-2: Given a to-do created in the running stack, when the person runs `docker compose down` and then
  `docker compose up`, then the to-do is still listed (verified by the person's UAT).
- AC-3: Given the stack on a fresh volume, when the API starts, then it creates the database file and runs its
  migration with no manual step (the API behaviour of CHG-0001 ADR-0014), and the non-root API user can write to the
  volume (proposed; verified by the smoke check reaching a healthy API on a fresh volume, REQ-CMP-005 AC-1, and by the
  person's UAT creating a to-do).
- AC-4: Given the person has data, when they run `docker compose down -v`, then the named volume is removed and the
  next `docker compose up` starts with an empty database (verified by the person's UAT).

## REQ-CMP-005 Healthcheck and start order (M)

Traces: Q6 (a).

- AC-1: Given `docker-compose.yml`, when inspected, then the API service has a healthcheck that requests an endpoint
  the API already exposes and treats an unauthenticated 401 answer as up; the check needs no new endpoint, dependency
  or API code change.
- AC-2: Given the stack starting, when the API is not yet answering, then the SPA container waits
  (`depends_on` with `condition: service_healthy`) and starts only after the API is healthy.
- AC-3: Given the API container keeps running but its healthcheck cannot succeed, when the healthcheck runs, then
  `docker compose ps` reports the API as unhealthy (proposed; verified by a trial run that makes the probe fail while
  the container keeps running, for example pointing the check at a closed port).

## REQ-CMP-006 Session cookie flag in the stack (M)

Traces: Q13 (a).

- AC-1: Given `docker-compose.yml`, when inspected, then the API service sets `TODO_COOKIE_SECURE=false`.
- AC-2: Given the README, when read, then it says the stack is for plain http on localhost, and that any hosting would
  need HTTPS with `TODO_COOKIE_SECURE=true`.

## REQ-CMP-007 Images for both linux/amd64 and linux/arm64 (M)

Traces: Q3, Q11 (a), Q12 (a).

- AC-1: Given a linux/amd64 machine or a linux/arm64 machine (for example Apple silicon), when the person runs
  `docker compose up` on the machine at hand, then the images build for that machine's own architecture and run, using
  the same files. Verified by the person's UAT on the machine at hand; the other architecture is proven only by AC-3.
- AC-2: Given the README, when read, then it gives a `docker buildx build --platform linux/amd64,linux/arm64`
  command for each Dockerfile that builds both architectures, neither loads the image nor pushes it (no `--push`, for
  example `--output type=cacheonly`), and says that it is the proof that both build and that nothing is published to a
  registry. It also states the prerequisites of that command: a builder that supports multi-platform builds (the
  containerd image store turned on, or a `docker-container` builder) and emulation for the non-native architecture
  (Q12, which says Docker keeps a multi-platform image only with the containerd image store or after a push).
- AC-3: Given a machine that meets the prerequisites stated in AC-2, when the README's proof command is run for
  `api/Dockerfile` and for `web/Dockerfile`, then both builds complete for both platforms (verified by running the
  command, by the agents or by the person; the report names who ran it and on which commit).

## REQ-DOC-004 README: Compose run (M)

Traces: request, Q2, Q5, Q12, D2.

- AC-1: Given the README, when read, then it gives the Compose steps from a fresh checkout (`docker compose up`, the URL
  `http://localhost:5173`, how to stop, how to wipe with `docker compose down -v`) and lists the ports and that they
  are bound to `127.0.0.1`.
- AC-2: Given the README, when read, then it points the person to the smoke check (REQ-SMK-001) and to their own UAT
  as the way to check the stack, and states that Playwright is not run against it (D2).
- AC-3: Given the README, when read, then it states that the data lives on a named volume that survives
  `docker compose down` and is removed by `docker compose down -v`.

## REQ-SMK-001 Scripted smoke check of the stack (M)

Traces: Q9 (c), D1, Q1.

- AC-1: Given the repository, when inspected, then the smoke check is a script in the `web/` module (for example
  `web/scripts/compose-smoke.mjs`; the exact path and implementation are the SDD's) that the person can run with Node
  and a documented command.
- AC-2: Given the running Compose stack, when the smoke check runs, then it requests the SPA page at
  `http://localhost:5173` and an unauthenticated `GET` of an existing API path through the SPA's proxy (for example
  `/api/v1/todos`), and it exits 0 only if the page returns 200 with HTML and the `/api` request returns 401 with
  `Content-Type: application/problem+json` (the contract answer for an unauthenticated request, CHG-0001
  `common-problem-v1.yaml` L21-L26, L70-L75). A 200 HTML answer on `/api` (the SPA fallback) does not pass.
- AC-3: Given the stack is not running, or the SPA answers but the `/api` request returns anything other than 401 with
  `application/problem+json` (a proxy error such as 502 or 504, a 200 HTML fallback, or a connection failure), when
  the smoke check runs, then it exits non-zero and prints which of the two requests failed and what it received.
- AC-4: Given the smoke check, when it runs, then it uses only the base URL (default `http://localhost:5173`, a
  documented override allowed), sends no credentials, creates no data, and makes no Playwright call (proposed).
- AC-5: Given the README, when read, then it gives the smoke check's command and says it needs the stack already
  running.
- AC-6: Given the stack built from this change's head commit, when the smoke check is run against it, then it exits 0,
  and the run is recorded as evidence with its output, who ran it (the agents when Docker is available to them, else
  the person) and the commit it ran on.
- AC-7: Given the stack built from this change's head commit, when the person has done the UAT steps (REQ-DOC-001
  AC-1, REQ-CMP-002 AC-2 and AC-3, REQ-CMP-003 AC-3, REQ-CMP-004 AC-2 and AC-4), then the outcome is recorded naming
  the person, the steps done and the commit, for example as a PR comment or an evidence note (proposed, P5).

## Non-functional requirements

Each NFR below is a cross-reference to the AC it names; a test or inspection for that AC may carry both IDs.

## NFR-101 Local-only exposure (M)

Traces: Q2 (a), CHG-0001 NFR-002.

- AC-1: Given the Compose stack, when its published ports are listed, then no port is bound to an address other than
  `127.0.0.1` (REQ-CMP-003 AC-1) and no other port is published.

## NFR-102 No secrets (M)

Traces: Q7, CLAUDE.md rule 10, CHG-0001 NFR-004 AC-3.

- AC-1: Given the files this change adds, when inspected, then they contain placeholders or non-secret defaults only
  (REQ-DOC-002 AC-3); no registry credential, token or password appears in any file or build argument.

## NFR-103 Non-root containers (M)

Traces: Q3, Q11.

- AC-1: Given both containers running, when their user id is read, then it is not 0 (REQ-CMP-001 AC-4).

## NFR-104 Multi-architecture build, no registry (M)

Traces: Q3, Q11, Q12.

- AC-1: Given the README's proof command, when run, then both linux/amd64 and linux/arm64 build and nothing is pushed
  (REQ-CMP-007 AC-2, AC-3); no file of this change configures a registry, a publish step or credentials for one.

## NFR-105 Data durability (M)

Traces: request, CHG-0001 REQ-DATA-001.

- AC-1: Given the SQLite file on a named volume, when the containers are stopped and removed without `-v`, then no
  to-do is lost (REQ-CMP-004 AC-2).

## NFR-106 Startup readiness (M)

Traces: Q6 (a); the target is an analyst proposal (P1), not an answer.

- AC-1: Given a built stack on a warm machine, when `docker compose up` runs, then the API reports healthy within 120
  seconds (proposed) and the SPA then answers on port 5173. Measured by whoever runs the smoke check (AC-6 of
  REQ-SMK-001) from the healthcheck status times in `docker compose ps` or `docker compose events`.

## NFR-107 Documentation accuracy (M)

Traces: CHG-0001 NFR-002 AC-1, Q8; the requirement is an analyst proposal (P4).

- AC-1: Given the README's commands, when each is read against the repository (`web/package.json` scripts,
  `docker-compose.yml` service names, the files and paths named), then every command and path it names exists, by
  inspection (proposed).

## Completion of CHG-0001 NFR-002 AC-1

This change completes CHG-0001 NFR-002 AC-1, which `conformance.md` L153-L154 deferred: REQ-DOC-001 supplies the
documented prerequisites and run steps; REQ-DOC-002 the `.env.example`. CHG-0001 NFR-002 AC-2 (no cloud or hosted
deployment, no `dev`/`test` deployment) stays in force and is not changed.

## Out of scope

- Any cloud or hosted deployment, and any `dev` or `test` environment deployment (CHG-0001 NFR-002 AC-2).
- Release automation, versioning or tagging of images.
- CI workflows (GitHub Actions or other).
- Publishing to any registry, including GitHub Container Registry (Q12 (c)).
- Playwright e2e against the Compose stack, and a Playwright container (Q5, D2); the Q4 fresh-database procedure
  (waived). Any change to `web/e2e` or the Playwright `baseURL`.
- Any change to `api/` code or its dependencies, including Spring Boot Actuator (Q6 (b)).
- Making `web/vite.config.ts`'s dev proxy target configurable, or changing the native run (Q1 (a)).
- HTTPS, certificates or `TODO_COOKIE_SECURE=true` in the stack (Q13 (b), (c)).
- Distroless images and digest pinning (Q11 (b), (c)).
- The README covering the contributor or harness workflow (Q8 (c)).
- Moving `README.md`, `.env.example` or `docker-compose.yml` out of the repository root (D3).

## Proposed (needs confirmation)

For the approver to confirm at the spec freeze.

- P1: the API reports healthy within 120 seconds on a warm machine (NFR-106); the warm-machine definition and the way
  to measure it are in the glossary and NFR-106 AC-1. (proposed)
- P2: the smoke check passes only when the page returns 200 HTML and an unauthenticated `GET /api/v1/todos` through
  the proxy returns 401 `application/problem+json`; the exact path and the script are the SDD's. (proposed)
- P3: the smoke check's base URL override is an environment variable or an argument; the name is the SDD's. (proposed)
- P4: the README troubleshooting lists only the two failures of Q8 (b) as a minimum; further entries are the author's
  choice. NFR-107 (README accuracy by inspection) is also an analyst addition. (proposed)
- P5: the person's UAT outcome and the smoke-check run are recorded as evidence naming who and which commit (REQ-SMK-001
  AC-6, AC-7); the form (PR comment or evidence note) is the approver's choice. (proposed)
- P6: reading of Q11's image rule, consistent with all the images the requester named: each tag is explicit, never
  `latest`, and names at least the major version line it pins (`eclipse-temurin` JDK 21 and JRE 21 on Ubuntu, Node LTS
  major); `nginx:stable-alpine` is accepted as the requester named it (a stable-channel tag, not a major/minor pair).
  Minor-version pinning is not required. (proposed)
- P7: the analyst additions not settled by an answer: `.env` is git-ignored (REQ-DOC-002 AC-4); the non-root API user
  can write to the volume (REQ-CMP-004 AC-3); the unhealthy-state trial (REQ-CMP-005 AC-3); the smoke check sends no
  credentials, creates no data and makes no Playwright call (REQ-SMK-001 AC-4); the README states the `buildx`
  prerequisites and a no-load, no-push output (REQ-CMP-007 AC-2); the README states the e2e needs ports 8080 and 5173
  free (REQ-DOC-003 AC-5). (proposed)

## Open questions

None.
