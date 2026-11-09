package app.todo.auth;

import app.todo.common.UtcTimestamps;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link UserRepository} over {@code JdbcClient}; its SQL names only {@code users}. */
@Component
class JdbcUserRepository implements UserRepository {

    private static final RowMapper<UserRow> ROW =
            (rs, rowNum) -> new UserRow(rs.getLong("id"), rs.getString("identifier"), rs.getString("password_hash"));

    private final JdbcClient jdbc;

    JdbcUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insert(String identifier, String identifierKey, String passwordHash, Instant createdAt) {
        // ON CONFLICT turns a UNIQUE violation, including a concurrent duplicate, into "no row inserted"
        int inserted = jdbc.sql("INSERT INTO users (identifier, identifier_key, password_hash, created_at) "
                        + "VALUES (:identifier, :key, :hash, :createdAt) ON CONFLICT (identifier_key) DO NOTHING")
                .param("identifier", identifier)
                .param("key", identifierKey)
                .param("hash", passwordHash)
                .param("createdAt", UtcTimestamps.format(createdAt))
                .update();
        return inserted == 1;
    }

    @Override
    public Optional<UserRow> findByIdentifierKey(String identifierKey) {
        return jdbc.sql("SELECT id, identifier, password_hash FROM users WHERE identifier_key = :key")
                .param("key", identifierKey)
                .query(ROW)
                .optional();
    }

    @Override
    public Optional<UserRow> findById(long userId) {
        return jdbc.sql("SELECT id, identifier, password_hash FROM users WHERE id = :id")
                .param("id", userId)
                .query(ROW)
                .optional();
    }
}
