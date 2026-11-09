package app.todo.common;

import java.util.List;

/** 404 {@code NOT_FOUND}: missing, deleted, malformed or another user's id all look the same (REQ-ISO-001 AC-2). */
public final class NotFoundException extends ApiException {

    public NotFoundException() {
        super(404, ProblemCode.NOT_FOUND, ProblemCode.NOT_FOUND.detail(), List.of());
    }
}
