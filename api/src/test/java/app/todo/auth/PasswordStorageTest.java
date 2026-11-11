package app.todo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.LogCapture;
import app.todo.testsupport.Reply;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** REQ-AUTH-003 Password storage, REQ-AUTH-005 AC-2 (one role) and NFR-004 AC-1. */
class PasswordStorageTest extends IntegrationTestBase {

    private String hashOf(String identifier) {
        return db().text("SELECT password_hash FROM users WHERE identifier_key = ?", identifier.toLowerCase(Locale.ROOT));
    }

    @Test
    @DisplayName("REQ-AUTH-003 AC-1 the database holds a salted Argon2id hash and no plaintext password")
    void REQ_AUTH_003_AC1_stored_value_is_an_argon2id_hash_not_the_password() {
        String password = "Plain-Text-Marker-" + uid();
        String id = "hash-" + uid();
        anonymous().post(SIGNUP, credentials(id, password));

        String stored = hashOf(id);

        assertThat(stored).startsWith("$argon2id$");
        assertThat(stored).doesNotContain(password);
        assertThat(db().count("SELECT count(*) FROM users WHERE password_hash = ?", password)).isZero();
    }

    @Test
    @DisplayName("REQ-AUTH-003 AC-2 two accounts with the same password have different hashes (unique salt)")
    void REQ_AUTH_003_AC2_same_password_gives_different_hashes() {
        String a = "salt-a-" + uid();
        String b = "salt-b-" + uid();
        anonymous().post(SIGNUP, credentials(a, PASSWORD));
        anonymous().post(SIGNUP, credentials(b, PASSWORD));

        assertThat(hashOf(a)).isNotEqualTo(hashOf(b));
    }

    @Test
    @DisplayName("REQ-AUTH-003 AC-1 the whole password counts: 100-character passwords differing only after byte 72 do not cross-verify")
    void REQ_AUTH_003_AC1_passwords_differing_after_byte_72_do_not_cross_verify() {
        String prefix = "p".repeat(89);
        String original = prefix + "A" + "z".repeat(10);
        String variant = prefix + "B" + "z".repeat(10);
        String id = "long-" + uid();
        anonymous().post(SIGNUP, credentials(id, original));

        assertThat(anonymous().post(LOGIN, credentials(id, original)).status()).isEqualTo(200);
        anonymous().post(LOGIN, credentials(id, variant)).assertProblem(401, "INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("REQ-AUTH-003 AC-3 no API response or log line contains a password, a password hash or the session token")
    void REQ_AUTH_003_AC3_no_response_or_log_line_contains_password_or_hash() {
        String password = "Sentinel-Password-" + uid();
        String id = "secret-" + uid();
        try (LogCapture logs = new LogCapture()) {
            Reply signUp = anonymous().post(SIGNUP, credentials(id, password));
            Reply taken = anonymous().post(SIGNUP, credentials(id, password));
            Reply wrong = anonymous().post(LOGIN, credentials(id, password + "-wrong"));
            Reply login = anonymous().post(LOGIN, credentials(id, password));
            var me = anonymous().withSession(login.sessionToken());
            Reply session = me.get(SESSION);
            Reply invalid = anonymous().postInvalid(SIGNUP, credentials(id + "!bad", password));
            String hash = hashOf(id);
            String token = login.sessionToken();

            for (Reply r : List.of(signUp, taken, wrong, login, session, invalid)) {
                assertThat(r.body()).doesNotContain(password).doesNotContain(hash).doesNotContain("argon2");
            }
            assertThat(logs.events()).isNotEmpty();
            for (var event : logs.events()) {
                String text = LogCapture.text(event);
                assertThat(text).doesNotContain(password).doesNotContain(hash).doesNotContain(token);
            }
        }
    }

    @Test
    @DisplayName("REQ-AUTH-003 AC-1 the session table holds a SHA-256 of the token, never the token itself")
    void REQ_AUTH_003_AC1_session_token_is_stored_only_as_a_hash() {
        Account account = newAccount();
        Reply login = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));
        String token = login.sessionToken();

        long withRawToken = db().count("SELECT count(*) FROM sessions WHERE token_hash = ?", token);
        String stored = db().text(
                "SELECT s.token_hash FROM sessions s JOIN users u ON u.id = s.user_id WHERE u.identifier_key = ?",
                account.identifier().toLowerCase(Locale.ROOT));

        assertThat(withRawToken).isZero();
        assertThat(stored).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("REQ-AUTH-005 AC-2 every account has the same single role: the users table has no role or permission column")
    void REQ_AUTH_005_AC2_users_table_has_no_role_column() {
        assertThat(db().columns("users"))
                .containsExactlyInAnyOrder("id", "identifier", "identifier_key", "password_hash", "created_at");
    }

    @Test
    @DisplayName("REQ-AUTH-005 AC-2 two new accounts get identical responses on every operation")
    void REQ_AUTH_005_AC2_two_accounts_get_identical_responses() {
        var one = signedIn();
        var two = signedIn();

        for (String path : List.of(TODOS, TODOS + "?status=active", TODOS + "?status=completed&q=zzz", TODOS + "?page=3")) {
            Reply a = one.get(path);
            Reply b = two.get(path);
            assertThat(a.status()).as(path).isEqualTo(b.status());
            assertThat(a.body()).as(path).isEqualTo(b.body());
        }
    }
}
