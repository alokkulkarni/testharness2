package app.todo.common;

import java.util.List;

/** 400 {@code VALIDATION_FAILED} with every field error of the request (REQ-TODO-002 AC-10). */
public final class ValidationException extends ApiException {

    public ValidationException(List<FieldError> errors) {
        super(400, ProblemCode.VALIDATION_FAILED, ProblemCode.VALIDATION_FAILED.detail(), errors);
    }
}
