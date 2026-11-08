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

/** REQ-ISO-001 Per-user isolation, REQ-AUTH-005 AC-1 no cross-user operation, REQ-ACC-001 AC-2. */
class TodoIsolationApiTest extends IntegrationTestBase {

    private List<String> titles(Reply list) {
        return list.json("$.items[*].title");
    }

    @Test
    @DisplayName("REQ-ISO-001 AC-1 listing with any filter, search or page returns only my to-dos, never another user's")
    void REQ_ISO_001_AC1_list_never_contains_another_users_todos() {
        var alice = signedIn();
        var bob = signedIn();
        createTodoId(alice, "alice shared-word");
        setDone(alice, createTodoId(alice, "alice done shared-word"), true);
        for (int i = 0; i < 25; i++) {
            createTodoId(bob, "bob shared-word " + i);
        }
        setDone(bob, createTodoId(bob, "bob done shared-word"), true);

        for (String query : List.of("", "?status=active", "?status=completed", "?q=shared-word", "?q=bob", "?page=2", "?page=99", "?status=active&q=shared-word&page=1")) {
            Reply r = alice.get(TODOS + query);
            assertThat(titles(r)).as(query).allSatisfy(t -> assertThat(t).startsWith("alice"));
        }
        Reply all = alice.get(TODOS);
        assertThat(all.<Integer>json("$.totalItems")).isEqualTo(2);
        assertThat(all.<Integer>json("$.totals.all")).isEqualTo(2);
        assertThat(all.<Integer>json("$.totals.completed")).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-ISO-001 AC-2 reading, editing, toggling or deleting another user's to-do is not found, identical to a missing and a malformed id")
    void REQ_ISO_001_AC2_another_users_todo_is_not_found_like_any_missing_id() {
        var alice = signedIn();
        var bob = signedIn();
        String bobsId = bob.post(TODOS, todoBody("bob private", "2026-12-01", "HIGH", "bob secret")).json("$.id");
        String randomId = UUID.randomUUID().toString();
        String malformed = "not-a-uuid";
        String edit = todoBody("hijacked", "2027-01-01", "LOW", "hijacked");

        Reply[] get = {alice.getInvalid(TODOS + "/" + bobsId), alice.getInvalid(TODOS + "/" + randomId), alice.getInvalid(TODOS + "/" + malformed)};
        Reply[] put = {alice.putInvalid(TODOS + "/" + bobsId, edit), alice.putInvalid(TODOS + "/" + randomId, edit), alice.putInvalid(TODOS + "/" + malformed, edit)};
        String done = Json.object("done", true);
        Reply[] toggle = {alice.putInvalid(TODOS + "/" + bobsId + "/done", done), alice.putInvalid(TODOS + "/" + randomId + "/done", done), alice.putInvalid(TODOS + "/" + malformed + "/done", done)};
        Reply[] delete = {alice.deleteInvalid(TODOS + "/" + bobsId), alice.deleteInvalid(TODOS + "/" + randomId), alice.deleteInvalid(TODOS + "/" + malformed)};

        for (Reply[] group : new Reply[][] {get, put, toggle, delete}) {
            group[0].assertProblem(404, "NOT_FOUND");
            assertThat(group[1].status()).isEqualTo(group[0].status());
            assertThat(group[1].contentType()).isEqualTo(group[0].contentType());
            assertThat(group[1].body()).as("a missing id looks exactly like another user's id").isEqualTo(group[0].body());
            assertThat(group[2].body()).as("a malformed id looks exactly like another user's id").isEqualTo(group[0].body());
        }
        Reply stillThere = bob.get(TODOS + "/" + bobsId);
        assertThat(stillThere.status()).isEqualTo(200);
        assertThat(stillThere.<String>json("$.title")).isEqualTo("bob private");
        assertThat(stillThere.<String>json("$.description")).isEqualTo("bob secret");
        assertThat(stillThere.<Boolean>json("$.done")).isFalse();
    }

    @Test
    @DisplayName("REQ-ISO-001 AC-3 clear all completed deletes only the caller's completed to-dos")
    void REQ_ISO_001_AC3_clear_completed_touches_only_my_todos() {
        var alice = signedIn();
        var bob = signedIn();
        setDone(alice, createTodoId(alice, "alice done"), true);
        String aliceActive = createTodoId(alice, "alice active");
        setDone(bob, createTodoId(bob, "bob done 1"), true);
        setDone(bob, createTodoId(bob, "bob done 2"), true);
        String bobActive = createTodoId(bob, "bob active");

        Reply r = alice.send("DELETE", TODOS + "/completed", null, "application/json", ApiClient.Check.NONE);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.<Integer>json("$.deletedCount")).isEqualTo(1);
        assertThat(alice.get(TODOS).<List<String>>json("$.items[*].id")).containsExactly(aliceActive);
        Reply bobs = bob.get(TODOS);
        assertThat(bobs.<Integer>json("$.totalItems")).isEqualTo(3);
        assertThat(bobs.<Integer>json("$.totals.completed")).isEqualTo(2);
        assertThat(bobs.<List<String>>json("$.items[*].id")).contains(bobActive);
    }

