package app.todo.common;

import java.util.Locale;

/**
 * The closed list of Problem codes: exactly the {@code Problem.code} enum of {@code common-problem-v1.yaml}
 * (ADR-0011). Each code has a fixed title and a fixed, safe default detail sentence.
 */
public enum ProblemCode {
    VALIDATION_FAILED("Validation failed", "One or more fields are invalid. Check them and try again."),
    MALFORMED_REQUEST("Malformed request", "The request body is missing or is not valid JSON."),
    IDENTIFIER_TAKEN("Identifier taken", "That email address or username is already in use."),
    INVALID_CREDENTIALS("Invalid credentials", "Invalid identifier or password."),
    UNAUTHENTICATED("Not signed in", "Please log in."),
    CLIENT_HEADER_MISSING("Client header missing", "The X-Todo-Client header is missing or wrong."),
    NOT_FOUND("Not found", "That item does not exist."),
    METHOD_NOT_ALLOWED("Method not allowed", "That method is not allowed for this path."),
    UNSUPPORTED_MEDIA_TYPE("Unsupported media type", "Send the request body as application/json."),
    REQUEST_REJECTED("Request rejected", "The request could not be accepted."),
    INTERNAL_ERROR("Internal error", "Something went wrong on the server. Please try again.");

    private final String title;
    private final String detail;

    ProblemCode(String title, String detail) {
        this.title = title;
        this.detail = detail;
    }

    public String title() {
        return title;
    }

    /** The fixed detail sentence for this code. */
    public String detail() {
        return detail;
    }

    /** {@code urn:todo:problem:} followed by the code in lower kebab case. */
    public String type() {
        return "urn:todo:problem:" + name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
