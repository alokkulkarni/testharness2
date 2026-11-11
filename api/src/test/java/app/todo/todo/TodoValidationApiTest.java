package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * REQ-TODO-002 To-do field rules and validation, REQ-TODO-004 AC-2. Every rule is exercised on create and on
 * update (the server is the authority, NFR-004 AC-2). The validators' own classes are not named by the spec, so the
 * boundaries are pinned through the API.
 */
class TodoValidationApiTest extends IntegrationTestBase {

    private static final String GRIN = "😀"; // one code point, two UTF-16 units

    /** Sends the body as a create and as an update of an existing to-do; returns both replies. */
    private List<Reply> onCreateAndUpdate(ApiClient me, String existingId, String body) {
        return List.of(me.postInvalid(TODOS, body), me.putInvalid(TODOS + "/" + existingId, body));
    }

    private void assertRefused(List<Reply> replies, String field) {
        for (Reply r : replies) {
            r.assertProblem(400, "VALIDATION_FAILED");
            assertThat(r.errorFields()).contains(field);
        }
    }

    private void assertAccepted(ApiClient me, String existingId, String body) {
        assertThat(me.post(TODOS, body).status()).as("create: " + body).isEqualTo(201);
        assertThat(me.put(TODOS + "/" + existingId, body).status()).as("update: " + body).isEqualTo(200);
    }

    // ---- title (AC-1, AC-2, AC-3) ----

