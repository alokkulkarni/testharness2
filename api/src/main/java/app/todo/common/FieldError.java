package app.todo.common;

/** One entry of {@code Problem.errors} (contract {@code FieldError}); {@code message} is shown to the user as sent. */
public record FieldError(String field, Code code, String message) {

    /** The closed list of field error codes of {@code common-problem-v1.yaml}. */
    public enum Code {
        REQUIRED,
        BLANK,
        TOO_SHORT,
        TOO_LONG,
        INVALID_FORMAT,
        INVALID_TYPE,
        INVALID_DATE,
        INVALID_VALUE,
        UNKNOWN_FIELD,
        TAKEN
    }
}
