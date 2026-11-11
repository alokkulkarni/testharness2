package app.todo.auth;

import app.todo.common.BodyReader;
import app.todo.common.FieldError;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sign-up identifier rules (sdd.md section 5, REQ-AUTH-001 AC-7..AC-10). The identifier is trimmed with the shared
 * whitespace set before it reaches this class; lengths count Unicode code points.
 */
final class IdentifierRules {

    static final int MIN_LENGTH = 3;
    static final int MAX_LENGTH = 254;

    private static final String LOCAL_SPECIALS = "!#$%&'*+/=?^_`{|}~-.";
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern LABEL = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?");
    private static final Pattern TLD = Pattern.compile("[A-Za-z]{2,}");

    private IdentifierRules() {}

    /** The key used for uniqueness and login lookup: the trimmed identifier lower-cased with {@link Locale#ROOT}. */
    static String keyOf(String trimmedIdentifier) {
        return trimmedIdentifier.toLowerCase(Locale.ROOT);
    }

    /** Checks an already-trimmed sign-up identifier; records at most one error on {@code identifier}. */
    static void validate(String trimmed, BodyReader body) {
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < MIN_LENGTH) {
            body.add("identifier", FieldError.Code.TOO_SHORT,
                    "The email address or username must be 3 to 254 characters.");
        } else if (length > MAX_LENGTH) {
            body.add("identifier", FieldError.Code.TOO_LONG,
                    "The email address or username must be 3 to 254 characters.");
        } else if (trimmed.indexOf('@') >= 0) {
            if (!isValidEmail(trimmed)) {
                body.add("identifier", FieldError.Code.INVALID_FORMAT,
                        "Enter a valid email address, for example name@example.com.");
            }
        } else if (!USERNAME.matcher(trimmed).matches()) {
            body.add("identifier", FieldError.Code.INVALID_FORMAT,
                    "A username can contain only letters A to Z, digits 0 to 9, and the characters . _ -");
        }
    }

    /** The ASCII-only email rule of sdd.md section 5. */
    static boolean isValidEmail(String value) {
        int at = value.indexOf('@');
        if (at < 0 || at != value.lastIndexOf('@')) {
            return false;
        }
        return isValidLocalPart(value.substring(0, at)) && isValidDomain(value.substring(at + 1));
    }

    private static boolean isValidLocalPart(String local) {
        if (local.isEmpty() || local.length() > 64 || local.startsWith(".") || local.endsWith(".") || local.contains("..")) {
            return false;
        }
        for (int i = 0; i < local.length(); i++) {
            char c = local.charAt(i);
            boolean alphanumeric = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (!alphanumeric && LOCAL_SPECIALS.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidDomain(String domain) {
        if (domain.isEmpty() || domain.length() > 253) {
            return false;
        }
        String[] labels = domain.split("\\.", -1);
        if (labels.length < 2) {
            return false;
        }
        for (String label : labels) {
            if (!LABEL.matcher(label).matches()) {
                return false;
            }
        }
        return TLD.matcher(labels[labels.length - 1]).matches();
    }
}
