package app.todo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Reply;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** REQ-AUTH-002 Login, REQ-AUTH-004 AC-3 and AC-4 logout and expiry (identity-api-v1.yaml logIn, logOut, getSession). */
class LoginSessionApiTest extends IntegrationTestBase {

    private long sessionsOf(String identifier) {
        return db().count(
                "SELECT count(*) FROM sessions s JOIN users u ON u.id = s.user_id WHERE u.identifier_key = ?",
                identifier.toLowerCase(Locale.ROOT));
    }

    // ---- REQ-AUTH-002 AC-1 ----

    @Test
    @DisplayName("REQ-AUTH-002 AC-1 correct identifier and password sign the user in and return the session")
    void REQ_AUTH_002_AC1_correct_credentials_sign_in() {
        Account account = newAccount();

        Reply login = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));

        assertThat(login.status()).isEqualTo(200);
        assertThat(login.<String>json("$.user.identifier")).isEqualTo(account.identifier());
        assertThat(login.sessionToken()).isNotBlank();
        assertThat(sessionsOf(account.identifier())).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-1 after login the user sees their own (empty) to-do list")
    void REQ_AUTH_002_AC1_user_sees_their_own_list_after_login() {
        var me = signedIn();

        Reply list = me.get(TODOS);

        assertThat(list.status()).isEqualTo(200);
        assertThat(list.<Integer>json("$.totalItems")).isZero();
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-4 the session cookie is HttpOnly, SameSite=Strict, Path=/, Max-Age=28800 and not Secure locally")
    void REQ_AUTH_002_AC4_session_cookie_attributes() {
        Account account = newAccount();

        Reply login = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));

        String line = login.sessionCookieLine().toLowerCase(Locale.ROOT);
        assertThat(line).contains("httponly", "samesite=strict", "path=/", "max-age=28800");
        assertThat(line).doesNotContain("secure;").doesNotEndWith("secure");
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-4 the session lasts 8 hours from login: expiresAt = login time + 8 h")
    void REQ_AUTH_002_AC4_expires_at_is_login_plus_eight_hours() {
        Account account = newAccount();
        clock.advance(Duration.ofMinutes(5));
        Instant loginTime = clock.instant();

        Reply login = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));

        assertThat(Instant.parse(login.json("$.expiresAt"))).isEqualTo(loginTime.plus(Duration.ofHours(8)));
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-4 getSession returns the identifier as stored at sign-up and the expiresAt set at login")
    void REQ_AUTH_002_AC4_get_session_returns_stored_identifier_and_login_expiry() {
        String stored = "MixedCase" + uid();
        anonymous().post(SIGNUP, credentials(stored, PASSWORD));
        Reply login = anonymous().post(LOGIN, credentials(stored.toLowerCase(Locale.ROOT), PASSWORD));
        assertThat(login.status()).isEqualTo(200);
        var me = anonymous().withSession(login.sessionToken());

        Reply session = me.get(SESSION);

        assertThat(session.status()).isEqualTo(200);
        assertThat(session.<String>json("$.user.identifier")).isEqualTo(stored);
        assertThat(Instant.parse(session.json("$.expiresAt"))).isEqualTo(Instant.parse(login.json("$.expiresAt")));
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-4 reloading keeps the user signed in: the cookie keeps working for 8 hours, activity does not extend it")
    void REQ_AUTH_002_AC4_session_is_valid_until_exactly_eight_hours_and_not_extended() {
        Account account = newAccount();
        Reply login = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));
        var me = anonymous().withSession(login.sessionToken());
        Instant expiresAt = Instant.parse(login.json("$.expiresAt"));

        clock.advance(Duration.ofHours(7));
        Reply midway = me.get(SESSION);
        assertThat(midway.status()).isEqualTo(200);
        assertThat(Instant.parse(midway.json("$.expiresAt"))).as("not extended by activity").isEqualTo(expiresAt);
        assertThat(midway.sessionCookieLine()).as("no refreshed cookie").isNull();

        clock.advance(Duration.ofHours(1).minusMillis(1));
        assertThat(me.get(SESSION).status()).as("1 ms before expiry").isEqualTo(200);

        clock.advance(Duration.ofMillis(1));
        me.getInvalid(SESSION).assertProblem(401, "UNAUTHENTICATED");
    }

    // ---- REQ-AUTH-004 AC-4 expiry ----

    @Test
    @DisplayName("REQ-AUTH-004 AC-4 an expired session is refused on a to-do call as unauthenticated and its row is removed")
    void REQ_AUTH_004_AC4_expired_session_is_refused_and_removed() {
        Account account = newAccount();
        var me = logIn(account);
        assertThat(me.get(TODOS).status()).isEqualTo(200);

        clock.advance(Duration.ofHours(8).plusMinutes(1));

        me.getInvalid(TODOS).assertProblem(401, "UNAUTHENTICATED");
        assertThat(sessionsOf(account.identifier())).as("expired row deleted").isZero();
    }

    // ---- REQ-AUTH-002 AC-2 / AC-3 ----

    @Test
    @DisplayName("REQ-AUTH-002 AC-2 a wrong password is refused with INVALID_CREDENTIALS and no session is created")
    void REQ_AUTH_002_AC2_wrong_password_is_refused() {
        Account account = newAccount();
        long before = db().count("SELECT count(*) FROM sessions");

        Reply r = anonymous().post(LOGIN, credentials(account.identifier(), "not-the-password"));

        r.assertProblem(401, "INVALID_CREDENTIALS");
        assertThat(r.<String>json("$.detail")).isEqualTo("Invalid identifier or password.");
        assertThat(r.sessionCookieLine()).isNull();
        assertThat(db().count("SELECT count(*) FROM sessions")).isEqualTo(before);
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-3 an unknown identifier, a wrong password and a password over 128 characters give byte-identical 401 responses")
    void REQ_AUTH_002_AC3_login_responses_do_not_reveal_whether_the_identifier_exists() {
        Account account = newAccount();

        Reply wrongPassword = anonymous().post(LOGIN, credentials(account.identifier(), "not-the-password"));
        Reply unknownIdentifier = anonymous().post(LOGIN, credentials("nobody-" + uid(), "not-the-password"));
        Reply tooLongPassword = anonymous().post(LOGIN, credentials(account.identifier(), "p".repeat(129)));
        Reply unknownWithTooLong = anonymous().post(LOGIN, credentials("nobody-" + uid(), "p".repeat(129)));

        wrongPassword.assertProblem(401, "INVALID_CREDENTIALS");
        for (Reply other : new Reply[] {unknownIdentifier, tooLongPassword, unknownWithTooLong}) {
            assertThat(other.status()).isEqualTo(wrongPassword.status());
            assertThat(other.contentType()).isEqualTo(wrongPassword.contentType());
            assertThat(other.body()).isEqualTo(wrongPassword.body());
            assertThat(other.sessionCookieLine()).isNull();
        }
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-3 no format rule applies at login: a malformed identifier is just an unknown identifier (401)")
    void REQ_AUTH_002_AC3_no_format_check_at_login() {
        Reply r = anonymous().post(LOGIN, credentials("not valid @@ identifier", "whatever-1"));

        r.assertProblem(401, "INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-2 an empty identifier or an empty password is refused with a field error")
    void REQ_AUTH_002_AC2_empty_fields_are_refused_with_field_errors() {
        Reply noId = anonymous().postInvalid(LOGIN, credentials("", "x"));
        Reply blankId = anonymous().postInvalid(LOGIN, credentials("   ", "x"));
        Reply noPassword = anonymous().postInvalid(LOGIN, credentials("someone", ""));

        noId.assertProblem(400, "VALIDATION_FAILED");
        assertThat(noId.errorFields()).contains("identifier");
        blankId.assertProblem(400, "VALIDATION_FAILED");
        assertThat(blankId.errorFields()).contains("identifier");
        noPassword.assertProblem(400, "VALIDATION_FAILED");
        assertThat(noPassword.errorFields()).contains("password");
    }

    // ---- REQ-AUTH-002 AC-5 ----

    @Test
    @DisplayName("REQ-AUTH-002 AC-5 login works with a different letter case of the identifier")
    void REQ_AUTH_002_AC5_login_is_case_insensitive() {
        String stored = "Alice" + uid() + "@Example.com";
        anonymous().post(SIGNUP, credentials(stored, PASSWORD));

        for (String variant : new String[] {stored.toUpperCase(Locale.ROOT), stored.toLowerCase(Locale.ROOT), stored}) {
            Reply r = anonymous().post(LOGIN, credentials(variant, PASSWORD));
            assertThat(r.status()).as(variant).isEqualTo(200);
            assertThat(r.<String>json("$.user.identifier")).as("shown as stored at sign-up").isEqualTo(stored);
        }
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-5 the login identifier is trimmed like at sign-up, including U+00A0 padding")
    void REQ_AUTH_002_AC5_login_identifier_is_trimmed() {
        Account account = newAccount();

        Reply r = anonymous().post(LOGIN, credentials("  " + account.identifier() + " 　", account.password()));

        assertThat(r.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("REQ-AUTH-002 AC-1 each login creates its own session; two sessions of one user are independent")
    void REQ_AUTH_002_AC1_each_login_creates_its_own_session() {
        Account account = newAccount();
        Reply first = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));
        Reply second = anonymous().post(LOGIN, credentials(account.identifier(), account.password()));

        assertThat(first.sessionToken()).isNotEqualTo(second.sessionToken());
        assertThat(sessionsOf(account.identifier())).isEqualTo(2);

        anonymous().withSession(first.sessionToken()).post(LOGOUT, null);

        assertThat(anonymous().withSession(second.sessionToken()).get(SESSION).status()).isEqualTo(200);
    }

    // ---- REQ-AUTH-004 AC-3 logout ----

    @Test
    @DisplayName("REQ-AUTH-004 AC-3 logout ends the session: 204, cookie cleared, later calls with that session are unauthenticated")
    void REQ_AUTH_004_AC3_logout_ends_the_session() {
        Account account = newAccount();
        var me = logIn(account);

        Reply out = me.post(LOGOUT, null);

        assertThat(out.status()).isEqualTo(204);
        assertThat(out.body()).isEmpty();
        assertThat(out.sessionCookieLine().toLowerCase(Locale.ROOT)).startsWith("todo_session=;").contains("max-age=0");
        me.getInvalid(SESSION).assertProblem(401, "UNAUTHENTICATED");
        me.getInvalid(TODOS).assertProblem(401, "UNAUTHENTICATED");
        assertThat(sessionsOf(account.identifier())).isZero();
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-3 logout is 204 also when there is no session or it already ended")
    void REQ_AUTH_004_AC3_logout_without_session_is_204() {
        assertThat(anonymous().post(LOGOUT, null).status()).isEqualTo(204);
        assertThat(anonymous().withSession("not-a-real-token").post(LOGOUT, null).status()).isEqualTo(204);
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-1 getSession without a session is unauthenticated")
    void REQ_AUTH_004_AC1_get_session_without_cookie_is_unauthenticated() {
        anonymous().get(SESSION).assertProblem(401, "UNAUTHENTICATED");
    }

    @Test
    @DisplayName("NFR-004 AC-2 the X-Todo-Client header is required on sign-up, login and logout (403 CLIENT_HEADER_MISSING)")
    void NFR_004_AC2_unsafe_auth_operations_need_the_client_header() {
        var noHeader = anonymous().withoutClientHeader();

        noHeader.postInvalid(SIGNUP, credentials("x-" + uid(), PASSWORD)).assertProblem(403, "CLIENT_HEADER_MISSING");
        noHeader.postInvalid(LOGIN, credentials("x-" + uid(), PASSWORD)).assertProblem(403, "CLIENT_HEADER_MISSING");
        noHeader.postInvalid(LOGOUT, null).assertProblem(403, "CLIENT_HEADER_MISSING");
    }
}
