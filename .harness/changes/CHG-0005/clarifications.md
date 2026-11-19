# Clarifications — CHG-0005

Request (requester @alokkulkarni): make the merged to-do app of CHG-0001 (Spring Boot API in `api/`, React SPA in
`web/`, SQLite) easy to run and test end to end: README run steps on a fresh checkout and `.env.example`
(placeholders only), completing CHG-0001 NFR-002 AC-1 (deferred in `docs/specs/CHG-0001/conformance.md` L153-L154);
`api/Dockerfile`, `web/Dockerfile` and a `docker-compose.yml` so that one `docker compose up` brings up both with the
SQLite file on a named volume; and how to run the Playwright e2e against it. No cloud or hosted deployment
(CHG-0001 NFR-002 AC-2), no release automation, no CI workflows.

Already settled by the repository, so not asked: the native run's SPA and API share one origin through a proxy of
`/api` (ADR-0001, `web/vite.config.ts` L4-L10); the native run's cookie is not Secure over plain http
(`TODO_COOKIE_SECURE=false`, `docs/specs/CHG-0001/sdd.md` L610-L611); the API runs its Flyway migration on start and
creates the database file (ADR-0014, sdd.md L607-L608); JDK 21 (`api/pom.xml` L24). The SPA builds from `web/` alone:
the generated `web/src/styles/tokens.css` is committed.

The requester answered Q1, Q3 and Q5 on 2026-11-13, before seeing the questions; their words are quoted as given.
They answered the remaining questions on 2026-11-13 with "all as recommended", replying to a list of the
recommended options; each answer below quotes the option they accepted.

## Q1. How should the SPA container serve the app?

`web/vite.config.ts` L9 hard-codes the dev proxy to `http://localhost:8080`, which does not reach another container.
(a) A production build (`pnpm build`) served by nginx, which also reverse-proxies `/api` to the API container, so
the browser still sees one origin. (b) The Vite dev server inside the container, with the proxy target made
configurable (this edits the file the native run uses). (c) The API serves the built SPA itself (one container, a
change to `api/`).
Recommended: (a), because it is closest to how the app would really be served, keeps the same-origin design of
ADR-0001 and leaves the native dev run untouched.

**Answer:** "web to be production build." and "for browser to api in compose as suggested same origin and session cookie." — @alokkulkarni, 2026-11-13

## Q2. Which host ports should the stack publish?

The e2e support code fixes the SPA at `localhost:5173` (`web/e2e/support/launch.ts` L7-L8, Playwright `baseURL`);
another SPA port means changing test code. (a) SPA on 5173 and API on 8080, both bound to `127.0.0.1` only (the API
port for curl and debugging). (b) SPA on 5173 only; the API is reachable only inside the compose network through
the SPA's proxy. (c) Other ports (for example the SPA on 80 or 8080), with the e2e port made configurable.
Recommended: (a), because it matches the native run's ports, needs no test change, and binding to `127.0.0.1` keeps
the app off the local network (NFR-002).

**Answer:** (a) SPA 5173 and API 8080, both bound to 127.0.0.1. (@alokkulkarni: all as recommended, 2026-11-13)

## Q3. Which base images, version pinning and CPU architectures?

`web/.nvmrc` says only `lts/*`, so the Node version is open. (a) Official multi-stage images: `eclipse-temurin:21-jdk`
to build the API with `./mvnw` and `eclipse-temurin:21-jre` (Debian/Ubuntu based, not Alpine, because the SQLite JDBC
driver ships native code) to run it; `node:<current LTS major>` with pnpm through corepack to build the SPA and
`nginx:stable-alpine` to serve it; tags pinned to a major/minor version; containers run as a non-root user; built
locally for the host's own architecture, so Apple silicon (arm64) and x86-64 both work, with nothing published to a
registry. (b) As (a), but every image pinned by digest. (c) Minimal runtimes (distroless) and an explicit
multi-platform `docker buildx` build.
Recommended: (a), because official multi-arch images built locally work on both Apple silicon and x86-64 without a
registry, and minor-version tags are easy to keep current.

**Answer:** "images are both arch created. not just apple silicon.." — @alokkulkarni, 2026-11-13

## Q4. What state should the database be in when the e2e runs against the stack?

The e2e assumes a fresh database for every run (`web/e2e/support/launch.ts` L17-L30; the performance spec adds 500
to-dos), while a named volume keeps data between runs. (a) The README tells the person to start from a fresh stack
before an e2e run (`docker compose down -v`, then `docker compose up -d`), and the same command is the documented way
to wipe their data. (b) A separate compose override or profile for e2e that puts the database on a throwaway volume,
leaving the person's named volume alone. (c) Run against whatever data the volume holds.
Recommended: (a), because it adds no file and makes the e2e start from the state the tests expect; it does wipe the
person's own to-dos, which the README would say.