    @Test
    @DisplayName("REQ-ISO-001 AC-4 a search for a term that only appears in another user's to-dos finds nothing and reveals nothing")
    void REQ_ISO_001_AC4_search_does_not_reveal_another_users_data() {
        var alice = signedIn();
        var bob = signedIn();
        createTodoId(alice, "ordinary");
        bob.post(TODOS, todoBody("bob title", "2026-12-01", "LOW", "xylophone-secret-term"));
        createTodoId(bob, "xylophone-secret-term in title");

        Reply r = alice.get(TODOS + "?q=xylophone-secret-term");

        assertThat(r.<List<Object>>json("$.items")).isEmpty();
        assertThat(r.<Integer>json("$.totalItems")).isZero();
        assertThat(r.<Integer>json("$.totals.all")).as("totals count only my own to-dos").isEqualTo(1);
        assertThat(r.body()).doesNotContain("bob").doesNotContain("xylophone-secret-term in title");
    }

    @Test
    @DisplayName("REQ-ISO-001 AC-1 the owner comes only from the session: a body naming another owner is refused and creates nothing")
    void REQ_ISO_001_AC1_owner_cannot_be_chosen_in_the_request() {
        var alice = signedIn();
        long before = db().count("SELECT count(*) FROM todos");

        Reply r = alice.postInvalid(TODOS, Json.object("title", "stolen", "dueDate", "2026-12-01", "userId", 1, "owner", "bob"));

        r.assertProblem(400, "VALIDATION_FAILED");
        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(before);
    }

    @Test
    @DisplayName("REQ-AUTH-005 AC-1 there is no operation that lists users or reads across users")
    void REQ_AUTH_005_AC1_no_user_listing_or_cross_user_endpoint() {
        var me = signedIn();

        for (String path : List.of("/api/v1/users", "/api/v1/users/1", "/api/v1/auth/users", "/api/v1/admin", "/api/v1/admin/users", "/api/v1/todos/all", "/api/v1/users/1/todos")) {
            Reply r = me.raw("GET", path, null, null);
            assertThat(r.status()).as(path).isIn(404, 405);
            assertThat(r.body()).as(path).doesNotContain("user-");
        }
    }

    @Test
    @DisplayName("REQ-ACC-001 AC-2 a second user who looks for the first user's to-dos by listing, searching or direct id cannot see or change them")
    void REQ_ACC_001_AC2_second_user_cannot_see_or_change_the_first_users_todos() {
        var first = signedIn();
        var second = signedIn();
        String id = first.post(TODOS, todoBody("first user's plan", "2026-12-01", "HIGH", "private")).json("$.id");

        assertThat(second.get(TODOS).<List<Object>>json("$.items")).isEmpty();
        assertThat(second.get(TODOS + "?q=plan").<List<Object>>json("$.items")).isEmpty();
        second.getInvalid(TODOS + "/" + id).assertProblem(404, "NOT_FOUND");
        second.putInvalid(TODOS + "/" + id, todoBody("mine now", "2026-12-01", "LOW", null)).assertProblem(404, "NOT_FOUND");
        second.deleteInvalid(TODOS + "/" + id).assertProblem(404, "NOT_FOUND");
        assertThat(first.get(TODOS + "/" + id).<String>json("$.title")).isEqualTo("first user's plan");
    }
}