    static Stream<Arguments> blankTitles() {
        return Stream.of(
                Arguments.of("empty", ""),
                Arguments.of("spaces", "   "),
                Arguments.of("tab and newline", "\t\n"),
                Arguments.of("U+00A0", " "),
                Arguments.of("U+2007 U+202F", "  "),
                Arguments.of("U+3000", "　"),
                Arguments.of("U+0085", "\u0085"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("blankTitles")
    @DisplayName("REQ-TODO-002 AC-2 an empty or whitespace-only title is refused on create and update, naming the title field")
    void REQ_TODO_002_AC2_blank_title_is_refused(String label, String title) {
        var me = signedIn();
        String id = createTodoId(me, "original");
        long before = db().count("SELECT count(*) FROM todos");

        List<Reply> replies = onCreateAndUpdate(me, id, todoBody(title, "2026-12-01", "LOW", null));

        assertRefused(replies, "title");
        assertThat(db().count("SELECT count(*) FROM todos")).as("nothing saved").isEqualTo(before);
        assertThat(me.get(TODOS + "/" + id).<String>json("$.title")).isEqualTo("original");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {" ", "  ", "　"})
    @DisplayName("REQ-TODO-002 AC-2 a title of only Unicode spaces is refused as BLANK")
    void REQ_TODO_002_AC2_unicode_space_title_is_blank(String title) {
        Reply r = signedIn().postInvalid(TODOS, todoBody(title, "2026-12-01", "LOW", null));

        assertThat(r.errorCodeFor("title")).isEqualTo("BLANK");
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-3 a title absent from the request is refused naming the title field")
    void REQ_TODO_002_AC3_absent_title_is_refused() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertRefused(onCreateAndUpdate(me, id, todoBody(null, "2026-12-01", "LOW", null)), "title");
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-3 a title of exactly 200 characters is accepted; 201 is refused")
    void REQ_TODO_002_AC3_title_boundary_is_200() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertAccepted(me, id, todoBody("t".repeat(200), "2026-12-01", "LOW", null));
        List<Reply> refused = onCreateAndUpdate(me, id, todoBody("t".repeat(201), "2026-12-01", "LOW", null));

        assertRefused(refused, "title");
        for (Reply r : refused) {
            assertThat(r.errorCodeFor("title")).isEqualTo("TOO_LONG");
        }
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-3 the 200 limit applies after trimming: 200 characters padded with spaces and U+00A0 is accepted")
    void REQ_TODO_002_AC3_limit_applies_to_the_trimmed_title() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertAccepted(me, id, todoBody("   " + "t".repeat(200) + "　 ", "2026-12-01", "LOW", null));
        assertRefused(onCreateAndUpdate(me, id, todoBody("  " + "t".repeat(201) + "  ", "2026-12-01", "LOW", null)), "title");
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-3 title length counts code points: 200 emoji accepted, 201 refused")
    void REQ_TODO_002_AC3_title_length_counts_code_points() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertAccepted(me, id, todoBody(GRIN.repeat(200), "2026-12-01", "LOW", null));
        assertRefused(onCreateAndUpdate(me, id, todoBody(GRIN.repeat(201), "2026-12-01", "LOW", null)), "title");
    }

    // ---- description (AC-4) ----

    @Test
    @DisplayName("REQ-TODO-002 AC-4 a description of exactly 2000 characters, or none, is accepted; 2001 is refused")
    void REQ_TODO_002_AC4_description_boundary_is_2000() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertAccepted(me, id, todoBody("T", "2026-12-01", "LOW", "d".repeat(2000)));
        assertAccepted(me, id, todoBody("T", "2026-12-01", "LOW", null));
        List<Reply> refused = onCreateAndUpdate(me, id, todoBody("T", "2026-12-01", "LOW", "d".repeat(2001)));

        assertRefused(refused, "description");
        for (Reply r : refused) {
            assertThat(r.errorCodeFor("description")).isEqualTo("TOO_LONG");
        }
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-4 the description limit is measured as sent: 1999 characters and two spaces is 2001")
    void REQ_TODO_002_AC4_description_is_measured_as_sent_not_trimmed() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertRefused(onCreateAndUpdate(me, id, todoBody("T", "2026-12-01", "LOW", "d".repeat(1999) + "  ")), "description");
        assertAccepted(me, id, todoBody("T", "2026-12-01", "LOW", "d".repeat(1998) + "  "));
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-4 description length counts code points: 2000 emoji accepted, 2001 refused")
    void REQ_TODO_002_AC4_description_length_counts_code_points() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertAccepted(me, id, todoBody("T", "2026-12-01", "LOW", GRIN.repeat(2000)));
        assertRefused(onCreateAndUpdate(me, id, todoBody("T", "2026-12-01", "LOW", GRIN.repeat(2001))), "description");
    }

    // ---- due date (AC-5, AC-6, AC-7, AC-9) ----

    @Test
    @DisplayName("REQ-TODO-002 AC-5 a to-do saved without a due date, or with a null due date, is refused")
    void REQ_TODO_002_AC5_due_date_is_required() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertRefused(onCreateAndUpdate(me, id, todoBody("T", null, "LOW", null)), "dueDate");
        assertRefused(onCreateAndUpdate(me, id, todoBody("T", Json.NULL, "LOW", null)), "dueDate");
        assertRefused(onCreateAndUpdate(me, id, todoBody("T", "", "LOW", null)), "dueDate");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        "2026-02-30", "2023-02-29", "2026-13-01", "2026-00-10", "2026-04-31", "2026-01-00", "tomorrow", "not a date",
        "2026-1-5", "26-01-05", "2026/01/05", "2026-12-01T00:00:00Z", "2026-12-01 ", "0000-01-01", "10000-01-01",
        "20261201"
    })
    @DisplayName("REQ-TODO-002 AC-6 a due date that is not a valid calendar date is refused on create and update")
    void REQ_TODO_002_AC6_invalid_calendar_date_is_refused(String date) {
        var me = signedIn();
        String id = createTodoId(me, "original");

        List<Reply> replies = onCreateAndUpdate(me, id, todoBody("T", date, "LOW", null));

        assertRefused(replies, "dueDate");
        assertThat(me.get(TODOS + "/" + id).<String>json("$.dueDate")).isEqualTo("2026-12-01");
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-6 an impossible calendar date has the field error code INVALID_DATE")
    void REQ_TODO_002_AC6_impossible_date_is_invalid_date() {
        assertThat(signedIn().postInvalid(TODOS, todoBody("T", "2026-02-30", "LOW", null)).errorCodeFor("dueDate"))
                .isEqualTo("INVALID_DATE");
        assertThat(signedIn().postInvalid(TODOS, todoBody("T", "free text", "LOW", null)).errorCodeFor("dueDate"))
                .isEqualTo("INVALID_DATE");
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-6 leap days, year 0001 and year 9999 are valid dates")
    void REQ_TODO_002_AC6_edge_valid_dates_are_accepted() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        for (String date : List.of("2024-02-29", "0001-01-01", "9999-12-31", "2000-02-29")) {
            assertAccepted(me, id, todoBody("T", date, "LOW", null));
        }
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-9 a due date in the past is accepted")
    void REQ_TODO_002_AC9_past_due_date_is_accepted() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertAccepted(me, id, todoBody("T", "1999-01-01", "LOW", null));
    }

    // ---- priority (AC-8) ----

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"URGENT", "low", "Medium", "high", "", " ", "NONE", "LOW "})
    @DisplayName("REQ-TODO-002 AC-8 a priority other than LOW, MEDIUM or HIGH (case-sensitive) is refused through the API")
    void REQ_TODO_002_AC8_unknown_priority_is_refused(String priority) {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertRefused(onCreateAndUpdate(me, id, todoBody("T", "2026-12-01", priority, null)), "priority");
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOW", "MEDIUM", "HIGH"})
    @DisplayName("REQ-TODO-002 AC-8 LOW, MEDIUM and HIGH are accepted")
    void REQ_TODO_002_AC8_the_three_priorities_are_accepted(String priority) {
        var me = signedIn();
        String id = createTodoId(me, "original");

        assertAccepted(me, id, todoBody("T", "2026-12-01", priority, null));
    }

    @Test
    @DisplayName("REQ-TODO-004 AC-1 priority is required on update: absent or null is refused")
    void REQ_TODO_004_AC1_priority_is_required_on_update() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        Reply absent = me.putInvalid(TODOS + "/" + id, todoBody("T", "2026-12-01", null, null));
        Reply nul = me.putInvalid(TODOS + "/" + id, todoBody("T", "2026-12-01", Json.NULL, null));

        assertRefused(List.of(absent, nul), "priority");
    }

    // ---- AC-10: every refusal names its field, all errors reported together ----

    @Test
    @DisplayName("REQ-TODO-002 AC-10 several invalid fields are reported together, each naming its field")
    void REQ_TODO_002_AC10_all_field_errors_are_reported_together() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        List<Reply> replies = onCreateAndUpdate(me, id, todoBody("", "2026-02-30", "URGENT", "d".repeat(2001)));

        for (Reply r : replies) {
            r.assertProblem(400, "VALIDATION_FAILED");
            assertThat(r.errorFields()).contains("title", "dueDate", "priority", "description");
            assertThat(r.<List<String>>json("$.errors[*].message")).allSatisfy(m -> assertThat(m).isNotBlank());
        }
    }

    // ---- bodies: unknown properties, wrong types, malformed JSON ----

    @Test
    @DisplayName("NFR-004 AC-2 an unknown property is refused as UNKNOWN_FIELD naming it (the owner can never come from the request)")
    void NFR_004_AC2_unknown_property_is_refused() {
        var me = signedIn();
        String id = createTodoId(me, "original");
        long before = db().count("SELECT count(*) FROM todos");
        String body = Json.object("title", "T", "dueDate", "2026-12-01", "owner", "someone-else", "userId", 1);

        for (Reply r : List.of(me.postInvalid(TODOS, body), me.putInvalid(TODOS + "/" + id, body))) {
            r.assertProblem(400, "VALIDATION_FAILED");
            assertThat(r.errorFields()).contains("owner", "userId");
            assertThat(r.errorCodeFor("owner")).isEqualTo("UNKNOWN_FIELD");
        }
        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(before);
    }

    @Test
    @DisplayName("NFR-004 AC-2 a JSON value of the wrong type is refused as INVALID_TYPE naming the field")
    void NFR_004_AC2_wrong_type_is_refused() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        for (String body : List.of(
                Json.object("title", 5, "dueDate", "2026-12-01", "priority", "LOW"),
                Json.object("title", "T", "dueDate", 20261201, "priority", "LOW"),
                Json.object("title", "T", "dueDate", "2026-12-01", "priority", 3),
                Json.object("title", "T", "dueDate", "2026-12-01", "priority", "LOW", "description", 7))) {
            for (Reply r : onCreateAndUpdate(me, id, body)) {
                r.assertProblem(400, "VALIDATION_FAILED");
                assertThat(r.<List<String>>json("$.errors[*].code")).contains("INVALID_TYPE");
            }
        }
    }

    @Test
    @DisplayName("NFR-004 AC-2 a done update with a non-boolean or missing done is refused")
    void NFR_004_AC2_done_must_be_a_boolean() {
        var me = signedIn();
        String id = createTodoId(me, "original");

        Reply wrongType = me.putInvalid(TODOS + "/" + id + "/done", Json.object("done", "yes"));
        Reply missing = me.putInvalid(TODOS + "/" + id + "/done", "{}");
        Reply unknown = me.putInvalid(TODOS + "/" + id + "/done", Json.object("done", true, "title", "x"));

        for (Reply r : List.of(wrongType, missing, unknown)) {
            r.assertProblem(400, "VALIDATION_FAILED");
        }
        assertThat(wrongType.errorFields()).contains("done");
        assertThat(missing.errorFields()).contains("done");
        assertThat(unknown.errorCodeFor("title")).isEqualTo("UNKNOWN_FIELD");
        assertThat(me.get(TODOS + "/" + id).<Boolean>json("$.done")).isFalse();
    }
}
