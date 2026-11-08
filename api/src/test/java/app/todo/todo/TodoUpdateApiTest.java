package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** REQ-TODO-004 Edit a to-do, REQ-TODO-005 Mark done and not done (todo-api-v1.yaml updateTodo, setTodoDone). */
class TodoUpdateApiTest extends IntegrationTestBase {

    @Test
    @DisplayName("REQ-TODO-004 AC-1 changing title, description, due date and priority persists the new values, advances updatedAt, keeps createdAt")
    void REQ_TODO_004_AC1_edit_replaces_content_and_advances_updated_at() {
        var me = signedIn();
        Reply created = me.post(TODOS, todoBody("Old title", "2026-12-01", "LOW", "old description"));
        String id = created.json("$.id");
        clock.advance(Duration.ofMinutes(5));

        Reply updated = me.put(TODOS + "/" + id, todoBody("  New title ", "2027-01-15", "HIGH", "new description"));

        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.<String>json("$.title")).isEqualTo("New title");
        assertThat(updated.<String>json("$.description")).isEqualTo("new description");
        assertThat(updated.<String>json("$.dueDate")).isEqualTo("2027-01-15");
        assertThat(updated.<String>json("$.priority")).isEqualTo("HIGH");
        assertThat(updated.<String>json("$.createdAt")).isEqualTo(created.<String>json("$.createdAt"));
        assertThat(updated.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:05:00.000Z");
        Reply reread = me.get(TODOS + "/" + id);
        assertThat(reread.body()).isEqualTo(updated.body());
    }

