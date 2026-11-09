package app.todo.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Server-side sessions (ADR-0004, ADR-0008): a 256-bit random token from {@code SecureRandom}, base64url in the cookie,
 * and only its SHA-256 hex in the database. A session lasts 8 hours absolute from login; activity never extends it.
 */
@Service
public class SessionService {

    static final Duration LIFETIME = Duration.ofHours(8);

    private final SessionRepository sessions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    SessionService(SessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    /** A new session for the user: the token to put in the cookie and the instant the session ends. */
    IssuedSession issue(long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        Instant expiresAt = now.plus(LIFETIME);
        sessions.insert(hash(token), userId, now, expiresAt);
        return new IssuedSession(token, expiresAt);
    }

    /** The live session for a cookie token. An expired row is deleted and treated as absent. */
    Optional<ActiveSession> resolve(String token) {
        String tokenHash = hash(token);
        Optional<SessionRepository.SessionRow> row = sessions.findByTokenHash(tokenHash);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        if (!row.get().expiresAt().isAfter(clock.instant())) {
            sessions.deleteByTokenHash(tokenHash);
            return Optional.empty();
        }
        return Optional.of(new ActiveSession(row.get().userId(), row.get().expiresAt()));
    }

    /** Ends the session named by the token, if there is one. */
    void end(String token) {
        sessions.deleteByTokenHash(hash(token));
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    record IssuedSession(String token, Instant expiresAt) {}

    record ActiveSession(long userId, Instant expiresAt) {}
}
