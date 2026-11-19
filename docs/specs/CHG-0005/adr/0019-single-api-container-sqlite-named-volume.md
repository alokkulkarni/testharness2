# ADR-0019 One API container writing SQLite on a named volume

Status: proposed (CHG-0005, N03-spec). Requirements: REQ-CMP-004 AC-1..AC-4, NFR-105, REQ-CMP-002 AC-3. Asked for by
`docs/architecture/solution.md` section 8 (the "single API container with SQLite on a named volume" row) and section 9
(the one-writer risk row). Numbered after CHG-0001's ADR-0001..0018 so that every ADR number is unique in the
repository.

## Context

The API keeps all data in one SQLite file at `TODO_DB_PATH` (CHG-0001 ADR-0003), through a pool of exactly one
connection with a WAL journal (CHG-0001 ADR-0016; `api/src/main/java/app/todo/config/DatabaseConfig.java:L22-L39`).
SQLite allows one writer, and WAL writes `-wal` and `-shm` files beside the database file, so the whole directory must
be writable by the API process. In the Compose stack the to-dos must survive `docker compose down` and go away with
`docker compose down -v` (REQ-CMP-004 AC-2, AC-4). The API container runs as a non-root user (NFR-103). The app has
one local user (CHG-0001 NFR-002).

## Decision

- `docker-compose.yml` declares exactly one `api` service with no `deploy.replicas`, no scaling, and nothing else that
  opens the database: the API is the file's only writer, as in the native run.
- The SQLite file is `/data/todo.db` in the container (`TODO_DB_PATH=/data/todo.db`, a literal value). The directory
  `/data` is a **named volume** (`todo-data`) declared in the same file; no bind mount of the checkout.
- The API image creates `/data` and gives it to the non-root API user (fixed uid and gid 10001). A new, empty named
  volume takes the ownership and mode of the image directory it is mounted on when it is first used (SDD Assumption
  A9), so the API user can create the file and its WAL siblings with no manual step.
- No database server or other store is added.

## Consequences

- Data survives `docker compose down` and container rebuilds; `docker compose down -v` deletes it (README warns).
- The volume's ownership is set only when the volume is first populated. A volume left over from an image with a
  different uid would not be writable; the cure is `docker compose down -v` (it deletes data, so the README says so).
- Running a second API replica, or another container on the same volume, is not supported; nothing in the stack does.
- The native run's database (`api/data/todo.db`) and the stack's database are separate; neither sees the other's data.

## Alternatives

- **Bind mount of `api/data/`**: shares the native file, but the container's uid rarely matches the host user's on
  Linux (write failures), and SQLite's WAL locking over Docker Desktop's file sharing on macOS is not reliable; it also
  mixes the two runs' data. Rejected.
- **A database server container (for example PostgreSQL)**: a new store and a code change, contrary to CHG-0001
  ADR-0003 and the "no API change" scope. Rejected.
- **Several API replicas**: SQLite has one writer and the pool one connection (ADR-0016); no load needs it. Rejected.
- **No volume (file in the container layer) or tmpfs**: data lost on every recreate, failing NFR-105. Rejected.
