package app.todo.auth;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Argon2id password hashing (ADR-0005): memory 19 MiB (19456 KiB), 2 iterations, parallelism 1, a 16-byte random salt
 * and a 32-byte hash per password, stored as the encoder's self-describing string. The whole password is hashed: no
 * truncation, no pre-hash, no trimming or normalisation.
 */
@Component
class PasswordHasher {

    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    /** Verified against when the identifier is unknown, so the response time does not reveal whether it exists. */
    private final String dummyHash = encoder.encode("dummy-password-for-unknown-identifiers");

    String hash(String password) {
        return encoder.encode(password);
    }

    boolean matches(String password, String hash) {
        return encoder.matches(password, hash);
    }

    void verifyAgainstDummy(String password) {
        encoder.matches(password, dummyHash);
    }
}
