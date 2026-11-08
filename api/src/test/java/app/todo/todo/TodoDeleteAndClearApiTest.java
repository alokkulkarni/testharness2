package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * REQ-TODO-006 Delete a to-do (API half; the confirmation dialog is UI), REQ-TODO-007 Clear all completed
 * (API half) (todo-api-v1.yaml deleteTodo, clearCompletedTodos).
 */
class TodoDeleteAndClearApiTest extends IntegrationTestBase {

    private Reply clearCompleted(ApiClient me, String query) {
        Reply r = me.send("DELETE", TODOS + "/completed" + query, null, "application/json", ApiClient.Check.FULL);
        assertThat(r.status()).as(r.body()).isEqualTo(200);
        assertThat(r.contentType()).startsWith("application/json");
        assertThat(r.<Object>json("$.deletedCount")).isInstanceOf(Integer.class);
        return r;
    }

    @Test
    @DisplayName("REQ-TODO-006 AC-3 a deleted to-do is permanently gone: 204, then not found")
    void REQ_TODO_006_AC3_delete_removes_the_todo_permanently() {
        var me = signedIn();
        String id = createTodoId(me, "to delete");
        createTodoId(me, "bystander");

        Reply r = me.delete(TODOS + "/" + id);

        assertThat(r.status()).isEqualTo(204);
        assertThat(r.body()).isEmpty();
        me.getInvalid(TODOS + "/" + id).assertProblem(404, "NOT_FOUND");
        assertThat(me.get(TODOS).<List<String>>json("$.items[*].title")).containsExactly("bystander");
        assertThat(db().count("SELECT count(*) FROM todos WHERE public_id = ?", id)).as("no soft delete").isZero();
    }

    @Test
    @DisplayName("REQ-TODO-006 AC-4 updating or deleting a deleted to-do is not found")
    void REQ_TODO_006_AC4_update_and_delete_of_a_deleted_todo_are_not_found() {
        var me = signedIn();
        String id = createTodoId(me, "short lived");
        me.delete(TODOS + "/" + id);
        long before = db().count("SELECT count(*) FROM todos");

        me.put(TODOS + "/" + id, todoBody("revived", "2026-12-01", "LOW", null)).assertProblem(404, "NOT_FOUND");
        me.put(TODOS + "/" + id + "/done", Json.object("done", true)).assertProblem(404, "NOT_FOUND");
        me.delete(TODOS + "/" + id).assertProblem(404, "NOT_FOUND");

        assertThat(db().count("SELECT count(*) FROM todos")).as("nothing re-created").isEqualTo(before);
    }

    @Test
    @DisplayName("REQ-TODO-006 AC-5 deleting the only to-do on the last page, the next request for that page gives the last valid page, not an error")
    void REQ_TODO_006_AC5_page_past_the_last_after_delete_gives_the_last_valid_page() {
        var me = signedIn();
        String first = createTodoId(me, "first"); // oldest, so it is on page 2 of 21
        for (int i = 2; i <= 21; i++) {
            createTodoId(me, "todo " + i);
        }
        Reply page2 = me.get(TODOS + "?page=2");
        assertThat(page2.<List<String>>json("$.items[*].id")).containsExactly(first);

        me.delete(TODOS + "/" + first);
        Reply after = me.get(TODOS + "?page=2");

        assertThat(after.status()).isEqualTo(200);
        assertThat(after.<Integer>json("$.page")).isEqualTo(1);
        assertThat(after.<Integer>json("$.totalPages")).isEqualTo(1);
        assertThat(after.<List<Object>>json("$.items")).hasSize(20);
    }

    // ---- REQ-TODO-007 ----

    @Test
    @DisplayName("REQ-TODO-007 AC-2 clear completed deletes all completed to-dos across all pages and leaves active ones")
    void REQ_TODO_007_AC2_clear_completed_deletes_all_completed_across_pages() {
        var me = signedIn();
        for (int i = 0; i < 25; i++) {
            setDone(me, createTodoId(me, "done " + i), true);
        }
        String active = createTodoId(me, "still active");

        Reply r = clearCompleted(me, "");

        assertThat(r.<Integer>json("$.deletedCount")).isEqualTo(25);
        Reply list = me.get(TODOS);
        assertThat(list.<Integer>json("$.totalItems")).isEqualTo(1);
        assertThat(list.<List<String>>json("$.items[*].id")).containsExactly(active);
        assertThat(list.<Integer>json("$.totals.completed")).isZero();
    }

    @Test
    @DisplayName("REQ-TODO-007 AC-2 clear completed ignores any filter, search or page parameter")
    void REQ_TODO_007_AC2_clear_completed_ignores_filter_search_and_page() {
        var me = signedIn();
        setDone(me, createTodoId(me, "alpha"), true);
        setDone(me, createTodoId(me, "beta"), true);
        createTodoId(me, "gamma");

        Reply r = clearCompleted(me, "?status=active&q=alpha&page=7");

        assertThat(r.<Integer>json("$.deletedCount")).isEqualTo(2);
        assertThat(me.get(TODOS).<List<String>>json("$.items[*].title")).containsExactly("gamma");
    }

    @Test
    @DisplayName("REQ-TODO-007 AC-4 with no completed to-dos clear completed answers 200 with deletedCount 0 and deletes nothing")
    void REQ_TODO_007_AC4_clear_completed_with_nothing_completed_is_200_zero() {
        var me = signedIn();
        createTodoId(me, "active one");

        Reply r = clearCompleted(me, "");

        assertThat(r.<Integer>json("$.deletedCount")).isZero();
        assertThat(me.get(TODOS).<Integer>json("$.totalItems")).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-TODO-007 AC-1 totals.completed gives the number shown in the confirmation, over all pages")
    void REQ_TODO_007_AC1_totals_completed_counts_all_completed_todos() {
        var me = signedIn();
        for (int i = 0; i < 22; i++) {
            setDone(me, createTodoId(me, "done " + i), true);
        }
        createTodoId(me, "active");

        Reply firstPage = me.get(TODOS + "?status=active");

        assertThat(firstPage.<Integer>json("$.totals.completed")).isEqualTo(22);
        assertThat(firstPage.<Integer>json("$.totals.active")).isEqualTo(1);
        assertThat(firstPage.<Integer>json("$.totals.all")).isEqualTo(23);
    }

    @Test
    @DisplayName("REQ-TODO-007 AC-2 'completed' is not a to-do id: reading it is not found, and nothing was deleted by reading it")
    void REQ_TODO_007_AC2_completed_is_not_a_todo_id() {
        var me = signedIn();
        setDone(me, createTodoId(me, "done and kept"), true);

        me.raw("GET", TODOS + "/completed", null, null).assertProblem(404, "NOT_FOUND");
        me.raw("PUT", TODOS + "/" + UUID.randomUUID() + "/completed", "{}", "application/json")
                .assertProblem(404, "NOT_FOUND");

        assertThat(me.get(TODOS).<Integer>json("$.totals.completed")).isEqualTo(1);
    }
}
