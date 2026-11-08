package app.todo.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Integration tests with the injected clock fixed at {@link MutableClock#START} and advanced only by the test
 * (ADR-0013). Provides signed-in clients and request bodies.
 */
@Import(TestClockConfiguration.class)
public abstract class IntegrationTestBase extends SpringContextTestBase {

    protected static final String SIGNUP = "/api/v1/auth/signup";
    protected static final String LOGIN = "/api/v1/auth/login";
    protected static final String LOGOUT = "/api/v1/auth/logout";
    protected static final String SESSION = "/api/v1/auth/session";
    protected static final String TODOS = "/api/v1/todos";
    protected static final String PASSWORD = "correct-horse-battery";

    /** An account created through the API. */
    public record Account(String identifier, String password) {}

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    protected void resetClock() {
        clock.reset();
    }

    protected static String credentials(String identifier, String password) {
        return Json.object("identifier", identifier, "password", password);
    }

    protected Account newAccount() {
        Account account = new Account("user-" + uid(), PASSWORD);
        Reply r = anonymous().post(SIGNUP, credentials(account.identifier(), account.password()));
        assertThat(r.status()).as("sign-up: %s", r.body()).isEqualTo(201);
        return account;
    }

    protected ApiClient logIn(Account account) {
        Reply r = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));
        assertThat(r.status()).as("login: %s", r.body()).isEqualTo(200);
        assertThat(r.sessionToken()).as("login sets the session cookie").isNotBlank();
        return anonymous().withSession(r.sessionToken());
    }

    /** A new account, logged in. */
    protected ApiClient signedIn() {
        return logIn(newAccount());
    }

    /**
     * A to-do request body. A Java null leaves the member out; {@link Json#NULL} sends a JSON null; a String is
     * sent as a JSON string.
     */
    protected static String todoBody(Object title, Object dueDate, Object priority, Object description) {
        StringBuilder sb = new StringBuilder("{");
        append(sb, "title", title);
        append(sb, "dueDate", dueDate);
        append(sb, "priority", priority);
        append(sb, "description", description);
        return sb.append('}').toString();
    }

    private static void append(StringBuilder sb, String name, Object value) {
        if (value == null) {
            return;
        }
        if (sb.length() > 1) {
            sb.append(',');
        }
        sb.append(Json.quote(name)).append(':').append(Json.value(value));
    }

    protected static String todoBody(String title) {
        return todoBody(title, "2026-12-01", "MEDIUM", null);
    }

    protected Reply createTodo(ApiClient client, String title) {
        Reply r = client.post(TODOS, todoBody(title));
        assertThat(r.status()).as("create %s: %s", title, r.body()).isEqualTo(201);
        return r;
    }

    protected String createTodoId(ApiClient client, String title) {
        return createTodo(client, title).json("$.id");
    }

    protected Reply setDone(ApiClient client, String id, boolean done) {
        Reply r = client.put(TODOS + "/" + id + "/done", Json.object("done", done));
        assertThat(r.status()).as("setDone: %s", r.body()).isEqualTo(200);
        return r;
    }
}
