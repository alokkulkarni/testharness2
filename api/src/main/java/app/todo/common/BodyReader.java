package app.todo.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the members of a parsed JSON object body for a validator: refuses unknown members, refuses members of the
 * wrong JSON type, and collects every field error so they are reported together (sdd.md section 5 "Bodies").
 */
public final class BodyReader {

    private final Map<String, Object> body;
    private final List<FieldError> errors = new ArrayList<>();

    public BodyReader(Map<String, Object> body, Set<String> allowed) {
        this.body = body;
        for (String name : body.keySet()) {
            if (!allowed.contains(name)) {
                errors.add(new FieldError(name, FieldError.Code.UNKNOWN_FIELD, "This field is not accepted. Remove it and try again."));
            }
        }
    }

    /** True when the member is present and not JSON null. */
    public boolean has(String name) {
        return body.get(name) != null;
    }

    /** The member as text; null when absent or JSON null; INVALID_TYPE (and null) for any other JSON type. */
    public String string(String name) {
        Object value = body.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        errors.add(new FieldError(name, FieldError.Code.INVALID_TYPE, "Send this value as text."));
        return null;
    }

    /** The member as a boolean; null when absent or JSON null; INVALID_TYPE (and null) for any other JSON type. */
    public Boolean bool(String name) {
        Object value = body.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        errors.add(new FieldError(name, FieldError.Code.INVALID_TYPE, "Send true or false."));
        return null;
    }

    /** True when an error has already been recorded for the field (for example a wrong type). */
    public boolean hasErrorFor(String name) {
        return errors.stream().anyMatch(e -> e.field().equals(name));
    }

    public void add(String field, FieldError.Code code, String message) {
        errors.add(new FieldError(field, code, message));
    }

    /** Throws {@link ValidationException} when any error was recorded. */
    public void failIfInvalid() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