**Waived:** @alokkulkarni — no Playwright e2e runs against the compose stack (Q5); compose is for the person's manual UAT. (all as recommended, 2026-11-13)

## Q5. Which e2e suites run against the stack, and where does Playwright run?

`E2E_EXTERNAL_SERVERS=1` lets the main Playwright configuration use servers that are already running
(`web/playwright.config.ts` L4-L7, `web/e2e/global-setup.ts` L4-L9). The restart and session-expiry suites start and
stop their own API process, the latter with the test-only clock of ADR-0013, so they cannot use a container.
(a) Playwright runs on the host (`E2E_EXTERNAL_SERVERS=1 pnpm -C web e2e:main`) against the stack on
`localhost:5173`; the restart and session-expiry suites stay on the native run, and the README says so. (b) A
Playwright container (Microsoft's Playwright image) inside compose, under a profile, runs the main suite. (c) Both.
Recommended: (a), because it reuses the existing switch with no test change and needs only Node and the Playwright
browsers on the host.

**Answer:** "dont run playwright e-e against compose.. user would check and validate and run User acceptance testing." — @alokkulkarni, 2026-11-13

## Q6. Should the containers have healthchecks, and how?

The API has no health endpoint (`api/pom.xml` has no Actuator starter). (a) A compose healthcheck on the API that
probes an existing endpoint (an unauthenticated request answered with 401 counts as up), with the SPA container
waiting for it (`depends_on: condition: service_healthy`); no new dependency. (b) Add Spring Boot Actuator and probe
`/actuator/health` (a new dependency, a security-configuration change and its tests). (c) No healthchecks; start
order only.
Recommended: (a), because `docker compose up` then reports the stack ready only when the API answers, without
changing the API's code or attack surface.

**Answer:** (a) a compose healthcheck on an existing endpoint (401 = up); web waits for the API to be healthy. No API code change. (@alokkulkarni: all as recommended, 2026-11-13)

## Q7. What is `.env.example` for?

Nothing in the native run loads a `.env` file (CHG-0001 validator finding F-11, recorded at
`.harness/changes/CHG-0001/conformance.json` L991), but Docker Compose reads a `.env` beside `docker-compose.yml`
automatically. Inside a container the database path differs from the native default `./data/todo.db`. (a) It
documents the native run's settings (`TODO_DB_PATH`, `TODO_COOKIE_SECURE=false`, `SERVER_PORT=8080`) for the person
to export in their shell; `docker-compose.yml` sets the containers' own values itself, and a comment in
`.env.example` says compose does not need it. (b) It serves both: compose reads the same variables through
`${VAR:-default}` substitution. (c) Two files, one per run.
Recommended: (a), because one variable cannot hold both the native and the container database path, and keeping
compose self-contained means `docker compose up` works with no setup.

**Answer:** (a) `.env.example` documents the native run only; compose sets the containers' own values. (@alokkulkarni: all as recommended, 2026-11-13)

## Q8. What should the README cover?

(a) Run steps only: prerequisites, the native run (sdd.md §11), the Docker Compose run and the e2e run against each.
(b) As (a), plus the unit and component test commands, how to wipe the data, and the common failures (a port already
in use, Docker not running). (c) As (b), plus the contributor and harness workflow.
Recommended: (b), because "run and test end to end on a fresh checkout" needs the test commands and the reset, and
the harness workflow already lives in `CLAUDE.md` and `AGENTS.md`.

**Answer:** (b) run steps + unit/component test commands + wiping data + common failures. (@alokkulkarni: all as recommended, 2026-11-13)

## Q9. How should "done" be proven for the containers?

(a) The agents build both images, run `docker compose up`, and run the main e2e suite against the stack, attaching
the report as evidence; if Docker is not available to them, they stop and you run it, and that report is attached
with who ran it and on which commit. (b) Review only: the critic and validator inspect the files, and you run the
stack once yourself. (c) A scripted smoke check only (the SPA page loads and `/api` answers through its proxy).
Recommended: (a), because the request's goal is that the stack really runs and the e2e passes against it; Docker on
this machine decides who runs it.

**Answer:** (c) a scripted smoke check (page loads and /api answers through the proxy) plus the user's own UAT. (@alokkulkarni: all as recommended, 2026-11-13; the list they accepted recommended (c) after their Q5 answer, not this file's (a))