    @Test
    @DisplayName("REQ-TODO-004 AC-1 updatedAt strictly advances even within the same millisecond (previous + 1 ms)")
    void REQ_TODO_004_AC1_updated_at_strictly_advances_within_the_same_millisecond() {
        var me = signedIn();
        String id = createTodoId(me, "tick");

        Reply first = me.put(TODOS + "/" + id, todoBody("tick 1", "2026-12-01", "LOW", null));
        Reply second = me.put(TODOS + "/" + id, todoBody("tick 2", "2026-12-01", "LOW", null));

        assertThat(first.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:00:00.001Z");
        assertThat(second.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:00:00.002Z");
        assertThat(second.<String>json("$.createdAt")).isEqualTo("2026-11-08T10:00:00.000Z");
    }

    @Test
    @DisplayName("REQ-TODO-004 AC-1 an edit does not change done")
    void REQ_TODO_004_AC1_edit_leaves_done_unchanged() {
        var me = signedIn();
        String id = createTodoId(me, "finished");
        setDone(me, id, true);

        Reply updated = me.put(TODOS + "/" + id, todoBody("finished, renamed", "2026-12-01", "LOW", null));

        assertThat(updated.<Boolean>json("$.done")).isTrue();
    }

    @Test
    @DisplayName("REQ-TODO-004 AC-2 an edit with invalid values is refused and the stored to-do is unchanged")
    void REQ_TODO_004_AC2_invalid_edit_leaves_the_stored_todo_unchanged() {
        var me = signedIn();
        String id = me.post(TODOS, todoBody("Keep", "2026-12-01", "LOW", "keep description")).json("$.id");
        String before = me.get(TODOS + "/" + id).body();
        clock.advance(Duration.ofMinutes(1));

        Reply r = me.putInvalid(TODOS + "/" + id, todoBody("", "2026-02-30", "HIGH", "changed"));

        r.assertProblem(400, "VALIDATION_FAILED");
        assertThat(me.get(TODOS + "/" + id).body()).isEqualTo(before);
    }

    @Test
    @DisplayName("REQ-TODO-004 AC-3 editing a to-do that does not exist gives not found and creates nothing")
    void REQ_TODO_004_AC3_editing_a_missing_todo_is_not_found_and_creates_nothing() {
        var me = signedIn();
        long before = db().count("SELECT count(*) FROM todos");

        Reply r = me.put(TODOS + "/" + UUID.randomUUID(), todoBody("Ghost", "2026-12-01", "LOW", null));

        r.assertProblem(404, "NOT_FOUND");
        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(before);
        assertThat(me.get(TODOS).<Integer>json("$.totalItems")).isZero();
    }

    @Test
    @DisplayName("REQ-TODO-004 AC-4 clearing the description saves the to-do with no description (null, empty, whitespace or absent)")
    void REQ_TODO_004_AC4_clearing_the_description_stores_none() {
        var me = signedIn();
        Object[] clears = {Json.NULL, "", "   ", " ", null};

        for (Object clear : clears) {
            String id = me.post(TODOS, todoBody("Has description", "2026-12-01", "LOW", "something")).json("$.id");

            Reply r = me.put(TODOS + "/" + id, todoBody("Has description", "2026-12-01", "LOW", clear));

            assertThat(r.status()).as(String.valueOf(clear)).isEqualTo(200);
            assertThat(r.<Object>json("$.description")).as(String.valueOf(clear)).isNull();
            assertThat(me.get(TODOS + "/" + id).<Object>json("$.description")).isNull();
        }
    }

    @Test
    @DisplayName("REQ-TODO-004 AC-4 the due date cannot be cleared: it is required on update")
    void REQ_TODO_004_AC4_due_date_cannot_be_cleared() {
        var me = signedIn();
        String id = createTodoId(me, "needs a date");

        Reply r = me.putInvalid(TODOS + "/" + id, todoBody("needs a date", Json.NULL, "LOW", null));

        r.assertProblem(400, "VALIDATION_FAILED");
        assertThat(r.errorFields()).contains("dueDate");
        assertThat(me.get(TODOS + "/" + id).<String>json("$.dueDate")).isEqualTo("2026-12-01");
    }

    // ---- REQ-TODO-005 ----

    @Test
    @DisplayName("REQ-TODO-005 AC-1 marking an active to-do done persists Completed and advances updatedAt")
    void REQ_TODO_005_AC1_mark_done_persists_and_advances_updated_at() {
        var me = signedIn();
        String id = createTodoId(me, "to finish");
        clock.advance(Duration.ofMinutes(2));

        Reply r = me.put(TODOS + "/" + id + "/done", Json.object("done", true));

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.<Boolean>json("$.done")).isTrue();
        assertThat(r.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:02:00.000Z");
        assertThat(r.<String>json("$.createdAt")).isEqualTo("2026-11-08T10:00:00.000Z");
        assertThat(r.<String>json("$.title")).isEqualTo("to finish");
        assertThat(me.get(TODOS + "/" + id).<Boolean>json("$.done")).isTrue();
    }

    @Test
    @DisplayName("REQ-TODO-005 AC-2 marking a completed to-do not done makes it Active again and advances updatedAt")
    void REQ_TODO_005_AC2_mark_not_done_persists_and_advances_updated_at() {
        var me = signedIn();
        String id = createTodoId(me, "reopen");
        setDone(me, id, true);
        clock.advance(Duration.ofMinutes(3));

        Reply r = me.put(TODOS + "/" + id + "/done", Json.object("done", false));

        assertThat(r.<Boolean>json("$.done")).isFalse();
        assertThat(r.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:03:00.000Z");
        assertThat(me.get(TODOS + "/" + id).<Boolean>json("$.done")).isFalse();
    }

    @Test
    @DisplayName("REQ-TODO-005 AC-1 within one millisecond updatedAt still strictly advances on every accepted done request, also when done does not change")
    void REQ_TODO_005_AC1_updated_at_strictly_advances_on_every_accepted_request() {
        var me = signedIn();
        String id = createTodoId(me, "idempotent");

        Reply one = me.put(TODOS + "/" + id + "/done", Json.object("done", true));
        Reply two = me.put(TODOS + "/" + id + "/done", Json.object("done", true));
        Reply three = me.put(TODOS + "/" + id + "/done", Json.object("done", false));

        assertThat(one.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:00:00.001Z");
        assertThat(two.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:00:00.002Z");
        assertThat(three.<String>json("$.updatedAt")).isEqualTo("2026-11-08T10:00:00.003Z");
    }

    @Test
    @DisplayName("REQ-TODO-005 AC-1 marking done changes only done and updatedAt")
    void REQ_TODO_005_AC1_done_leaves_content_fields_unchanged() {
        var me = signedIn();
        Reply created = me.post(TODOS, todoBody("Content", "2026-12-24", "HIGH", "words"));
        String id = created.json("$.id");

        Reply r = setDone(me, id, true);

        for (String field : new String[] {"id", "title", "description", "dueDate", "priority", "createdAt"}) {
            assertThat(r.<Object>json("$." + field)).as(field).isEqualTo(created.<Object>json("$." + field));
        }
    }

    @Test
    @DisplayName("REQ-TODO-005 AC-3 the Active filter no longer lists a to-do right after it is marked done")
    void REQ_TODO_005_AC3_done_todo_leaves_the_active_view_immediately() {
        var me = signedIn();
        String id = createTodoId(me, "leaving active");
        createTodoId(me, "staying active");
        assertThat(me.get(TODOS + "?status=active").<Integer>json("$.totalItems")).isEqualTo(2);

        setDone(me, id, true);

        Reply active = me.get(TODOS + "?status=active");
        assertThat(active.<Integer>json("$.totalItems")).isEqualTo(1);
        assertThat(active.<java.util.List<String>>json("$.items[*].id")).doesNotContain(id);
        assertThat(me.get(TODOS + "?status=completed").<java.util.List<String>>json("$.items[*].id")).containsExactly(id);
    }

    @Test
    @DisplayName("REQ-TODO-005 AC-1 marking a missing to-do done is not found")
    void REQ_TODO_005_AC1_done_on_a_missing_todo_is_not_found() {
        signedIn().put(TODOS + "/" + UUID.randomUUID() + "/done", Json.object("done", true)).assertProblem(404, "NOT_FOUND");
    }
}
