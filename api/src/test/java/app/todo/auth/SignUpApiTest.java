package app.todo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** REQ-AUTH-001 Self sign-up, through the HTTP API (identity-api-v1.yaml signUp). */
class SignUpApiTest extends IntegrationTestBase {

    private static final String GRIN = "😀"; // one code point, two UTF-16 units

    private Reply signUpInvalid(String identifier, String password) {
        return anonymous().postInvalid(SIGNUP, credentials(identifier, password));
    }

    private void assertRefusedOn(Reply r, String field) {
        r.assertProblem(400, "VALIDATION_FAILED");
        assertThat(r.errorFields()).contains(field);
    }

    private long accountsWithKey(String identifier) {
        return db().count("SELECT count(*) FROM users WHERE identifier_key = ?", identifier.toLowerCase(Locale.ROOT));
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-1 a valid identifier and password create an account and do not start a session")
    void REQ_AUTH_001_AC1_creates_an_account_without_starting_a_session() {
        String id = "alice-" + uid();
        Reply r = anonymous().post(SIGNUP, credentials(id, PASSWORD));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.<String>json("$.user.identifier")).isEqualTo(id);
        assertThat(r.sessionCookieLine()).as("sign-up does not set a session cookie").isNull();
        assertThat(accountsWithKey(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-1 the new account can log in with the same identifier and password")
    void REQ_AUTH_001_AC1_new_account_can_log_in() {
        String id = "bob-" + uid();
        anonymous().post(SIGNUP, credentials(id, PASSWORD));

        Reply login = anonymous().post(LOGIN, credentials(id, PASSWORD));

        assertThat(login.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-4 no verification step: the account is usable straight after sign-up")
    void REQ_AUTH_001_AC4_no_email_verification_step() {
        String id = "carol" + uid() + "@example.com";
        assertThat(anonymous().post(SIGNUP, credentials(id, PASSWORD)).status()).isEqualTo(201);

        Reply login = anonymous().post(LOGIN, credentials(id, PASSWORD));

        assertThat(login.status()).isEqualTo(200);
        assertThat(login.<String>json("$.user.identifier")).isEqualTo(id);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-7 the one identifier field accepts an email address and a username")
    void REQ_AUTH_001_AC7_identifier_accepts_email_or_username() {
        assertThat(anonymous().post(SIGNUP, credentials("name-" + uid(), PASSWORD)).status()).isEqualTo(201);
        assertThat(anonymous().post(SIGNUP, credentials("mail" + uid() + "@example.org", PASSWORD)).status())
                .isEqualTo(201);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-2 a taken identifier is refused as taken and no second account is created")
    void REQ_AUTH_001_AC2_taken_identifier_is_refused() {
        String id = "dave-" + uid();
        anonymous().post(SIGNUP, credentials(id, PASSWORD));

        Reply second = anonymous().post(SIGNUP, credentials(id, "another-password-1"));

        second.assertProblem(409, "IDENTIFIER_TAKEN");
        assertThat(second.errorFields()).containsExactly("identifier");
        assertThat(second.errorCodeFor("identifier")).isEqualTo("TAKEN");
        assertThat(accountsWithKey(id)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"upper", "lower", "mixed"})
    @DisplayName("REQ-AUTH-001 AC-6 an identifier that differs only in letter case is a duplicate")
    void REQ_AUTH_001_AC6_case_variants_are_duplicates(String variant) {
        String id = "Alice" + uid() + "@Example.com";
        assertThat(anonymous().post(SIGNUP, credentials(id, PASSWORD)).status()).isEqualTo(201);
        String other = switch (variant) {
            case "upper" -> id.toUpperCase(Locale.ROOT);
            case "lower" -> id.toLowerCase(Locale.ROOT);
            default -> "aLiCe" + id.substring(5);
        };

        Reply second = anonymous().post(SIGNUP, credentials(other, PASSWORD));

        second.assertProblem(409, "IDENTIFIER_TAKEN");
        assertThat(accountsWithKey(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-6 a username that differs only in letter case is a duplicate")
    void REQ_AUTH_001_AC6_username_case_variant_is_duplicate() {
        String id = "Zed" + uid();
        anonymous().post(SIGNUP, credentials(id, PASSWORD));

        anonymous().post(SIGNUP, credentials(id.toLowerCase(Locale.ROOT), PASSWORD)).assertProblem(409, "IDENTIFIER_TAKEN");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-6 two concurrent sign-ups with one identifier create exactly one account")
    void REQ_AUTH_001_AC6_concurrent_duplicate_creates_one_account() throws Exception {
        String id = "race-" + uid();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = pool.submit(() -> {
                start.await();
                return anonymous().post(SIGNUP, credentials(id, PASSWORD)).status();
            });
            var b = pool.submit(() -> {
                start.await();
                return anonymous().post(SIGNUP, credentials(id.toUpperCase(Locale.ROOT), PASSWORD)).status();
            });
            start.countDown();
            assertThat(java.util.List.of(a.get(), b.get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(accountsWithKey(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-3 an empty identifier is refused with a field-level message and no account is created")
    void REQ_AUTH_001_AC3_empty_identifier_is_refused() {
        long before = db().count("SELECT count(*) FROM users");

        Reply r = signUpInvalid("", PASSWORD);

        assertRefusedOn(r, "identifier");
        assertThat(db().count("SELECT count(*) FROM users")).isEqualTo(before);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-3 an empty password is refused with a field-level message and no account is created")
    void REQ_AUTH_001_AC3_empty_password_is_refused() {
        String id = "eve-" + uid();

        Reply r = signUpInvalid(id, "");

        assertRefusedOn(r, "password");
        assertThat(accountsWithKey(id)).isZero();
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-3 an empty identifier and an empty password are both reported together")
    void REQ_AUTH_001_AC3_both_fields_reported_together() {
        Reply r = signUpInvalid("", "");

        r.assertProblem(400, "VALIDATION_FAILED");
        assertThat(r.errorFields()).contains("identifier", "password");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-3 an absent identifier or password is refused naming the field")
    void REQ_AUTH_001_AC3_absent_fields_are_refused() {
        assertRefusedOn(anonymous().postInvalid(SIGNUP, Json.object("password", PASSWORD)), "identifier");
        assertRefusedOn(anonymous().postInvalid(SIGNUP, Json.object("identifier", "x-" + uid())), "password");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-3 an identifier of only whitespace is refused")
    void REQ_AUTH_001_AC3_whitespace_only_identifier_is_refused() {
        assertRefusedOn(signUpInvalid("    　  ", PASSWORD), "identifier");
    }

    // ---- password length, in code points (AC-5, AC-11) ----

    @ParameterizedTest(name = "{0} chars refused")
    @ValueSource(ints = {0, 1, 7})
    @DisplayName("REQ-AUTH-001 AC-5 a password of fewer than 8 characters is refused")
    void REQ_AUTH_001_AC5_short_password_is_refused(int length) {
        String id = "pw-" + uid();

        assertRefusedOn(signUpInvalid(id, "p".repeat(length)), "password");
        assertThat(accountsWithKey(id)).isZero();
    }

    @ParameterizedTest(name = "{0} chars accepted")
    @ValueSource(ints = {8, 9, 127, 128})
    @DisplayName("REQ-AUTH-001 AC-11 a password of 8 up to and including 128 characters is accepted")
    void REQ_AUTH_001_AC11_password_within_bounds_is_accepted(int length) {
        Reply r = anonymous().post(SIGNUP, credentials("pw-" + uid(), "p".repeat(length)));

        assertThat(r.status()).isEqualTo(201);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-11 a password of 129 characters is refused")
    void REQ_AUTH_001_AC11_password_over_128_is_refused() {
        assertRefusedOn(signUpInvalid("pw-" + uid(), "p".repeat(129)), "password");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-11 password length counts code points: 128 emoji accepted, 129 refused, 7 refused")
    void REQ_AUTH_001_AC11_password_length_counts_code_points() {
        assertThat(anonymous().post(SIGNUP, credentials("pw-" + uid(), GRIN.repeat(128))).status()).isEqualTo(201);
        assertThat(anonymous().post(SIGNUP, credentials("pw-" + uid(), GRIN.repeat(8))).status()).isEqualTo(201);
        assertRefusedOn(signUpInvalid("pw-" + uid(), GRIN.repeat(129)), "password");
        assertRefusedOn(signUpInvalid("pw-" + uid(), GRIN.repeat(7)), "password");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-11 the password is not trimmed: 7 characters and a space count as 8")
    void REQ_AUTH_001_AC11_password_is_not_trimmed() {
        assertThat(anonymous().post(SIGNUP, credentials("pw-" + uid(), "abcdefg ")).status()).isEqualTo(201);
        assertThat(anonymous().post(SIGNUP, credentials("pw-" + uid(), "        ")).status()).isEqualTo(201);
    }

    // ---- identifier length and trimming (AC-8) ----

    @ParameterizedTest(name = "{0} chars")
    @ValueSource(ints = {1, 2})
    @DisplayName("REQ-AUTH-001 AC-8 an identifier shorter than 3 characters after trimming is refused")
    void REQ_AUTH_001_AC8_short_identifier_is_refused(int length) {
        assertRefusedOn(signUpInvalid("a".repeat(length), PASSWORD), "identifier");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-8 an identifier of exactly 3 characters is accepted")
    void REQ_AUTH_001_AC8_three_character_identifier_is_accepted() {
        // the database file is new for each test JVM and no other test signs up "x-y"
        Reply r = anonymous().post(SIGNUP, credentials("x-y", PASSWORD));

        assertThat(r.status()).as(r.body()).isEqualTo(201);
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-8 an identifier of 254 characters is accepted and 255 is refused")
    void REQ_AUTH_001_AC8_identifier_upper_bound_is_254() {
        String u = uid();
        String at254 = u + "a".repeat(254 - u.length());
        String at255 = at254 + "b";

        assertThat(anonymous().post(SIGNUP, credentials(at254, PASSWORD)).status()).isEqualTo(201);
        assertRefusedOn(signUpInvalid(at255, PASSWORD), "identifier");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-8 leading and trailing whitespace (including U+00A0 and U+3000) is removed before saving")
    void REQ_AUTH_001_AC8_identifier_is_trimmed_with_unicode_whitespace() {
        String core = "trim-" + uid();

        Reply r = anonymous().post(SIGNUP, credentials(" 　 " + core + " 　 ", PASSWORD));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.<String>json("$.user.identifier")).isEqualTo(core);
        anonymous().post(SIGNUP, credentials(core.toUpperCase(Locale.ROOT), PASSWORD)).assertProblem(409, "IDENTIFIER_TAKEN");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-8 the 3 character minimum applies to the trimmed identifier")
    void REQ_AUTH_001_AC8_minimum_applies_after_trimming() {
        assertRefusedOn(signUpInvalid(" ab ", PASSWORD), "identifier");
    }

    // ---- email rule (AC-9) ----

    static Stream<Arguments> invalidEmails() {
        return Stream.of(
                "@example.com", "a@", "a@@example.com", "a@b@example.com", "a b@example.com", "a@exa mple.com",
                "a@example", "a@example.c", "a@example.c0m", ".a@example.com", "a.@example.com", "a..b@example.com",
                "a@-example.com", "a@example-.com", "a@exa_mple.com", "a@.example.com", "a@example..com",
                "é@example.com", "a@éxample.com", "a@xn--p1ai.xn--p1ai", "\"a b\"@example.com",
                "a@[127.0.0.1]", "(c)a@example.com", "a@example.com.", "a(c)@example.com", "a<b@example.com",
                "x".repeat(65) + "@example.com", "a@" + "l".repeat(64) + ".com")
                .map(Arguments::of);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidEmails")
    @DisplayName("REQ-AUTH-001 AC-9 an identifier with @ that is not a valid email address is refused")
    void REQ_AUTH_001_AC9_invalid_email_is_refused(String identifier) {
        assertRefusedOn(signUpInvalid(identifier, PASSWORD), "identifier");
        assertThat(accountsWithKey(identifier)).isZero();
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-9 valid email addresses are accepted: special local-part characters, 64-character local part, hyphenated labels")
    void REQ_AUTH_001_AC9_valid_emails_are_accepted() {
        String u = uid();
        assertThat(anonymous().post(SIGNUP, credentials(u + "!#$%&'*+/=?^_`{|}~-.x@example.com", PASSWORD)).status())
                .isEqualTo(201);
        assertThat(anonymous().post(SIGNUP, credentials(u + "x".repeat(64 - u.length()) + "@example.com", PASSWORD)).status())
                .isEqualTo(201);
        assertThat(anonymous().post(SIGNUP, credentials("a" + u + "@sub-domain.example.co", PASSWORD)).status())
                .isEqualTo(201);
        assertThat(anonymous().post(SIGNUP, credentials("b" + u + "@" + "l".repeat(63) + ".com", PASSWORD)).status())
                .isEqualTo(201);
    }

    // ---- username rule (AC-10) ----

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"bad name", "bad!name", "naïve-name", "a+b-name", "semi;colon", "slash/name", "quote'name", "tab\tname"})
    @DisplayName("REQ-AUTH-001 AC-10 an identifier without @ with a character outside A-Z a-z 0-9 . _ - is refused")
    void REQ_AUTH_001_AC10_invalid_username_is_refused(String identifier) {
        assertRefusedOn(signUpInvalid(identifier, PASSWORD), "identifier");
    }

    @Test
    @DisplayName("REQ-AUTH-001 AC-10 a username of letters, digits, dot, underscore and hyphen is accepted")
    void REQ_AUTH_001_AC10_valid_username_is_accepted() {
        assertThat(anonymous().post(SIGNUP, credentials("A.b_c-9." + uid(), PASSWORD)).status()).isEqualTo(201);
    }
}
