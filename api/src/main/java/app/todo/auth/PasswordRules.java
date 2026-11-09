package app.todo.auth;

import app.todo.common.BodyReader;
import app.todo.common.FieldError;

/**
 * Sign-up password rules (REQ-AUTH-001 AC-5, AC-11): 8 to 128 Unicode code points, counted as sent. The password is
 * never trimmed or normalised (ADR-0005).
 */
final class PasswordRules {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 128;

    private PasswordRules() {}

    static int length(String password) {
        return password.codePointCount(0, password.length());
    }

    /** Checks a sign-up password that was sent as text; an empty one is {@code REQUIRED}. */
    static void validate(String password, BodyReader body) {
        int length = length(password);
        if (length == 0) {
            body.add("password", FieldError.Code.REQUIRED, "Enter a password.");
        } else if (length < MIN_LENGTH) {
            body.add("password", FieldError.Code.TOO_SHORT, "The password must be at least 8 characters.");
        } else if (length > MAX_LENGTH) {
            body.add("password", FieldError.Code.TOO_LONG, "The password must be 128 characters or fewer.");
        }
    }
}
