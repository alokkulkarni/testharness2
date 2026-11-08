package app.todo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * REQ-AUTH-004 AC-1: every to-do operation refuses a caller without a valid session as unauthenticated and
 * changes nothing. The contract validator does not enforce the cookie scheme, so this is the 401 check of record
 * (ADR-0012).
 */
class AuthenticationRequiredApiTest extends IntegrationTestBase {

    /** One call per to-do operation (create, list, get, update, set done, delete, clear completed). */
    private record Call(String name, String method, String path, String body) {}

    private static List<Call> allTodoOperations() {
        String id = UUID.randomUUID().toString();
        String content = todoBody("changed", "2026-12-31", "HIGH", null);
        return List.of(
                new Call("listTodos", "GET", TODOS, null),
                new Call("createTodo", "POST", TODOS, todoBody("must not be created")),
                new Call("getTodo", "GET", TODOS + "/" + id, null),
                new Call("updateTodo", "PUT", TODOS + "/" + id, content),
                new Call("setTodoDone", "PUT", TODOS + "/" + id + "/done", Json.object("done", true)),
                new Call("deleteTodo", "DELETE", TODOS + "/" + id, null),
                new Call("clearCompletedTodos", "DELETE", TODOS + "/completed", null));
    }

    private Reply perform(ApiClient client, Call call) {
        return client.send(call.method(), call.path(), call.body(), "application/json", ApiClient.Check.NONE);
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-1 without a session every to-do operation is refused as unauthenticated")
    void REQ_AUTH_004_AC1_no_session_is_unauthenticated_on_every_operation() {
        long todosBefore = db().count("SELECT count(*) FROM todos");

        for (Call call : allTodoOperations()) {
            Reply r = perform(anonymous(), call);
            r.assertProblem(401, "UNAUTHENTICATED");
            assertThat(r.body()).as(call.name() + " returns no data").doesNotContain("must not be created");
        }

        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(todosBefore);
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-1 with an expired session every to-do operation is refused as unauthenticated")
    void REQ_AUTH_004_AC1_expired_session_is_unauthenticated_on_every_operation() {
        var me = signedIn();
        createTodo(me, "keep me");
        long todosBefore = db().count("SELECT count(*) FROM todos");
        clock.advance(Duration.ofHours(8).plusSeconds(1));

        for (Call call : allTodoOperations()) {
            perform(me, call).assertProblem(401, "UNAUTHENTICATED");
        }

        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(todosBefore);
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-1 with a logged-out session every to-do operation is refused as unauthenticated")
    void REQ_AUTH_004_AC1_logged_out_session_is_unauthenticated_on_every_operation() {
        var me = signedIn();
        createTodo(me, "keep me too");
        long todosBefore = db().count("SELECT count(*) FROM todos");
        me.post(LOGOUT, null);

        for (Call call : allTodoOperations()) {
            perform(me, call).assertProblem(401, "UNAUTHENTICATED");
        }

        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(todosBefore);
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-1 an unknown or garbage session cookie is unauthenticated")
    void REQ_AUTH_004_AC1_unknown_cookie_is_unauthenticated() {
        for (String junk : List.of("garbage", "", "0".repeat(64), "A".repeat(500))) {
            anonymous().withSession(junk).getInvalid(TODOS).assertProblem(401, "UNAUTHENTICATED");
        }
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-1 the unauthenticated body is the same in every case (none, expired, logged out)")
    void REQ_AUTH_004_AC1_unauthenticated_body_is_identical_in_every_case() {
        var expired = signedIn();
        var loggedOut = signedIn();
        loggedOut.post(LOGOUT, null);
        clock.advance(Duration.ofHours(9));

        String none = anonymous().getInvalid(TODOS).body();

        assertThat(expired.getInvalid(TODOS).body()).isEqualTo(none);
        assertThat(loggedOut.getInvalid(TODOS).body()).isEqualTo(none);
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-1 the session is checked first: no session gives 401 before the client header (403) and before the body (400)")
    void REQ_AUTH_004_AC1_session_is_checked_before_header_and_body() {
        anonymous().withoutClientHeader().postInvalid(TODOS, todoBody("x")).assertProblem(401, "UNAUTHENTICATED");
        anonymous().postInvalid(TODOS, "{not json").assertProblem(401, "UNAUTHENTICATED");
        anonymous().raw("PATCH", TODOS + "/" + UUID.randomUUID(), "{}", "application/json")
                .assertProblem(401, "UNAUTHENTICATED");
    }

    @Test
    @DisplayName("NFR-004 AC-2 with a session but without X-Todo-Client every unsafe to-do operation is 403 CLIENT_HEADER_MISSING")
    void NFR_004_AC2_missing_client_header_is_403_on_unsafe_operations() {
        var me = signedIn().withoutClientHeader();
        long todosBefore = db().count("SELECT count(*) FROM todos");

        for (Call call : allTodoOperations()) {
            Reply r = perform(me, call);
            if (call.method().equals("GET")) {
                assertThat(r.status()).as(call.name() + " is safe and needs no header").isNotEqualTo(403);
            } else {
                r.assertProblem(403, "CLIENT_HEADER_MISSING");
            }
        }

        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(todosBefore);
    }

    @Test
    @DisplayName("NFR-004 AC-2 a wrong X-Todo-Client value is refused like a missing one")
    void NFR_004_AC2_wrong_client_header_value_is_403() {
        var me = signedIn();

        Reply r = me.raw("POST", TODOS, todoBody("x"), "application/json");
        assertThat(r.status()).isEqualTo(201); // sanity: the helper's header is the right one

        Reply wrong = rawWithHeader(me, "mobile");

        wrong.assertProblem(403, "CLIENT_HEADER_MISSING");
    }

    private Reply rawWithHeader(ApiClient me, String headerValue) {
        try {
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(me.baseUrl() + TODOS))
                    .header("Content-Type", "application/json")
                    .header("X-Todo-Client", headerValue)
                    .header("Cookie", "todo_session=" + me.sessionToken())
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(todoBody("x")))
                    .build();
            var response = java.net.http.HttpClient.newHttpClient()
                    .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body(), response.headers().map());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
