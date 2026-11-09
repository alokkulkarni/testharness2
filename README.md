# testharness2

A to-do list app: a Spring Boot API (`api/`) and a React single-page app (`web/`).
Specification for the first change: [docs/specs/CHG-0001/](docs/specs/CHG-0001/).

## Prerequisites

- JDK 21
- Node.js, current LTS (the version is pinned in `web/.nvmrc`)
- pnpm

## Run locally

Start the API (runs on http://localhost:8080):

```bash
cd api && ./mvnw spring-boot:run
```

On first start the API creates the database file at `TODO_DB_PATH` (default `./data/todo.db`, relative to `api/`)
and its parent directory, then runs the Flyway migration. `api/data/` is git-ignored.

In a second terminal, start the web app (runs on http://localhost:5173, proxying `/api` to port 8080):

```bash
pnpm -C web install
pnpm -C web dev
```

The settings and their example values are listed in `.env.example`. No secrets are needed.
Nothing here uses a cloud service, a hosted database, an email service or a deployment.

## Tests

```bash
cd api && ./mvnw test     # API tests
pnpm -C web test          # web unit and component tests
pnpm -C web e2e           # end-to-end tests (Playwright)
```
