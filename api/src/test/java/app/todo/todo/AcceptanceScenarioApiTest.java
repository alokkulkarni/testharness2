package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Reply;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * REQ-ACC-001 AC-1, backend half (the browser e2e scenario is the web QA's): a signed-in user adds, sees, searches,
 * filters, edits, completes and deletes their own to-dos through the API.
 */
class AcceptanceScenarioApiTest extends IntegrationTestBase {

    @Test
    @DisplayName("REQ-ACC-001 AC-1 a signed-in user can add, see, search, filter, edit, complete and delete their own to-dos")
    void REQ_ACC_001_AC1_the_whole_journey_works_through_the_api() {
        Account account = newAccount();
        var me = logIn(account);

        // add
        String milk = me.post(TODOS, todoBody("Buy milk", "2026-12-01", "HIGH", "two litres")).json("$.id");
        clock.advance(Duration.ofSeconds(1));
        String bread = me.post(TODOS, todoBody("Bake bread", "2026-12-02", null, null)).json("$.id");
        clock.advance(Duration.ofSeconds(1));
        me.post(TODOS, todoBody("Walk the dog", "2026-12-03", "LOW", null));

        // see
        Reply all = me.get(TODOS);
        assertThat(all.<List<String>>json("$.items[*].title")).containsExactly("Walk the dog", "Bake bread", "Buy milk");

        // search
        assertThat(me.get(TODOS + "?q=litres").<List<String>>json("$.items[*].title")).containsExactly("Buy milk");

        // edit
        Reply edited = me.put(TODOS + "/" + bread, todoBody("Bake sourdough", "2026-12-05", "MEDIUM", "overnight"));
        assertThat(edited.<String>json("$.title")).isEqualTo("Bake sourdough");

        // complete, then filter
        setDone(me, milk, true);
        assertThat(me.get(TODOS + "?status=completed").<List<String>>json("$.items[*].title")).containsExactly("Buy milk");
        assertThat(me.get(TODOS + "?status=active").<List<String>>json("$.items[*].title"))
                .containsExactly("Walk the dog", "Bake sourdough");

        // delete
        assertThat(me.delete(TODOS + "/" + bread).status()).isEqualTo(204);
        assertThat(me.get(TODOS).<List<String>>json("$.items[*].title")).containsExactly("Walk the dog", "Buy milk");

        // log out ends it
        assertThat(me.post(LOGOUT, null).status()).isEqualTo(204);
        me.getInvalid(TODOS).assertProblem(401, "UNAUTHENTICATED");
    }
}
