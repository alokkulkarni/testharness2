package app.todo.todo;

import app.todo.common.BodyReader;
import app.todo.common.FieldError;
import app.todo.common.ValidationException;
import app.todo.common.Whitespace;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The authoritative validation of to-do input (sdd.md section 5, ADR-0010). Lengths count Unicode code points; the
 * title is trimmed with the shared whitespace set; every error of a request is reported together.
 */
@Component
class TodoValidator {

    static final int TITLE_MAX = 200;
    static final int DESCRIPTION_MAX = 2000;

    private static final Set<String> CONTENT_MEMBERS = Set.of("title", "description", "dueDate", "priority");
    private static final Set<String> DONE_MEMBERS = Set.of("done");
    private static final Pattern DATE_SHAPE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern PAGE = Pattern.compile("0*[1-9][0-9]*");

    /** Content of a new to-do; an omitted or null priority is MEDIUM (REQ-TODO-001 AC-2). */
    TodoContent forCreate(Map<String, Object> request) {
        return content(request, false);
    }

    /** Content of an update; the priority is required (ADR-0006). */
    TodoContent forUpdate(Map<String, Object> request) {
        return content(request, true);
    }

    /** The new value of {@code done}. */
    boolean done(Map<String, Object> request) {
        BodyReader body = new BodyReader(request, DONE_MEMBERS);
        Boolean done = body.bool("done");
        if (!body.hasErrorFor("done") && done == null) {
            body.add("done", FieldError.Code.REQUIRED, "Send done as true or false.");
        }
        body.failIfInvalid();
        return done;
    }

    /** A list request from the raw query parameters (REQ-LIST-001..003). */
    TodoQuery query(String status, String q, String page) {
        List<FieldError> errors = new ArrayList<>();

        TodoQuery.Status parsedStatus = TodoQuery.Status.ALL;
        if (status != null) {
            switch (status) {
                case "all" -> parsedStatus = TodoQuery.Status.ALL;
                case "active" -> parsedStatus = TodoQuery.Status.ACTIVE;
                case "completed" -> parsedStatus = TodoQuery.Status.COMPLETED;
                default -> errors.add(new FieldError("status", FieldError.Code.INVALID_VALUE,
                        "Choose All, Active or Completed."));
            }
        }

        String search = q == null ? "" : Whitespace.trim(q).toLowerCase(Locale.ROOT);

        long parsedPage = 1;
        if (page != null) {
            if (PAGE.matcher(page).matches()) {
                String digits = page.replaceFirst("^0+", "");
                // too large for a long: beyond the last page, which the repository clamps
                parsedPage = digits.length() > 18 ? Long.MAX_VALUE : Long.parseLong(digits);
            } else {
                errors.add(new FieldError("page", FieldError.Code.INVALID_VALUE,
                        "The page must be a whole number of 1 or more."));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new TodoQuery(parsedStatus, search, parsedPage);
    }

    private TodoContent content(Map<String, Object> request, boolean priorityRequired) {
        BodyReader body = new BodyReader(request, CONTENT_MEMBERS);
        String title = body.string("title");
        String description = body.string("description");
        String dueDate = body.string("dueDate");
        String priority = body.string("priority");

        String trimmedTitle = null;
        if (!body.hasErrorFor("title")) {
            if (title == null) {
                body.add("title", FieldError.Code.REQUIRED, "Enter a title.");
            } else {
                trimmedTitle = Whitespace.trim(title);
                if (trimmedTitle.isEmpty()) {
                    body.add("title", FieldError.Code.BLANK, "Enter a title.");
                } else if (codePoints(trimmedTitle) > TITLE_MAX) {
                    body.add("title", FieldError.Code.TOO_LONG, "The title must be 200 characters or fewer.");
                }
            }
        }

        String storedDescription = null;
        if (!body.hasErrorFor("description") && description != null) {
            // measured as sent; a blank description means no description, anything else is stored unchanged
            if (codePoints(description) > DESCRIPTION_MAX) {
                body.add("description", FieldError.Code.TOO_LONG, "The description must be 2000 characters or fewer.");
            } else if (!Whitespace.isBlank(description)) {
                storedDescription = description;
            }
        }

        String storedDate = null;
        if (!body.hasErrorFor("dueDate")) {
            if (dueDate == null || dueDate.isEmpty()) {
                body.add("dueDate", FieldError.Code.REQUIRED, "Enter a due date.");
            } else if (isRealDate(dueDate)) {
                storedDate = dueDate;
            } else {
                body.add("dueDate", FieldError.Code.INVALID_DATE, "Enter a real date, for example 2026-11-06.");
            }
        }

        Priority storedPriority = Priority.MEDIUM;
        if (!body.hasErrorFor("priority")) {
            if (priority == null) {
                if (priorityRequired) {
                    body.add("priority", FieldError.Code.REQUIRED, "Choose a priority: Low, Medium or High.");
                }
            } else {
                try {
                    storedPriority = Priority.valueOf(priority);
                } catch (IllegalArgumentException e) {
                    body.add("priority", FieldError.Code.INVALID_VALUE, "Choose a priority: Low, Medium or High.");
                }
            }
        }

        body.failIfInvalid();
        return new TodoContent(trimmedTitle, storedDescription, storedDate, storedPriority);
    }

    /** A real calendar date written as {@code YYYY-MM-DD}, year 0001 to 9999 (REQ-TODO-002 AC-6). */
    private static boolean isRealDate(String value) {
        if (!DATE_SHAPE.matcher(value).matches()) {
            return false;
        }
        try {
            return LocalDate.parse(value, DATE).getYear() >= 1;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }
}