## Q10. What should the solution architecture cover?

This plan has a solution-architecture node before the spec, and the repository has no `docs/architecture/` yet, so
this change writes the first one (with draw.io views). (a) The whole app as CHG-0001 built it (SPA, API, SQLite)
with two deployment views: the native local run and the Docker Compose stack. (b) Only the Docker Compose deployment
view, linking CHG-0001's design for the rest. (c) The whole app with the Compose stack as its only deployment view.
Recommended: (a), because a first solution architecture should describe the system as it is, and both ways of
running it remain supported.

**Answer:** (a) the whole app, with a native-run view and a Compose deployment view. (@alokkulkarni: all as recommended, 2026-11-13)

## Q11. With images for both architectures (Q3), which base images and version pinning?

Q3's answer settles the architectures, not the images or their pinning. (a) Official multi-arch images:
`eclipse-temurin:21-jdk` to build the API and `eclipse-temurin:21-jre` (Ubuntu based, not Alpine, because the SQLite
JDBC driver ships native code) to run it; `node:<current LTS major>` with pnpm through corepack to build the SPA and
`nginx:stable-alpine` to serve it; tags pinned to a major/minor version; containers run as a non-root user. (b) As
(a), but every image pinned by the digest of its multi-arch index. (c) Minimal runtimes (distroless).
Recommended: (a), because each of these images is published for both linux/amd64 and linux/arm64, and minor-version
tags are easy to keep current.

**Answer:** (a) official multi-arch images: temurin 21 jdk to build and 21 jre on Ubuntu (not Alpine), node LTS with corepack pnpm, nginx:stable-alpine. Major/minor tags, non-root. (@alokkulkarni: all as recommended, 2026-11-13)

## Q12. How are the images for both architectures produced, with no registry?

Plain `docker compose up` builds for the machine's own architecture only. Docker keeps an image for several platforms
locally only with its containerd image store turned on; otherwise a multi-platform image must be pushed to a
registry. (a) `docker compose up` builds for the machine's own architecture from multi-arch base images, so the same
files work on linux/amd64 and linux/arm64 machines, and the README gives a `docker buildx build --platform
linux/amd64,linux/arm64` command (no push) that proves both build. (b) `docker-compose.yml` lists both platforms
(`build.platforms`), so every build makes both; the README states the prerequisite (buildx and the containerd image
store). (c) Build both and publish them to a registry such as GitHub Container Registry (needs a registry, which the
request does not mention).
Recommended: (a), because `docker compose up` stays fast and needs no extra Docker setup, while the documented
command still shows both architectures build.

**Answer:** (a) `compose up` builds the native architecture; the README gives a `docker buildx build --platform linux/amd64,linux/arm64` proof command, with no push. (@alokkulkarni: all as recommended, 2026-11-13)

## Q13. In the compose stack, should the session cookie be sent without the Secure flag?

Q1's answer puts the browser and the API on one origin with the existing session cookie, served over plain http on
`localhost`. The API marks the cookie Secure only when `TODO_COOKIE_SECURE=true` (sdd.md L610-L611). (a)
`docker-compose.yml` sets `TODO_COOKIE_SECURE=false`, as the native run does, and the README says any hosting would
need HTTPS and `true`. (b) Set it to `true` and rely on browsers treating `http://localhost` as secure (Chromium and
Firefox do; Safari/WebKit may not keep the cookie). (c) Serve the stack over HTTPS with a local self-signed
certificate and set it to `true`.
Recommended: (a), because it works in every browser on plain http, matches the native run, and keeps certificates out
of a local-only stack.

**Answer:** (a) TODO_COOKIE_SECURE=false over http on localhost; the README notes that hosting needs HTTPS and true. (@alokkulkarni: all as recommended, 2026-11-13)

## Q14. Node 25 and later no longer bundle corepack: which Node image builds the SPA?

Asked after the spec review (SDD section 12, OQ-1), because Q11's answer names "node LTS with corepack pnpm". (a)
`node:26-slim` (the current LTS line) with an exactly pinned corepack installed through npm in the build stage only. (b)
`node:24-slim`, an older LTS line that still bundles corepack. (c) `node:22-slim`, matching the harness tool pin.
Recommended: (a), because it is the only option that is both the current LTS and corepack-managed pnpm, and it adds
nothing to the run image.

**Answer:** "Accept for node version": (a) `node:26-slim`, with an exactly pinned corepack installed through npm in the build stage only. (@alokkulkarni, 2026-11-13)
