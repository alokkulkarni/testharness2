package app.todo.common;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * NFR-004 AC-2 / ADR-0011: one request per error source. Each answer is application/problem+json, valid against
 * the Problem schema of common-problem-v1.yaml (checked directly: these answers have no contract operation), has
 * the listed code, and carries no stack trace, exception text or SQL, and never the whitelabel or Spring default body.
 */
class ErrorSourcesTest extends IntegrationTestBase {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private void assertNoInternals(Reply r) {
        assertThat(r.body()).doesNotContain("Exception").doesNotContain("at app.todo").doesNotContain("org.springframework")
                .doesNotContain("java.lang").doesNotContain("SQLite").doesNotContain("SELECT ").doesNotContain("stackTrace")
                .doesNotContain("Whitelabel").doesNotContain("\"trace\"").doesNotContain("\"timestamp\"").doesNotContain("\"path\"");
        assertThat(r.has("$.error")).as("Spring Boot's default error body is never used").isFalse();
        assertThat(r.<List<Object>>json("$.errors")).as("errors is always present").isNotNull();
    }

    private void assertSource(Reply r, int status, String code) {
        r.assertProblem(status, code);
        assertNoInternals(r);
    }

    @Test
    @DisplayName("NFR-004 AC-2 401 from the security entry point is a UNAUTHENTICATED Problem")
    void NFR_004_AC2_401_from_the_security_entry_point() {
        assertSource(anonymous().raw("GET", TODOS, null, null), 401, "UNAUTHENTICATED");
        assertSource(anonymous().raw("GET", SESSION, null, null), 401, "UNAUTHENTICATED");
    }

    @Test
    @DisplayName("NFR-004 AC-2 403 from the client-header filter is a CLIENT_HEADER_MISSING Problem")
    void NFR_004_AC2_403_from_the_client_header_filter() {
        var me = signedIn().withoutClientHeader();

        Reply r = me.raw("POST", TODOS, todoBody("x"), "application/json");

        assertSource(r, 403, "CLIENT_HEADER_MISSING");
        assertThat(r.<List<Object>>json("$.errors")).isEmpty();
    }

    @Test
    @DisplayName("NFR-004 AC-2 405 for a method not defined for the path has an Allow header and an empty errors list")
    void NFR_004_AC2_405_method_not_allowed() {
        var me = signedIn();
        String id = createTodoId(me, "for 405");

        Reply patch = me.raw("PATCH", TODOS + "/" + id, "{}", "application/json");
        Reply deleteList = me.raw("DELETE", TODOS, null, null);
        Reply getLogin = anonymous().raw("GET", LOGIN, null, null);

        assertSource(patch, 405, "METHOD_NOT_ALLOWED");
        assertThat(patch.header("allow")).contains("GET", "PUT", "DELETE");
        assertThat(patch.<List<Object>>json("$.errors")).isEmpty();
        assertSource(deleteList, 405, "METHOD_NOT_ALLOWED");
        assertThat(deleteList.header("allow")).contains("GET", "POST");
        assertSource(getLogin, 405, "METHOD_NOT_ALLOWED");
        assertThat(getLogin.header("allow")).contains("POST");
    }

