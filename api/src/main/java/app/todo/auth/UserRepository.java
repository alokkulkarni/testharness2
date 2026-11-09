package app.todo.auth;

import java.time.Instant;
import java.util.Optional;

/** Package-private store of the {@code users} table (ADR-0009). */
interface UserRepository {

    /** Inserts the account; false when the identifier key is already taken (also for a concurrent duplicate). */
    boolean insert(String identifier, String identifierKey, String passwordHash, Instant createdAt);

    Optional<UserRow> findByIdentifierKey(String identifierKey);

    Optional<UserRow> findById(long userId);

    /** A stored account; the identifier is as stored at sign-up (trimmed, original case). */
    record UserRow(long id, String identifier, String passwordHash) {}
}
