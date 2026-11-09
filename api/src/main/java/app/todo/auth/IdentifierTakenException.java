package app.todo.auth;

import app.todo.common.ApiException;
import app.todo.common.FieldError;
import app.todo.common.ProblemCode;
import java.util.List;

/** 409 {@code IDENTIFIER_TAKEN} with one field error on {@code identifier} (REQ-AUTH-001 AC-2, AC-6). */
final class IdentifierTakenException extends ApiException {

    IdentifierTakenException() {
        super(409, ProblemCode.IDENTIFIER_TAKEN, ProblemCode.IDENTIFIER_TAKEN.detail(), List.of(new FieldError(
                "identifier", FieldError.Code.TAKEN,
                "This email address or username is already taken. Choose a different one, or log in.")));
    }
}