    @Test
    @DisplayName("NFR-004 AC-2 415 for a body with another Content-Type or none is an UNSUPPORTED_MEDIA_TYPE Problem")
    void NFR_004_AC2_415_unsupported_media_type() {
        var me = signedIn();
        long before = db().count("SELECT count(*) FROM todos");

        Reply plain = me.raw("POST", TODOS, todoBody("x"), "text/plain");
        Reply none = me.raw("POST", TODOS, todoBody("x"), null);
        Reply form = me.raw("POST", TODOS, "title=x", "application/x-www-form-urlencoded");

        assertSource(plain, 415, "UNSUPPORTED_MEDIA_TYPE");
        assertSource(none, 415, "UNSUPPORTED_MEDIA_TYPE");
        assertSource(form, 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(plain.<List<Object>>json("$.errors")).isEmpty();
        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(before);
    }

    @Test
    @DisplayName("NFR-004 AC-2 a charset parameter on application/json is allowed")
    void NFR_004_AC2_json_with_charset_is_accepted() {
        Reply r = signedIn().raw("POST", TODOS, todoBody("with charset"), "application/json; charset=UTF-8");

        assertThat(r.status()).isEqualTo(201);
    }

    @Test
    @DisplayName("NFR-004 AC-2 404 for an unknown route under /api is a NOT_FOUND Problem, with or without a session")
    void NFR_004_AC2_404_unknown_route() {
        assertSource(signedIn().raw("GET", "/api/v1/nope", null, null), 404, "NOT_FOUND");
        assertSource(anonymous().raw("GET", "/api/v1/nope", null, null), 404, "NOT_FOUND");
        assertSource(anonymous().raw("GET", "/api/nope/deeper/still", null, null), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("NFR-004 AC-2 400 for unparseable or missing JSON is a MALFORMED_REQUEST Problem with an empty errors list")
    void NFR_004_AC2_400_malformed_json() {
        var me = signedIn();
        String id = createTodoId(me, "for malformed");

        List<Reply> replies = List.of(
                me.raw("POST", TODOS, "{not json", "application/json"),
                me.raw("POST", TODOS, "", "application/json"),
                me.raw("PUT", TODOS + "/" + id, "[1,2", "application/json"),
                me.raw("PUT", TODOS + "/" + id + "/done", "nope", "application/json"),
                anonymous().raw("POST", SIGNUP, "{\"identifier\":", "application/json"),
                anonymous().raw("POST", LOGIN, "", "application/json"));

        for (Reply r : replies) {
            assertSource(r, 400, "MALFORMED_REQUEST");
            assertThat(r.<List<Object>>json("$.errors")).isEmpty();
        }
    }

    @Test
    @DisplayName("NFR-004 AC-2 400 from the request firewall (a ';' or an encoded '/' in the path) is a REQUEST_REJECTED Problem")
    void NFR_004_AC2_400_request_rejected_by_the_firewall() {
        var me = signedIn();

        for (Reply r : List.of(
                me.raw("GET", TODOS + ";jsessionid=1", null, null),
                me.raw("GET", TODOS + "/a%2Fb", null, null),
                anonymous().raw("GET", "/api/v1/todos;x=1", null, null))) {
            assertSource(r, 400, "REQUEST_REJECTED");
            assertThat(r.<List<Object>>json("$.errors")).isEmpty();
        }
    }

    @Test
    @DisplayName("NFR-004 AC-2 the container's /error forward of a 413 is a REQUEST_REJECTED Problem and of a 500 an INTERNAL_ERROR Problem without the exception text")
    void NFR_004_AC2_error_controller_answers_forwarded_413_and_500() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        Reply tooLarge = forwardedError(mockMvc, 413, "Payload too large with secret detail", null);
        Reply internal = forwardedError(mockMvc, 500, "boom", new IllegalStateException("secret SQL: select * from users"));

        assertSource(tooLarge, 413, "REQUEST_REJECTED");
        assertThat(tooLarge.body()).doesNotContain("secret detail");
        assertSource(internal, 500, "INTERNAL_ERROR");
        assertThat(internal.body()).doesNotContain("secret").doesNotContain("boom").doesNotContain("select");
    }

    @Test
    @DisplayName("NFR-004 AC-2 a forwarded 404 and a forwarded 405 are NOT_FOUND and METHOD_NOT_ALLOWED Problems")
    void NFR_004_AC2_error_controller_answers_forwarded_404_and_405() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        assertSource(forwardedError(mockMvc, 404, "Not Found", null), 404, "NOT_FOUND");
        assertSource(forwardedError(mockMvc, 405, "Method Not Allowed", null), 405, "METHOD_NOT_ALLOWED");
    }

    private Reply forwardedError(MockMvc mockMvc, int status, String message, Throwable exception) throws Exception {
        var request = MockMvcRequestBuilders.get("/error")
                .requestAttr("jakarta.servlet.error.status_code", status)
                .requestAttr("jakarta.servlet.error.message", message)
                .requestAttr("jakarta.servlet.error.request_uri", "/api/v1/forwarded");
        if (exception != null) {
            request = request.requestAttr("jakarta.servlet.error.exception", exception);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        return new Reply(
                result.getResponse().getStatus(),
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                Map.of("content-type", List.of(String.valueOf(result.getResponse().getContentType()))));
    }

    @Test
    @DisplayName("NFR-004 AC-2 invalid input sent directly to the API is refused by the server, not only the UI (sign-up, create, list)")
    void NFR_004_AC2_the_server_validates_input_itself() {
        var me = signedIn();

        assertSource(anonymous().raw("POST", SIGNUP, Json.object("identifier", "ab", "password", "short"), "application/json"), 400, "VALIDATION_FAILED");
        assertSource(me.raw("POST", TODOS, Json.object("title", "", "dueDate", "bad"), "application/json"), 400, "VALIDATION_FAILED");
        assertSource(me.raw("GET", TODOS + "?page=0&status=bogus", null, null), 400, "VALIDATION_FAILED");
    }

    @Test
    @DisplayName("NFR-004 AC-2 a malformed to-do id is the same NOT_FOUND Problem as a missing one")
    void NFR_004_AC2_malformed_id_is_not_found() {
        var me = signedIn();

        assertSource(me.raw("GET", TODOS + "/" + UUID.randomUUID(), null, null), 404, "NOT_FOUND");
        assertSource(me.raw("GET", TODOS + "/not-a-uuid", null, null), 404, "NOT_FOUND");
    }
}
