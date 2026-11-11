package app.todo.common;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Reply;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NFR-004 AC-2 / common-problem-v1.yaml conventions: a request body must be sent as application/json (a charset
 * parameter is allowed); any other Content-Type, including structured-syntax +json types, is refused with 415
 * UNSUPPORTED_MEDIA_TYPE and changes nothing.
 */
class JsonSuffixMediaTypeTest extends IntegrationTestBase {

    private static final List<String> SUFFIX_JSON_TYPES = List.of("application/merge-patch+json", "application/vnd.x+json");

    @Test
    @DisplayName("NFR-004 AC-2 createTodo with an application/*+json body is 415 UNSUPPORTED_MEDIA_TYPE and creates no to-do")
    void NFR_004_AC2_create_todo_rejects_plus_json_content_types() {
        var me = signedIn();
        long before = db().count("SELECT count(*) FROM todos");

        for (String type : SUFFIX_JSON_TYPES) {
            Reply r = me.raw("POST", TODOS, todoBody("suffix " + type), type);

            assertThat(r.status()).as("status for %s: %s", type, r.body()).isEqualTo(415);
            r.assertProblem(415, "UNSUPPORTED_MEDIA_TYPE");
            assertThat(r.<List<Object>>json("$.errors")).isEmpty();
        }
        assertThat(db().count("SELECT count(*) FROM todos")).isEqualTo(before);
    }

    @Test
    @DisplayName("NFR-004 AC-2 signUp with an application/*+json body is 415 UNSUPPORTED_MEDIA_TYPE and creates no user")
    void NFR_004_AC2_sign_up_rejects_plus_json_content_types() {
        long before = db().count("SELECT count(*) FROM users");

        for (String type : SUFFIX_JSON_TYPES) {
            Reply r = anonymous().raw("POST", SIGNUP, credentials("suffix-" + uid(), PASSWORD), type);

            assertThat(r.status()).as("status for %s: %s", type, r.body()).isEqualTo(415);
            r.assertProblem(415, "UNSUPPORTED_MEDIA_TYPE");
            assertThat(r.<List<Object>>json("$.errors")).isEmpty();
        }
        assertThat(db().count("SELECT count(*) FROM users")).isEqualTo(before);
    }

    @Test
    @DisplayName("NFR-004 AC-2 createTodo and signUp with application/json;charset=UTF-8 are still accepted")
    void NFR_004_AC2_application_json_with_charset_is_still_accepted() {
        var me = signedIn();

        Reply created = me.raw("POST", TODOS, todoBody("charset no space"), "application/json;charset=UTF-8");
        Reply signedUp = anonymous().raw("POST", SIGNUP, credentials("charset-" + uid(), PASSWORD), "application/json;charset=UTF-8");

        assertThat(created.status()).as(created.body()).isEqualTo(201);
        assertThat(signedUp.status()).as(signedUp.body()).isEqualTo(201);
    }
}
