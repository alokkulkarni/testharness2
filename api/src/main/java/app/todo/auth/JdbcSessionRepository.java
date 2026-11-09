package app.todo.auth;

import app.todo.common.UtcTimestamps;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link SessionRepository} over {@code JdbcClient}; its SQL names only {@code sessions}. */
@Component
class JdbcSessionRepository implements SessionRepository {

    private final JdbcClient jdbc;

    JdbcSessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(String tokenHash, long userId, Instant createdAt, Instant expiresAt) {
        jdbc.sql("INSERT INTO sessions (token_hash, user_id, created_at, expires_at) VALUES (:hash, :userId, :createdAt, :expiresAt)")
                .param("hash", tokenHash)
                .param("userId", userId)
                .param("createdAt", UtcTimestamps.format(createdAt))
                .param("expiresAt", UtcTimestamps.format(expiresAt))
                .update();
    }

    @Override
    public Optional<SessionRow> findByTokenHash(String tokenHash) {
        return jdbc.sql("SELECT user_id, expires_at FROM sessions WHERE token_hash = :hash")
                .param("hash", tokenHash)
                .query((rs, rowNum) -> new SessionRow(rs.getLong("user_id"), UtcTimestamps.parse(rs.getString("expires_at"))))
                .optional();
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        jdbc.sql("DELETE FROM sessions WHERE token_hash = :hash").param("hash", tokenHash).update();
    }
}
