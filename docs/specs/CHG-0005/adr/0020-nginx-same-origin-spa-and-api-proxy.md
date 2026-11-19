# ADR-0020 nginx serves the SPA build and proxies `/api` in Compose

Status: proposed (CHG-0005, N03-spec). Requirements: REQ-CMP-001 AC-2, REQ-CMP-002 AC-2, REQ-CMP-003 AC-2, AC-3,
REQ-SMK-001 AC-2. Asked for by `docs/architecture/solution.md` section 8 (the "same-origin delivery through an nginx
reverse proxy" row). Requester's answer Q1 (2026-11-13): "web to be production build." and "for browser to api in
compose as suggested same origin and session cookie."

## Context

CHG-0001 ADR-0001 puts the page and the API on one origin through the Vite dev server's proxy, which is hard-coded to
`http://localhost:8080` (`web/vite.config.ts:L5-L10`) and cannot reach another container. The session cookie is
`SameSite=Strict`, `HttpOnly`, `Path=/`, with no `Domain` attribute (`api/src/main/java/app/todo/auth/AuthController.java:L61-L69`),
and every unsafe request carries `X-Todo-Client: web` instead of CORS (CHG-0001 ADR-0007; `web/src/api/client.ts:L68`).
The API serves no static files (CHG-0001 ADR-0002) and must not change (requirements, out of scope).

## Decision

- The `web` image builds the SPA with `pnpm build` and serves `dist/` with `nginx:stable-alpine` (the image the
  requester named, Q11), as a non-root user listening on port 5173 inside the container.
- nginx reverse-proxies `/api/` and `/api` to `http://api:8080` with **no URI part** in `proxy_pass`, so the request
  path and query reach the API exactly as the browser sent them, and with no header, cookie or path rewriting
  directives, so `Cookie`, `Set-Cookie` and `X-Todo-Client` pass unchanged.
- Every other path that is not a file answers `index.html` (client routes on reload); `/api` never does.

## Consequences

- nginx becomes a log source in the stack, so its configuration must keep search text in `?q=` out of
  `docker compose logs` (CHG-0001 `sdd.md:L575-L576`): the access log records the path without the query string and
  the error log only start-up failures (SDD section 3.3).
- The browser sees one origin, `http://localhost:5173`, for page and API: no CORS, the cookie works as in the native
  run, and the API is unchanged.
- The stack serves the production build, not the dev server: no hot reload; a code change needs an image rebuild
  (`docker compose up --build`).
- nginx resolves the name `api` when it starts; when the `api` container is recreated, the `web` container must
  restart too (Compose does it through `depends_on.api.restart: true`).
- The native run keeps the Vite proxy; `web/vite.config.ts` is not touched.

## Alternatives

- **Vite dev server in the container with a configurable proxy target** (Q1 (b)): edits the file the native run uses
  and serves a dev build. Rejected by the requester's Q1 answer.
- **The API serves the built SPA** (Q1 (c)): a change to `api/` and to ADR-0002. Rejected.
- **Two origins with CORS**: needs CORS and credentialed requests in the API, against ADR-0007, and an API change.
  Rejected.
- **`nginxinc/nginx-unprivileged`**: non-root by default, but not the image the requester named (Q11 (a)). Rejected;
  the same effect comes from a non-root configuration of `nginx:stable-alpine`.
