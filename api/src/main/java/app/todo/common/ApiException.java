package app.todo.common;

import java.util.List;

/**
 * Base of every application exception that maps to a Problem response (ADR-0011). It carries what the body needs, so
 * {@code ProblemDetailsAdvice} maps any subtype without naming it and {@code common} depends on no module.
 */
public abstract class ApiException extends RuntimeException {

    private final int status;
    private final ProblemCode code;
    private final String detail;
    private final List<FieldError> errors;

    protected ApiException(int status, ProblemCode code, String detail, List<FieldError> errors) {
        // expected, client-caused failures: no stack trace is captured
        super(code.name(), null, false, false);
        this.status = status;
        this.code = code;
        this.detail = detail;
        this.errors = List.copyOf(errors);
    }

    public int status() {
        return status;
    }

    public ProblemCode code() {
        return code;
    }

    public String detail() {
        return detail;
    }

    public List<FieldError> errors() {
        return errors;
    }
}
