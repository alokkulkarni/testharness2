package app.todo.auth;

import java.time.Instant;
import java.util.Optional;

/**
 * Package-private store of the {@code sessions} table (ADR-0009). Rows hold only the SHA-256 of the session token
 * (ADR-0004), never the token itself.
 */
interface SessionRepository {

    void insert(String tokenHash, long userId, Instant createdAt, Instant expiresAt);

    Optional<SessionRow> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    /** A stored session: its owner and the instant it ends. */
    record SessionRow(long userId, Instant expiresAt) {}
}
