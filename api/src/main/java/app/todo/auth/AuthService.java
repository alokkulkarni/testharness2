package app.todo.auth;

import app.todo.common.BodyReader;
import app.todo.common.FieldError;
import app.todo.common.Whitespace;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Sign-up, login and the current session (sdd.md section 4). Password hashing never runs inside a transaction. */
@Service
class AuthService {

    private static final Set<String> CREDENTIAL_MEMBERS = Set.of("identifier", "password");

    private final UserRepository users;
    private final SessionService sessions;
    private final PasswordHasher hasher;
    private final Clock clock;

    AuthService(UserRepository users, SessionService sessions, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.hasher = hasher;
        this.clock = clock;
    }

    /** Creates the account; does not start a session (REQ-AUTH-001 AC-1). Returns the identifier as stored. */
    String signUp(Map<String, Object> request) {
        BodyReader body = new BodyReader(request, CREDENTIAL_MEMBERS);
        String identifier = body.string("identifier");
        String password = body.string("password");

        String trimmed = null;
        if (!body.hasErrorFor("identifier")) {
            if (identifier == null) {
                body.add("identifier", FieldError.Code.REQUIRED, "Enter an email address or a username.");
            } else {
                trimmed = Whitespace.trim(identifier);
                if (trimmed.isEmpty()) {
                    body.add("identifier", FieldError.Code.BLANK, "Enter an email address or a username.");
                } else {
                    IdentifierRules.validate(trimmed, body);
                }
            }
        }
        if (!body.hasErrorFor("password")) {
            if (password == null) {
                body.add("password", FieldError.Code.REQUIRED, "Enter a password.");
            } else {
                PasswordRules.validate(password, body);
            }
        }
        body.failIfInvalid();

        String hash = hasher.hash(password);
        if (!users.insert(trimmed, IdentifierRules.keyOf(trimmed), hash, clock.instant())) {
            throw new IdentifierTakenException();
        }
        return trimmed;
    }

    /** Checks the credentials and starts a session (REQ-AUTH-002). */
    LoggedIn logIn(Map<String, Object> request) {
        BodyReader body = new BodyReader(request, CREDENTIAL_MEMBERS);
        String identifier = body.string("identifier");
        String password = body.string("password");

        String trimmed = "";
        if (!body.hasErrorFor("identifier")) {
            trimmed = identifier == null ? "" : Whitespace.trim(identifier);
            if (trimmed.isEmpty()) {
                body.add("identifier", identifier == null ? FieldError.Code.REQUIRED : FieldError.Code.BLANK,
                        "Enter your email address or username.");
            }
        }
        if (!body.hasErrorFor("password") && (password == null || password.isEmpty())) {
            body.add("password", FieldError.Code.REQUIRED, "Enter your password.");
        }
        body.failIfInvalid();

        // a password over 128 characters is refused like any wrong password, without hashing, for every identifier alike
        if (PasswordRules.length(password) > PasswordRules.MAX_LENGTH) {
            throw new InvalidCredentialsException();
        }
        Optional<UserRepository.UserRow> user = users.findByIdentifierKey(IdentifierRules.keyOf(trimmed));
        if (user.isEmpty()) {
            hasher.verifyAgainstDummy(password);
            throw new InvalidCredentialsException();
        }
        if (!hasher.matches(password, user.get().passwordHash())) {
            throw new InvalidCredentialsException();
        }
        SessionService.IssuedSession session = sessions.issue(user.get().id());
        return new LoggedIn(user.get().identifier(), session);
    }

    /** The identifier as stored at sign-up, for {@code getSession} (REQ-AUTH-002 AC-4). */
    String identifierOf(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("A session refers to a user that does not exist"))
                .identifier();
    }

    void logOut(String token) {
        if (token != null) {
            sessions.end(token);
        }
    }

    record LoggedIn(String identifier, SessionService.IssuedSession session) {}
}
