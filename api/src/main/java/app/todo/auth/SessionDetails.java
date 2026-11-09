package app.todo.auth;

import java.time.Instant;

/** Kept in the authentication's details by the session filter, so {@code getSession} can return the session's end. */
record SessionDetails(Instant expiresAt) {}
