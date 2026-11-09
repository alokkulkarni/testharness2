package app.todo.auth;

import app.todo.common.ApiException;
import app.todo.common.ProblemCode;
import java.util.List;

/** 401 {@code INVALID_CREDENTIALS}: the same body for a wrong password and an unknown identifier (REQ-AUTH-002 AC-2, AC-3). */
final class InvalidCredentialsException extends ApiException {

    InvalidCredentialsException() {
        super(401, ProblemCode.INVALID_CREDENTIALS, ProblemCode.INVALID_CREDENTIALS.detail(), List.of());
    }
}
