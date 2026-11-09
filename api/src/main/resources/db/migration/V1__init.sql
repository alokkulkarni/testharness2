-- V1: the whole schema (sdd.md section 3; ADR-0014, ADR-0015, ADR-0017, ADR-0018).
-- Table owners: users and sessions belong to the identity module (app.todo.auth), todos to the to-dos module
-- (app.todo.todo), flyway_schema_history to the composition root (app.todo.config).
-- Timestamps are UTC text yyyy-MM-ddTHH:mm:ss.SSSZ (text order = time order); due_date is YYYY-MM-DD text.

CREATE TABLE users (
    id             INTEGER PRIMARY KEY,
    identifier     TEXT NOT NULL,
    identifier_key TEXT NOT NULL UNIQUE,
    password_hash  TEXT NOT NULL,
    created_at     TEXT NOT NULL
);

CREATE TABLE sessions (
    token_hash TEXT PRIMARY KEY,
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE INDEX ix_sessions_expires_at ON sessions (expires_at);

CREATE TABLE todos (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    public_id          TEXT NOT NULL UNIQUE,
    user_id            INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title              TEXT NOT NULL,
    title_search       TEXT NOT NULL,
    description        TEXT NULL,
    description_search TEXT NULL,
    due_date           TEXT NOT NULL,
    priority           TEXT NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    done               INTEGER NOT NULL CHECK (done IN (0, 1)),
    created_at         TEXT NOT NULL,
    updated_at         TEXT NOT NULL
);

CREATE INDEX ix_todos_user_created ON todos (user_id, created_at DESC, id DESC);
CREATE INDEX ix_todos_user_done_created ON todos (user_id, done, created_at DESC, id DESC);
