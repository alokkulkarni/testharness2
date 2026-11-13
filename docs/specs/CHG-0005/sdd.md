---
stage: sdd
changeId: CHG-0005
authors:
  - squadSessionId: SQS-default-01M6QZZPTSQ6RP91Y1RRZ8AZAM
    changeId: CHG-0005
    agent: spec-author
    agentRunId: run-chg5-n03-2
upstream:
  - path: docs/specs/CHG-0005/requirements.md
    sha256: 9d004705a190fb1c59a66cceb81139c88a4ec5e4647836ee2dc2943ccbc2d531
---

# SDD — CHG-0005 Local run docs and Docker Compose for the to-do app

## 1. Context and goals

CHG-0005 makes the app CHG-0001 built (Spring Boot API in `api/`, React SPA in `web/`, SQLite) runnable and checkable
on a fresh checkout: README run steps, a placeholders-only `.env.example`, an image per module, and a
`docker-compose.yml` that starts both with the SQLite file on a named volume. Requirements: `requirements.md` —
REQ-DOC-001..004, REQ-CMP-001..007, REQ-SMK-001, NFR-101..107. It completes CHG-0001 NFR-002 AC-1 and keeps NFR-002 AC-2
(no cloud or hosted deployment).

The design follows the reviewed solution architecture, `docs/architecture/solution.md` (sections 3, 6.2, 7, 8, 9) and
adds no container, integration, store, deployment element, security boundary or observability element it does not
show. Section 6 below answers each of its section-9 questions.

**Interfaces.** No new interface: the stack serves CHG-0001's API unchanged, whose contracts
(`docs/specs/CHG-0001/contracts/identity-api-v1.yaml`, `todo-api-v1.yaml`, `common-problem-v1.yaml`) stay the source of
truth. The smoke check and the healthcheck only call existing operations, so this change has no `contracts/` folder.

**Decisions.** [adr/0019-single-api-container-sqlite-named-volume.md](adr/0019-single-api-container-sqlite-named-volume.md)
and [adr/0020-nginx-same-origin-spa-and-api-proxy.md](adr/0020-nginx-same-origin-spa-and-api-proxy.md), the two ADRs
the architecture's section 8 asks for. They continue CHG-0001's numbering (0001..0018) so that an ADR number names one
decision in the repository. The healthcheck and the image choices are fixed by requirements (REQ-CMP-005, REQ-CMP-001,
REQ-CMP-007) and need no ADR (`solution.md` section 8).

**Requester's decisions of 2026-11-13** (@alokkulkarni, after the clarification phase; recorded in
`requirements.md` L28-L40):

1. The Q9 (c) compose smoke-check script lives under `web/` (for example `web/scripts/compose-smoke.mjs`), so the React
   build node owns it. The requester said: "3 - yes".
2. Q5 governs Q8 (b): the README describes the Playwright e2e on the native run only, never against Compose. The
   requester said: "4 - yes".
3. `README.md`, `.env.example` and `docker-compose.yml` stay at the repository root, as specified; they are not
   relocated (requirements D3).
4. The solution architecture covers the whole app with a native-run view and a Compose view (Q10 (a)), already written
   (requirements D4).

**Requester's decision on the Node image** (@alokkulkarni, 2026-11-13, Q14 in
`.harness/changes/CHG-0005/clarifications.md:L180-L188`, asked after the first spec review): "Accept for node
version": option (a), `node:26-slim` (the current LTS line) with an exactly pinned corepack installed through npm in
the build stage only. Section 3.2 is written for it; no open question remains.

Nothing outside the requirements is designed: no CI, no registry, no HTTPS, no Playwright against Compose, no change to
`api/src`, `web/src`, `web/e2e`, `web/vite.config.ts` or the native run.

## 2. Module layout

The plan's tests and build nodes work in exactly these module directories:

- java: `api/` (`api/pom.xml`)
- react: `web/` (`web/package.json`)

`api/` gains `api/Dockerfile` and `api/.dockerignore`, with no change in `api/src` or `api/pom.xml`. `web/` gains
`web/Dockerfile`, `web/.dockerignore`, `web/nginx/nginx.conf`, `web/scripts/compose-smoke.mjs`,
`web/scripts/compose-smoke.d.mts` and the test `web/tests/scripts/compose-smoke.test.ts`; `web/package.json` is not
changed. The repository root, which belongs to no stack (risk R1), gains `.env.example` and `docker-compose.yml` and
changes `README.md` and `.gitignore`.

