package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** REQ-TODO-001 Create a to-do, REQ-TODO-003 View and order the list (todo-api-v1.yaml createTodo, getTodo, listTodos). */
class TodoCreateApiTest extends IntegrationTestBase {

    private static final String UUID_V4 = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    private static final String TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";

    @Test
    @DisplayName("REQ-TODO-001 AC-1 a valid title, due date and priority create a not-done to-do with equal created and updated timestamps")
    void REQ_TODO_001_AC1_creates_a_todo_for_me() {
        var me = signedIn();

        Reply r = me.post(TODOS, todoBody("Buy milk", "2026-12-01", "HIGH", null));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.<String>json("$.id")).matches(UUID_V4);
        assertThat(r.header("location")).endsWith("/api/v1/todos/" + r.<String>json("$.id"));
        assertThat(r.<String>json("$.title")).isEqualTo("Buy milk");
        assertThat(r.<String>json("$.dueDate")).isEqualTo("2026-12-01");
        assertThat(r.<String>json("$.priority")).isEqualTo("HIGH");
        assertThat(r.<Boolean>json("$.done")).isFalse();
        assertThat(r.<String>json("$.createdAt")).matches(TIMESTAMP).isEqualTo("2026-11-08T10:00:00.000Z");
        assertThat(r.<String>json("$.updatedAt")).isEqualTo(r.<String>json("$.createdAt"));
    }

    @Test
    @DisplayName("REQ-TODO-001 AC-1 the new to-do appears at the top of my list, newest first")
    void REQ_TODO_001_AC1_new_todo_appears_first_in_the_list() {
        var me = signedIn();
        createTodoId(me, "older");
        clock.advance(Duration.ofSeconds(1));
        String newest = createTodoId(me, "newest");

        Reply list = me.get(TODOS);

        assertThat(list.<String>json("$.items[0].id")).isEqualTo(newest);
        assertThat(list.<String>json("$.items[0].title")).isEqualTo("newest");
    }

    @Test
    @DisplayName("REQ-TODO-001 AC-2 a create request that omits the priority stores Medium")
    void REQ_TODO_001_AC2_omitted_priority_is_medium() {
        var me = signedIn();

        Reply r = me.post(TODOS, todoBody("No priority", "2026-12-01", null, null));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.<String>json("$.priority")).isEqualTo("MEDIUM");
        assertThat(me.get(TODOS + "/" + r.<String>json("$.id")).<String>json("$.priority")).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("REQ-TODO-001 AC-2 a create request with a null priority stores Medium")
    void REQ_TODO_001_AC2_null_priority_is_medium() {
        Reply r = signedIn().post(TODOS, todoBody("Null priority", "2026-12-01", Json.NULL, null));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.<String>json("$.priority")).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("REQ-TODO-001 AC-3 a description is stored exactly as sent and shown")
    void REQ_TODO_001_AC3_description_is_stored_as_sent() {
        var me = signedIn();

        Reply r = me.post(TODOS, todoBody("With description", "2026-12-01", "LOW", "  two words\nsecond line  "));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.<String>json("$.description")).isEqualTo("  two words\nsecond line  ");
        assertThat(me.get(TODOS + "/" + r.<String>json("$.id")).<String>json("$.description"))
                .isEqualTo("  two words\nsecond line  ");
    }

    @Test
    @DisplayName("REQ-TODO-001 AC-3 an omitted, null, empty or whitespace-only description means no description")
    void REQ_TODO_001_AC3_blank_description_means_no_description() {
        var me = signedIn();
        Object[] blanks = {null, Json.NULL, "", "   ", " 　"};

        for (Object blank : blanks) {
            Reply r = me.post(TODOS, todoBody("No description", "2026-12-01", "LOW", blank));
            assertThat(r.status()).as(String.valueOf(blank)).isEqualTo(201);
            assertThat(r.<Object>json("$.description")).as(String.valueOf(blank)).isNull();
        }
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-1 the title is stored with leading and trailing whitespace removed")
    void REQ_TODO_002_AC1_title_is_stored_trimmed() {
        Reply r = signedIn().post(TODOS, todoBody("   Buy milk　 ", "2026-12-01", "LOW", null));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.<String>json("$.title")).isEqualTo("Buy milk");
    }

    @Test
    @DisplayName("REQ-TODO-001 AC-4 the new to-do is still there on the next request (persisted)")
    void REQ_TODO_001_AC4_new_todo_is_persisted() {
        var me = signedIn();
        String id = createTodoId(me, "persisted");

        Reply got = me.get(TODOS + "/" + id);

        assertThat(got.status()).isEqualTo(200);
        assertThat(got.<String>json("$.title")).isEqualTo("persisted");
        assertThat(db().count("SELECT count(*) FROM todos WHERE public_id = ?", id)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-7 the due date is a date only and comes back as the same calendar date")
    void REQ_TODO_002_AC7_due_date_round_trips_as_a_plain_date() {
        var me = signedIn();

        for (String date : new String[] {"2026-12-01", "2026-01-31", "2024-02-29", "2026-12-31"}) {
            Reply r = me.post(TODOS, todoBody("date " + date, date, "LOW", null));
            assertThat(r.<String>json("$.dueDate")).isEqualTo(date);
            assertThat(me.get(TODOS + "/" + r.<String>json("$.id")).<String>json("$.dueDate")).isEqualTo(date);
        }
    }

    // ---- REQ-TODO-003 ----

    @Test
    @DisplayName("REQ-TODO-003 AC-1 the list is ordered newest first by creation time and each item shows title, done, due date and priority")
    void REQ_TODO_003_AC1_list_is_newest_first_with_the_listed_fields() {
        var me = signedIn();
        String a = createTodoId(me, "first");
        clock.advance(Duration.ofSeconds(1));
        String b = createTodoId(me, "second");
        clock.advance(Duration.ofSeconds(1));
        String c = createTodoId(me, "third");

        Reply list = me.get(TODOS);

        assertThat(list.<java.util.List<String>>json("$.items[*].id")).containsExactly(c, b, a);
        assertThat(list.<String>json("$.items[0].title")).isEqualTo("third");
        assertThat(list.<Boolean>json("$.items[0].done")).isFalse();
        assertThat(list.<String>json("$.items[0].dueDate")).isEqualTo("2026-12-01");
        assertThat(list.<String>json("$.items[0].priority")).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("REQ-TODO-003 AC-1 to-dos created in the same millisecond are ordered newest first by insertion (no tie)")
    void REQ_TODO_003_AC1_same_timestamp_ties_break_by_insertion_order_newest_first() {
        var me = signedIn();
        String a = createTodoId(me, "tie a");
        String b = createTodoId(me, "tie b");
        String c = createTodoId(me, "tie c");

        Reply list = me.get(TODOS);

        assertThat(list.<java.util.List<String>>json("$.items[*].id")).containsExactly(c, b, a);
    }

    @Test
    @DisplayName("REQ-TODO-003 AC-2 a user with no to-dos gets an empty first page and totals of zero (first-use state)")
    void REQ_TODO_003_AC2_no_todos_gives_empty_list_and_zero_totals() {
        Reply list = signedIn().get(TODOS);

        assertThat(list.status()).isEqualTo(200);
        assertThat(list.<java.util.List<Object>>json("$.items")).isEmpty();
        assertThat(list.<Integer>json("$.page")).isEqualTo(1);
        assertThat(list.<Integer>json("$.pageSize")).isEqualTo(20);
        assertThat(list.<Integer>json("$.totalItems")).isZero();
        assertThat(list.<Integer>json("$.totalPages")).isZero();
        assertThat(list.<Integer>json("$.totals.all")).isZero();
        assertThat(list.<Integer>json("$.totals.active")).isZero();
        assertThat(list.<Integer>json("$.totals.completed")).isZero();
    }

    @Test
    @DisplayName("REQ-TODO-003 AC-3 a to-do shows its description, created timestamp and updated timestamp")
    void REQ_TODO_003_AC3_detail_has_description_and_timestamps() {
        var me = signedIn();
        String id = me.post(TODOS, todoBody("Detail", "2026-12-01", "LOW", "the description")).json("$.id");

        Reply got = me.get(TODOS + "/" + id);

        assertThat(got.status()).isEqualTo(200);
        assertThat(got.<String>json("$.description")).isEqualTo("the description");
        assertThat(got.<String>json("$.createdAt")).matches(TIMESTAMP);
        assertThat(got.<String>json("$.updatedAt")).matches(TIMESTAMP);
    }

    @Test
    @DisplayName("REQ-TODO-001 AC-1 the totals count the caller's active and completed to-dos")
    void REQ_TODO_001_AC1_totals_reflect_created_todos() {
        var me = signedIn();
        String done = createTodoId(me, "will be done");
        createTodoId(me, "stays active");
        setDone(me, done, true);

        Reply list = me.get(TODOS);

        assertThat(list.<Integer>json("$.totals.all")).isEqualTo(2);
        assertThat(list.<Integer>json("$.totals.active")).isEqualTo(1);
        assertThat(list.<Integer>json("$.totals.completed")).isEqualTo(1);
    }
}