Data model changes: none (the schema stays CHG-0001's `V1__init.sql`).

| File | Module | New or changed | Requirements |
|---|---|---|---|
| `api/Dockerfile` | api | new | REQ-CMP-001 AC-1, AC-3, AC-4; REQ-CMP-004 AC-3; REQ-CMP-007 |
| `api/.dockerignore` | api | new | NFR-102 (keeps `api/data/` and `api/target/` out of the build context) |
| `web/Dockerfile` | web | new | REQ-CMP-001 AC-2, AC-3, AC-4; REQ-CMP-007 |
| `web/.dockerignore` | web | new | REQ-CMP-001 AC-2 (host `node_modules` and `dist` never enter the image) |
| `web/nginx/nginx.conf` | web | new | REQ-CMP-003 AC-2, AC-3; REQ-CMP-002 AC-2; NFR-103; section 9 privacy constraint |
| `web/scripts/compose-smoke.mjs` | web | new | REQ-SMK-001 AC-1..AC-5 |
| `web/scripts/compose-smoke.d.mts` | web | new | REQ-SMK-001 AC-1 (types for the test; signatures only) |
| `web/tests/scripts/compose-smoke.test.ts` | web | new | REQ-SMK-001 AC-2, AC-3, AC-4 |
| `docker-compose.yml` | root | new | REQ-CMP-002..006, NFR-101, NFR-105, NFR-106 |
| `README.md` | root | changed (today a title only, `README.md:L1`) | REQ-DOC-001, 003, 004, REQ-CMP-006 AC-2, REQ-CMP-007 AC-2, REQ-SMK-001 AC-5, NFR-107 |
| `.env.example` | root | new | REQ-DOC-002 AC-1..AC-3 |
| `.gitignore` | root | changed | REQ-DOC-002 AC-4 |

## 3. Design per file

### 3.1 API image — `api/Dockerfile`, `api/.dockerignore` (REQ-CMP-001 AC-1, AC-3, AC-4; REQ-CMP-004 AC-3)

Build context `api/`. Two stages:

| Stage | Base image (tag) | What it does |
|---|---|---|
| build | `eclipse-temurin:21-jdk-noble` (JDK 21, Ubuntu 24.04) | Copies `mvnw`, `.mvn/` and `pom.xml` first and resolves dependencies (a cacheable layer), then `src/`, and runs `./mvnw -B -DskipTests package`. The Spring Boot plugin (`api/pom.xml:L99-L107`, repackage execution from the parent `L11-L16`) writes one executable jar, which the stage copies to a fixed name. Tests are not run here: the API tests run natively (REQ-DOC-003 AC-1) and skipping them keeps the emulated multi-arch proof practical. A BuildKit cache mount for the Maven repository is allowed, not required. |
| run | `eclipse-temurin:21-jre-noble` (JRE 21 on Ubuntu, not Alpine: sqlite-jdbc ships native code) | As root: creates group and user `todo` with fixed uid and gid **10001**, no login shell, no home; creates `/data` owned by 10001:10001, mode 0750; checks that `curl` is present (`curl --version`, so a base image without it fails the build instead of producing a never-healthy container; Assumption A2); copies the jar to `/app/app.jar` (root-owned, world-readable). Then `USER 10001:10001`, `EXPOSE 8080`, and an exec-form entrypoint `java -jar /app/app.jar`, so the JVM is PID 1 and receives `SIGTERM` from `docker compose down` (the pool closes cleanly, `DatabaseConfig.java:L22`). |

The image sets no `TODO_*` or `SERVER_*` variable; Compose sets them (section 3.4). `.mvn/` and `mvnw` must be in the
context (`api/.mvn/wrapper/maven-wrapper.properties:L17-L19`; the wrapper fetches Maven 3.9.11 at build time).

`api/.dockerignore` excludes `target/` and `data/` (both git-ignored, `api/.gitignore:L1-L2`): `data/` holds the
person's native database, which must never enter a build context or image.

Both tags name the JDK/JRE 21 major line and the Ubuntu release, are not `latest`, and are official multi-arch images
(linux/amd64 and linux/arm64; Assumption A1). The codename tag makes "Ubuntu, not Alpine" checkable by reading the file.

### 3.2 Web image — `web/Dockerfile`, `web/.dockerignore` (REQ-CMP-001 AC-2, AC-3, AC-4)

Build context `web/` alone (the generated `web/src/styles/tokens.css` and `web/src/api/*.ts` are committed). Two
stages:

| Stage | Base image (tag) | What it does |
|---|---|---|
| build | `node:26-slim` (Node's current LTS major, Debian-based; the requester's decision Q14 (a), `.harness/changes/CHG-0005/clarifications.md:L180-L188`; Assumption A3) | Installs **corepack 0.34.0** with npm, globally, in this stage only (Node 25 and later no longer bundle it, Assumption A7; the same step works on any Node; Assumption A11), enables corepack's pnpm and activates **pnpm 10.34.8** as its global pnpm (the pin of `.github/harness/versions.env:L47`; `web/package.json` has no `packageManager` field), with `COREPACK_ENABLE_DOWNLOAD_PROMPT=0`. It prints `node --version`, `corepack --version` and `pnpm --version`, so the build log records the three versions (A3, A11). Copies `package.json` and `pnpm-lock.yaml` (lockfile 9.0, `web/pnpm-lock.yaml:L1`) and runs `pnpm install --frozen-lockfile` (all dependencies, development ones included: `pnpm build` runs `tsc --noEmit`, `web/package.json:L11`); `--frozen-lockfile` fails the build if the pinned pnpm and the lockfile disagree (Assumption A4). Then copies the rest of `web/` and runs `pnpm build`, which writes `dist/`. The corepack version is pinned exactly in the build argument `COREPACK_VERSION`, default `0.34.0` (never a range or `latest`). |
| run | `nginx:stable-alpine` (as named by the requester, Q11; P6) | Removes the image's `/etc/nginx/conf.d/default.conf` and the default files in `/usr/share/nginx/html/`, copies `web/nginx/nginx.conf` over `/etc/nginx/nginx.conf`, copies `dist/` into `/usr/share/nginx/html/` (root-owned, world-readable), then `USER nginx` (uid 101 in the official image; Assumption A5), `EXPOSE 5173`. The image's own entrypoint and command (`nginx -g 'daemon off;'`) are kept. |

`web/.dockerignore` excludes what `web/.gitignore:L1-L7` ignores (`node_modules`, `dist`, `test-results`,
`playwright-report`, `playwright-report-session-expiry`, `playwright-report-restart`, `blob-report`), so host binaries
built for macOS never enter a Linux image.

### 3.3 nginx configuration — `web/nginx/nginx.conf` (REQ-CMP-003 AC-2, AC-3; REQ-CMP-002 AC-2; NFR-103)

One complete main configuration file (it replaces the image's; it includes no `conf.d/`). Directives by context:

| Context | Setting | Reason |
|---|---|---|
| main | no `user` directive; `pid /tmp/nginx.pid` | non-root: the master cannot switch user nor write `/var/run` |
| main | an empty `events {}` block; `worker_processes` not set (nginx's default, 1) | nginx refuses to start without an `events` section; its defaults suffice for one local user |
| main | `error_log /dev/stdout emerg` | stdout, as `solution.md` 6.2 and 7.1 draw it; nginx error lines about a request (for example an upstream failure at level `error`) carry the full request line and referrer, so `?q=` search text, while start-up failures (bad config, bind, pid or temp-path errors) are `emerg` and still appear (Assumption A10) |
| http | `include /etc/nginx/mime.types`, `default_type application/octet-stream` | JavaScript modules need a JavaScript media type, or the browser refuses them |
| http | `client_body_temp_path`, `proxy_temp_path`, `fastcgi_temp_path`, `uwsgi_temp_path`, `scgi_temp_path` each under `/tmp/` | the non-root user cannot write the compiled-in `/var/cache/nginx` |
| http | a `map` from `$request_uri` to the path before the first `?`; `log_format` with only the ISO time, method, that path, status, bytes sent and request time; `access_log /dev/stdout` with that format | the access log never holds a query string, referrer, cookie or user agent (CHG-0001 `sdd.md:L575-L576`; `solution.md` 7.1) |
| server | `listen 5173`; `root /usr/share/nginx/html` | an unprivileged port, the same number inside and on the host |
| `location = /api` and `location ^~ /api/` | `proxy_pass http://api:8080` with no URI part; no `proxy_set_header`, `proxy_cookie_*`, `proxy_hide_header`, `rewrite` or body-size directive | path and query pass as the browser sent them; all request headers pass except `Host` and `Connection`, which nginx sets itself; all response headers including `Set-Cookie` pass. The default 1 MiB body limit is far above the largest body (description at most 2000 code points, `todo-api-v1.yaml:L289`, `L310`). |
| `location /assets/` | `try_files $uri =404` | REQ-CMP-002 AC-2: a missing hashed asset is a 404, never HTML served as a script (a blank app) |
| `location = /index.html` | header `Cache-Control: no-cache` | REQ-CMP-002 AC-2 after the rebuild of `solution.md` 7.1: the browser revalidates the page instead of loading asset names that no longer exist |
| `location /` | `try_files $uri /index.html` (no `$uri/`) | client routes (`web/src/app/AppRoutes.tsx:L16-L44`) work on reload and direct entry; paths under `/api` never reach this location |

Vite writes hashed files under `dist/assets/` by default; `web/vite.config.ts:L5-L11` does not change `build`.

### 3.4 Compose — `docker-compose.yml` (REQ-CMP-002..006, NFR-101, NFR-105, NFR-106)

| Element | Value |
|---|---|
| top-level `name` | `todo-app`, so the volume the README names (REQ-DOC-004 AC-3, NFR-107) is `todo-app_todo-data` whatever the checkout's folder is called |
| service `api` | `build.context: ./api`; `environment`: `TODO_DB_PATH=/data/todo.db`, `TODO_COOKIE_SECURE=false`, `SERVER_PORT=8080` as literal values; `ports`: `"127.0.0.1:8080:8080"` only; `volumes`: `todo-data:/data` |
| `api.healthcheck` | `test` (`CMD-SHELL`): curl, silent, body discarded, `--max-time 3`, writing only the HTTP status code of `GET http://127.0.0.1:8080/api/v1/auth/session`, piped to `grep -qx 401`, so the check passes only on exactly 401 and fails on any other status or on no answer (curl writes `000`); `interval: 10s`, `timeout: 5s`, `retries: 3`, `start_period: 120s` |
| service `web` | `build.context: ./web`; `ports`: `"127.0.0.1:5173:5173"` only; `depends_on.api`: `condition: service_healthy`, `restart: true` |
| top-level `volumes` | `todo-data: {}` (Compose names it `todo-app_todo-data`) |
| networks | the project's default network only |

Rules for the file: no `$` anywhere (so no interpolation and no `$$` escape is needed; the healthcheck uses curl's
`%{http_code}`, not shell variables), no `env_file`, no `image:` names, no registry, no `platform`/`platforms`, no
`deploy`, no other published port. A `.env` copied from `.env.example` therefore changes nothing in the containers
(REQ-CMP-004 AC-1). `restart: true` serves REQ-CMP-002 AC-1 and AC-3 when `api` is recreated alone (ADR-0020,
consequences; risk R5) and needs Docker Compose 2.17 or later (README prerequisite; Assumption A6).

Start-up (`solution.md` 4, flow 3): Compose builds missing images for the machine's own architecture; `api` starts,
creates `/data/todo.db`, runs the Flyway migration (CHG-0001 ADR-0014) and only then starts its HTTP server (Spring
Boot starts the embedded server after the application context, including Flyway, is ready; Assumption A8); the first
probe answered
401 marks it healthy; `web` starts and publishes 5173. If `api` is not healthy within the start period plus three
failed probes, Compose reports it unhealthy and does not start `web`.

### 3.5 Smoke check — `web/scripts/compose-smoke.mjs` (REQ-SMK-001 AC-1..AC-5)

A dependency-free Node ES module, run from the repository root with `node web/scripts/compose-smoke.mjs` (no
`pnpm install` needed; Node's built-in `fetch`). Base URL: environment variable `TODO_SMOKE_BASE_URL`, default
`http://localhost:5173` (P3); it must be an absolute `http:` or `https:` URL, trailing `/` removed.

Behaviour: two `GET` requests, always both, each with a 10-second timeout, `redirect: 'manual'`, no cookie, no
`Authorization` header and no body:

1. **page**: `<base>/`. Passes only on status 200 with media type `text/html` (parameters such as `charset` ignored).
2. **api**: `<base>/api/v1/todos` (the path of `solution.md` 4, flow 4; protected, `SecurityConfig.java:L56`). Passes
   only on status 401 with media type `application/problem+json`, parameters ignored (`common-problem-v1.yaml:L21-L26`,
   `L70-L79`). A 200 HTML fallback, a redirect, a 502 or 504, or no answer fails.

It prints one line per request: `PASS` or `FAIL`, the check name, the URL, and what it received (status and media type,
or "no answer" with the error's code or message), and for a failure what it expected. Exit code: 0 when both pass, 1
when either fails, 2 when `TODO_SMOKE_BASE_URL` is invalid. It runs `main` only when executed directly, not when the
test imports it. It writes no file and creates no data; it makes no Playwright call.

Lint: `pnpm -C web lint` checks `.mjs` files with no Node or browser globals defined (`web/eslint.config.js:L4-L8`),
so every global the script uses that is not an ECMAScript built-in (for example `process`, `URL`, `console`, `fetch`,
`AbortSignal`, timers) is imported from its `node:` module or reached through `globalThis`; no package and no
lint-configuration change. Typing: the test (TypeScript, `web/vitest.config.ts:L12`) imports the `.mjs`, and
`tsc --noEmit` needs its types (`web/tsconfig.json:L16`), so `web/scripts/compose-smoke.d.mts` declares them:

```ts
// web/scripts/compose-smoke.d.mts — signatures only
export type Env = Readonly<Record<string, string | undefined>>;
export interface SmokeCheckResult { name: 'page' | 'api'; url: string; ok: boolean; detail: string; }
export interface SmokeReport { ok: boolean; checks: [SmokeCheckResult, SmokeCheckResult]; }
export declare const DEFAULT_BASE_URL: 'http://localhost:5173';
/** Base URL from TODO_SMOKE_BASE_URL or the default; throws a TypeError naming the variable when invalid. */
export declare function resolveBaseUrl(env: Env): string;
/** Runs both checks with the given fetch; never throws for a network failure (it becomes a failed check). */
export declare function runSmokeCheck(baseUrl: string, fetchImpl: typeof fetch, timeoutMs?: number): Promise<SmokeReport>;
/** Resolves the base URL, runs the checks, writes one line per check; resolves to the exit code 0, 1 or 2. */
export declare function main(env: Env, fetchImpl: typeof fetch, write: (line: string) => void): Promise<number>;
```

### 3.6 `.env.example` and `.gitignore` (REQ-DOC-002)

`.env.example` (root) holds exactly three settings with their native defaults, `TODO_DB_PATH=./data/todo.db`,
`TODO_COOKIE_SECURE=false`, `SERVER_PORT=8080` (`api/src/main/resources/application.properties:L3-L6`), and comments
saying: the native run loads no `.env` file, so the person exports the variables in the shell before starting the API
from `api/` (the relative path is resolved from there); Docker Compose does not need it, and `docker-compose.yml` uses no
variables, so a `.env` changes nothing in the stack; keep `SERVER_PORT=8080`, which the SPA's dev proxy expects
(`web/vite.config.ts:L9`); no secret is needed or allowed in this file.

`.gitignore` gains the pattern `.env`, under a comment line naming REQ-DOC-002 AC-4 (the pattern matches `.env` in
any directory and not `.env.example`). The existing harness
lines (`.gitignore:L1-L7`) stay.

### 3.7 README — `README.md` (REQ-DOC-001, 003, 004; REQ-CMP-006 AC-2; REQ-CMP-007 AC-2; REQ-SMK-001 AC-5)

The title line stays; sections, in order:

1. **Prerequisites.** JDK 21; Node.js current LTS (`web/.nvmrc` is `lts/*`); pnpm 10, through corepack, which Node.js
   25 and later no longer bundle (A7): `npm install -g corepack@0.34.0`, then `corepack enable pnpm`, then
   `corepack install -g pnpm@10.34.8` (the same pins as the web image's build stage, 3.2; `pnpm --version` then shows
   10.34.8); the Playwright browsers, only for
   the e2e: `pnpm -C web exec playwright install` (Chromium, Firefox and WebKit, `web/playwright.config.ts:L27-L29`;
   on Linux add `--with-deps`); for the Compose run only, Docker with Docker Compose 2.17 or later, and Node.js on the
   host for the smoke check (item 5), even when the native run is not used (pnpm and `pnpm install` are not needed
   for it).
2. **Native run** (CHG-0001 `sdd.md:L603-L611`). API: `cd api && ./mvnw spring-boot:run`, answers on
   `http://localhost:8080`; SPA: `pnpm -C web install`, then `pnpm -C web dev`, opens on `http://localhost:5173`. No
   cloud service, hosted database or email service. On first start the API creates `api/data/todo.db` (it is started
   from `api/`) and runs its migration. Settings: defaults work; to change one, export it in the shell (see
   `.env.example`; nothing loads a `.env`). It never mentions the test-only clock, the `e2e` profile or
   `TODO_E2E_CLOCK_OFFSET_FILE`.
3. **Tests**, each run from the root: `./api/mvnw -f api/pom.xml test`; `pnpm -C web test`; `pnpm -C web e2e`. The e2e
   runs on the native code only, never against the Compose stack; it starts its own API and SPA on a temporary
   database, so ports 8080 and 5173 must be free: stop the native run and the Compose stack first (otherwise it fails
   with "Port 8080 is already in use", `web/e2e/support/api-process.ts:L33`).
4. **Compose run.** `docker compose up` from the root (builds both images the first time; after pulling or changing
   code, `docker compose up --build`); `docker compose up -d --wait` runs it in the background and returns when the API
   is healthy. Open `http://localhost:5173`. Ports: 5173 (app) and 8080 (API, for `curl` and debugging), both bound to
   `127.0.0.1` only. Stop: `docker compose down` (data kept). Data lives on the named volume `todo-app_todo-data`, which
   survives `down` and is removed by `docker compose down -v`, which deletes the person's own to-dos. Logs:
   `docker compose logs`. The stack is plain http on localhost with `TODO_COOKIE_SECURE=false`; any hosting would need
   HTTPS and `TODO_COOKIE_SECURE=true`. Playwright is not run against the stack: check it with the smoke check and
   your own use of the app (UAT). Cleanup: `docker compose down --rmi local` removes the built images (add `-v` to
   remove the data too).
5. **Smoke check.** Needs the stack already running and Node.js on the host; `node web/scripts/compose-smoke.mjs`; another address with
   `TODO_SMOKE_BASE_URL=http://127.0.0.1:5173 node web/scripts/compose-smoke.mjs`; exit 0 means both checks passed.
6. **Both architectures (proof build).** `docker buildx build --platform linux/amd64,linux/arm64 --output type=cacheonly
   api` and the same with `web`: builds both, loads nothing and pushes nothing; it is the proof that both build and that
   nothing is published. Prerequisites: a builder that supports multi-platform builds (Docker's containerd image store
   turned on, or a `docker-container` builder made with `docker buildx create --use`) and emulation for the other
   architecture (included in Docker Desktop; on Linux, QEMU through binfmt). Plain `docker compose up` builds only for
   the machine's own architecture.
7. **Wiping data.** Native: stop the API and delete `api/data/todo.db` (or the file a custom `TODO_DB_PATH` names).
   Compose: `docker compose down -v`. Both delete the person's own to-dos.
8. **Troubleshooting**, each with symptom and fix: port 5173 or 8080 in use (Vite moves to another port and says so;
   Spring Boot stops with "Port 8080 was already in use"; Compose says the port "is already allocated": stop the other
   run or find the process with `lsof -i :8080`); Docker not running ("Cannot connect to the Docker daemon": start
   Docker); the API never healthy or the app answers 502 on `/api` (`docker compose logs api`, then
   `docker compose restart web`); an old volume the API cannot write (`docker compose down -v`, which deletes data). Exact messages are copied from the tools when written
   (P4).

## 4. Answers to `solution.md` section 9

| Question | Answer | Reason and check |
|---|---|---|
| Non-root nginx: listen port, pid, writable directories | `USER nginx` (uid 101); `listen 5173`; `pid /tmp/nginx.pid`; all five temp paths under `/tmp/`; own main `nginx.conf` with no `user` directive; `conf.d/default.conf` removed, so the image's IPv6 entrypoint script finds nothing to edit | ports above 1023 need no privilege; `/tmp` is writable by any user. Checked by the container starting, `docker compose exec web id -u` (not 0) and the smoke check (Assumption A5) |
| Healthcheck tool and endpoint | `curl` in the `eclipse-temurin:21-jre-noble` image, guarded at build time; `GET /api/v1/auth/session` on `127.0.0.1:8080`, healthy only on 401 | the contract calls 401 the normal answer without a session (`identity-api-v1.yaml:L139-L158`); the path is protected (`SecurityConfig.java:L56`); with no cookie the session filter does no database lookup (`SessionCookieAuthenticationFilter.java:L40-L53`); no new endpoint, dependency or API change (Assumption A2) |
| Database path, volume mount, non-root write | `TODO_DB_PATH=/data/todo.db`; named volume `todo-data` on `/data`; `/data` made in the image, owned by uid/gid 10001, which a new named volume inherits (Assumption A9) | the API creates the parent directory and file (`DatabaseConfig.java:L23-L28`); WAL siblings need the directory writable (`L35`). Checked by the smoke check on a fresh volume and UAT (ADR-0019) |
| Smoke-check path, override, command | `web/scripts/compose-smoke.mjs`; `TODO_SMOKE_BASE_URL`, default `http://localhost:5173`; `node web/scripts/compose-smoke.mjs` | decision 1 (D1); section 3.5 |
| Cookie, Set-Cookie, X-Todo-Client unchanged; no `/api` rewrite | `proxy_pass http://api:8080` with no URI part; no header, cookie or rewrite directives | nginx passes request headers except `Host`/`Connection` and response headers except `Date`, `Server` and `X-Accel-*` by default; the cookie has no `Domain` (`AuthController.java:L61-L69`) and the API reads neither `Host` nor forwarded headers (no use of them in `api/src/main`), so the rewritten `Host` does not matter; the API expects the `/api` prefix (`SecurityConfig.java:L56`). Checked by the smoke check (401 problem+json via the proxy) and UAT (login kept, to-do created) |
| No query strings in nginx logs | path-only access-log format (section 3.3); error log at `emerg` | `$request`, `$request_uri`, `$args`, `$query_string` and `$http_referer` all carry `?q=`; checked by inspection and by UAT: search, then `docker compose logs web` shows no `q=` |
| `index.html` fallback, never for `/api` | `try_files $uri /index.html` in `location /` only; `/api` and `/api/` proxied; `/assets/` misses are 404 | checked by the smoke check (a 200 HTML on `/api` fails) and UAT (reload on `/todos`) |
| API binds all interfaces in the container | yes: no `server.address` is set (`application.properties:L1-L14`; none anywhere in `api/`) and Compose sets no `SERVER_ADDRESS`, so Spring Boot binds all interfaces | checked by the smoke check through the `web` container and `curl http://127.0.0.1:8080/...` from the host |

The architecture's two section-7.2 assumptions: nginx passes the cookie and the client header by default (row above);
a lockfile is committed (`web/pnpm-lock.yaml:L1`; the API's versions are pinned by `api/pom.xml:L11-L16`, `L23-L29`).

## 5. Error handling

| Failure | What the person sees | Where handled |
|---|---|---|
| Port 5173 or 8080 already in use | Compose fails to bind and names the port | README troubleshooting |
| Docker not running | `docker compose` cannot reach the daemon | README troubleshooting |
| API does not start or cannot write `/data` | `api` unhealthy, `web` not started; cause in `docker compose logs api` | healthcheck (3.4); README |
| `web` cannot reach `api` | `/api` answers 502 or 504; the smoke check fails on `api` and prints the status | nginx default; smoke check |
| Missing asset | 404, not HTML | nginx `/assets/` (3.3) |
| Smoke check against a stopped stack | `FAIL ... no answer (ECONNREFUSED)` for each request, exit 1 | smoke check (3.5) |

## 6. Security and privacy

- Exposure: only `127.0.0.1:5173` and `127.0.0.1:8080` are published (NFR-101); the API-to-web hop stays on the project
  network (`solution.md` 7.2).
- Non-root: API uid 10001, web uid 101 (NFR-103).
- No secrets: no file holds a credential, token, password or registry setting; no build argument carries one; `.env` is
  git-ignored (NFR-102). The native database stays out of build contexts (`api/.dockerignore`).
- Cookie not `Secure` over plain http on localhost, as natively; the README states hosting needs HTTPS (REQ-CMP-006).
- Logs: neither nginx log holds a query string, referrer or cookie; the API's logging rules are unchanged.
- Supply chain: official images with major-line tags (digests out of scope); dependency versions from `api/pom.xml` and
  `web/pnpm-lock.yaml` (frozen); pnpm and corepack pinned exactly.

## 7. Observability

Unchanged for the API (CHG-0001 `sdd.md` section 10): JSON lines on stdout, read with `docker compose logs api`. The
healthcheck adds one API log line every 10 seconds (`GET` on the session route, status 401). The `web` container
writes its path-only access log and its start-up failures to stdout (`docker compose logs web`). Health status:
`docker compose ps`. No metrics or tracing.

## 8. Migration and rollout

None: no data migration and no change to the native run. The first `docker compose up` creates the volume and the
database. Images are built locally and never published.

## 9. Test approach and verification plan

Tests first: the React tests node writes `web/tests/scripts/compose-smoke.test.ts` (red), the React build node writes
the script (green). The test injects a fake `fetch` (no network), names each case with its requirement ID and AC, and
covers: both pass (also with `charset` parameters); `/api` answers 200 HTML; 502; 302; a network error; `/api`
answers 401 with a media type other than `application/problem+json` (`application/json`, and `text/html`) (each
failing the `api` check with its received value printed); the page failing alone; the page answering 200 with a media
type other than `text/html` (`application/json`), failing the `page` check with the status and media type it received
printed; exactly two calls, both `GET`, with no
`Cookie` or `Authorization` header and no body; the default and the overridden base URL; an invalid base URL giving
exit code 2. The Java stack adds no test: no behaviour in `api/src` changes. Files that configure rather than behave
(Dockerfiles, nginx, Compose, README) are verified by building them, by the live checks below and by inspection.

Live checks run on the change's head commit by the agents when Docker is available to them, otherwise by the person;
each record names who ran it, the commit, the date and the output (REQ-SMK-001 AC-6, AC-7; the form, a PR comment or
an evidence note, is the approver's choice, P5). Abbreviations: **I** inspection, **B** image build (`docker compose
build`), **S** smoke-check run, **X** buildx proof run, **U** the person's UAT, **T** automated test, **C** a command
run against the stack. Contract element: none for every row (no interface is added; the smoke check and the
healthcheck call CHG-0001 operations as their contracts define them).

| AC | Design | Verified by |
|---|---|---|
| REQ-DOC-001 AC-1 | 3.7 item 2 | U (native: sign up, log in, create a to-do); I against CHG-0001 `sdd.md:L603-L611` |
| REQ-DOC-001 AC-2 | 3.7 item 1 | I |
| REQ-DOC-001 AC-3 | 3.7 item 2 | I |
| REQ-DOC-001 AC-4 | 3.7 item 2 | I (no "clock", "e2e profile", `TODO_E2E_CLOCK_OFFSET_FILE` in README) |
| REQ-DOC-002 AC-1 | 3.6 | I |
| REQ-DOC-002 AC-2 | 3.6 | I |
| REQ-DOC-002 AC-3 | 3.6, 6 | I of `.env.example`, `docker-compose.yml`, both Dockerfiles, README; `git ls-files` shows no `.env` |
| REQ-DOC-002 AC-4 | 3.6 | I; `git check-ignore .env` matches and `git check-ignore .env.example` does not |
| REQ-DOC-003 AC-1 | 3.7 item 3 | I against `web/package.json:L14`, `L16` and the e2e's `./api/mvnw -f api/pom.xml` pattern (`web/e2e/support/api-process.ts:L35`, `web/e2e/support/launch.ts:L35`) |
| REQ-DOC-003 AC-2 | 3.7 items 3, 4 | I |
| REQ-DOC-003 AC-3 | 3.7 item 7 | I |
| REQ-DOC-003 AC-4 | 3.7 item 8 | I |
| REQ-DOC-003 AC-5 | 3.7 item 3 | I against `api-process.ts:L33` |
| REQ-CMP-001 AC-1 | 3.1 | I; B |
| REQ-CMP-001 AC-2 | 3.2 | I; B |
| REQ-CMP-001 AC-3 | 3.1, 3.2 | I (four `FROM` lines: explicit, not `latest`, major line named, official multi-arch) |
| REQ-CMP-001 AC-4 | 3.1, 3.2 | C: `docker compose exec api id -u` gives 10001; `docker compose exec web id -u` gives a value other than 0 (expected 101, A5) |
| REQ-CMP-002 AC-1 | 3.4 | S after `docker compose up` on a fresh checkout; C: `docker compose ps` shows both running, `api` healthy |
| REQ-CMP-002 AC-2 | 3.3, 3.4 | U (sign up, log in, create a to-do, log out on `http://localhost:5173`) |
| REQ-CMP-002 AC-3 | 3.4, ADR-0019 | U (`down`, `up`, to-dos still listed) |
| REQ-CMP-003 AC-1 | 3.4 | I; C: `docker compose port web 5173` and `docker compose port api 8080` show `127.0.0.1` |
| REQ-CMP-003 AC-2 | 3.3, ADR-0020 | S (401 problem+json through `:5173/api`) |
| REQ-CMP-003 AC-3 | 3.3 | U (log in, reload `/todos`, list still shown) |
| REQ-CMP-003 AC-4 | 3.4 | C: `curl -i http://127.0.0.1:8080/api/v1/todos` answers 401 |
| REQ-CMP-004 AC-1 | 3.4, ADR-0019 | I (named volume, no bind mount, literal values, no `$`, no `env_file`) |
| REQ-CMP-004 AC-2 | ADR-0019 | U |
| REQ-CMP-004 AC-3 | 3.1, 3.4 | S on a fresh volume (`down -v` first) reaching a healthy API; U creating a to-do |
| REQ-CMP-004 AC-4 | 3.4 | U (`down -v`, `up`, list empty) |
| REQ-CMP-005 AC-1 | 3.4 | I |
| REQ-CMP-005 AC-2 | 3.4 | I; C: `docker compose events` (or `up` output) shows the `web` container started only after `api` reported healthy |
| REQ-CMP-005 AC-3 | 3.4 | C trial: an uncommitted override `.harness/tmp/compose-unhealthy.yml` replaces the `api` probe with the same probe on closed port 9 and `start_period: 0s`; `docker compose -f docker-compose.yml -f .harness/tmp/compose-unhealthy.yml up -d api`; after at least 40 s `docker compose ps api` shows running and `unhealthy`; then `down` |
| REQ-CMP-006 AC-1 | 3.4 | I |
| REQ-CMP-006 AC-2 | 3.7 item 4 | I |
| REQ-CMP-007 AC-1 | 3.1, 3.2 | U (stack built and used on the machine at hand, its architecture recorded) |
| REQ-CMP-007 AC-2 | 3.7 item 6 | I |
| REQ-CMP-007 AC-3 | 3.7 item 6 | X for `api` and for `web`, both complete; record names who, commit, machine |
| REQ-DOC-004 AC-1 | 3.7 item 4 | I |
| REQ-DOC-004 AC-2 | 3.7 items 4, 5 | I |
| REQ-DOC-004 AC-3 | 3.7 item 4 | I |
| REQ-SMK-001 AC-1 | 3.5 | I |
| REQ-SMK-001 AC-2 | 3.5 | T (section 9's cases, including the right status with the wrong media type on each request); S |
| REQ-SMK-001 AC-3 | 3.5 | T (section 9's cases, including `/api` 401 that is not `application/problem+json`); C: run with the stack stopped exits non-zero and names both requests |
| REQ-SMK-001 AC-4 | 3.5 | T; I |
| REQ-SMK-001 AC-5 | 3.7 item 5 | I |
| REQ-SMK-001 AC-6 | 3.5, 9 | S on the head commit, recorded |
| REQ-SMK-001 AC-7 | 9 | U on the head commit, recorded: native sign up/log in/create; Compose sign up, log in, create, reload, log out, `down`/`up` kept, `down -v`/`up` empty |
| NFR-101 AC-1 | 3.4 | I (two `ports` entries, both `127.0.0.1`); C as REQ-CMP-003 AC-1 |
| NFR-102 AC-1 | 6 | I as REQ-DOC-002 AC-3, plus no registry or credential in any build argument |
| NFR-103 AC-1 | 3.1, 3.2 | C as REQ-CMP-001 AC-4 |
| NFR-104 AC-1 | 3.7 item 6 | X; I (no file configures a registry, publish step or credentials) |
| NFR-105 AC-1 | ADR-0019 | U as REQ-CMP-004 AC-2 |
| NFR-106 AC-1 | 3.4 | C with the S run: on a warm machine, the time from the `api` container's start to its healthy status in `docker compose events` (or the elapsed time of `docker compose up -d --wait`) is at most 120 s, then S passes |
| NFR-107 AC-1 | 3.7 | I: every command, script name, service name and path in the README exists |

## 10. Assumptions

| ID | Assumption | Checked by |
|---|---|---|
| A1 | `eclipse-temurin:21-jdk-noble`, `21-jre-noble`, `node:26-slim` and `nginx:stable-alpine` are published for linux/amd64 and linux/arm64 | X |
| A2 | The `eclipse-temurin` JRE 21 Ubuntu image ships `curl` (and `grep`, `sh`) | the build-time `curl --version` step (B) |
| A3 | Node 26 is the current Active LTS line on 2026-11-13 (the line the requester chose, Q14 (a)) | B, uncached with `--progress=plain` (`node --version` in the build log shows v26), and Node.js's published release schedule |
| A4 | `web/pnpm-lock.yaml` was written by pnpm 10 and is accepted by pnpm 10.34.8 | `pnpm install --frozen-lockfile` in B |
| A5 | The `nginx:stable-alpine` image has user `nginx` (uid 101) and its entrypoint scripts skip a missing `conf.d/default.conf` | B, container start, `id -u` |
| A6 | Docker Compose 2.17 or later (the first with `depends_on.restart`), with BuildKit, on the person's machine (rootless Docker and Podman are not covered) | README prerequisite; U |
| A7 | Node.js 25 and later no longer bundle corepack, so `node:26-slim` may lack it | B (the npm install makes it present either way) |
| A8 | Spring Boot opens its HTTP port only after the context, including the Flyway migration, is ready, so a 401 means migrated | S on a fresh volume, U creating a to-do |
| A9 | A new, empty named volume takes the owner and mode of the image directory it is mounted on, at its first use | S on a fresh volume (healthy API, to-do created in U) |
| A10 | nginx logs request-related errors (with request line and referrer) at `error` or below, and start-up failures at `emerg` | I of a start with a broken config during build-node work; UAT: search, then no `q=` in `docker compose logs web` |
| A11 | corepack 0.34.0 is a published npm release that runs on Node 26 and can activate pnpm 10.34.8 (not verifiable from the repository) | B: the npm install fails if the version is not published, and `pnpm install --frozen-lockfile` fails if pnpm is not activated (A4); supplementary, an uncached B with `--progress=plain` shows `corepack --version` 0.34.0 and `pnpm --version` 10.34.8 in the build log; the README's host steps use the same pins |

## 11. Risks

| ID | Risk | Mitigation |
|---|---|---|
| R1 | No build node may own root files today (a known harness limitation, being fixed by the requester in parallel); `README.md`, `.env.example`, `docker-compose.yml` and `.gitignore` need an owning node in the plan | stays at the root as specified (decision 3); not designed around here |
| R2 | A base image drops `curl` | the build fails at the check step (A2); a new design decision is then needed |
| R3 | Node or corepack change (A3, A7, A11) | corepack pinned and installed explicitly; `--frozen-lockfile` |
| R4 | A named volume created earlier with another owner is not writable | README troubleshooting: `down -v` (deletes data) |
| R5 | nginx keeps the old `api` address after `api` is recreated alone | `depends_on.api.restart: true`; `docker compose restart web` as a fallback in the README |
| R6 | nginx runtime errors are not logged (error log at `emerg`) | the access log keeps status codes; the API log has the cause |
| R7 | `localhost` resolving to `::1` first while ports are published on `127.0.0.1` only | browsers and Node fall back to IPv4; the README's smoke override example uses `127.0.0.1` |
| R8 | First builds and the emulated multi-arch proof are slow; agents may have no Docker | NFR-106 covers a warm machine only; REQ-SMK-001 AC-6 lets the person run the checks |
| R9 | `pnpm -C web e2e` would reuse a Compose SPA on 5173 (`web/playwright.config.ts:L35-L42`) | the README says to stop the stack first; the e2e's port-8080 check stops it anyway |
| R10 | Major-line tags move; a rebuild may pull a newer patch | accepted (digests out of scope) |

## 12. For the approver at freeze

- **Node image: decided by the requester (Q14, `.harness/changes/CHG-0005/clarifications.md:L180-L188`).**
  REQ-CMP-001 AC-2 asks for "Node's current LTS major and pnpm through corepack", and Node 25 and later no longer
  bundle corepack. The requester accepted (a): `node:26-slim` (the current LTS line) with an exactly pinned corepack
  installed through npm in the build stage only; it adds one build-time tool and nothing at run time. Not chosen:
  (b) `node:24-slim`, an older LTS line that still bundles corepack, is not the current LTS; (c) `node:22-slim`,
  matching the harness tool pin `.github/harness/versions.env:L46`, is older still. No open question remains.
- The pnpm pin 10.34.8 and the corepack pin 0.34.0 (3.2, A11), the healthcheck timings (3.4: 10 s interval, 120 s start
  period, matching P1's 120 s), the error log at `emerg` (3.3), and uid 10001 for the API user are engineering defaults
  confirmed by approving the freeze.
