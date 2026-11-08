package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Reply;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** REQ-LIST-001 filter, REQ-LIST-002 search, REQ-LIST-003 pagination (todo-api-v1.yaml listTodos). */
class TodoListApiTest extends IntegrationTestBase {

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private List<String> titles(Reply list) {
        return list.json("$.items[*].title");
    }

    // ---- REQ-LIST-001 ----

    private ApiClient mixed() {
        var me = signedIn();
        createTodoId(me, "active one");
        String done1 = createTodoId(me, "done one");
        createTodoId(me, "active two");
        String done2 = createTodoId(me, "done two");
        setDone(me, done1, true);
        setDone(me, done2, true);
        return me;
    }

    @Test
    @DisplayName("REQ-LIST-001 AC-1 the All filter lists both active and completed to-dos")
    void REQ_LIST_001_AC1_all_lists_active_and_completed() {
        assertThat(titles(mixed().get(TODOS + "?status=all"))).containsExactlyInAnyOrder("active one", "done one", "active two", "done two");
    }

    @Test
    @DisplayName("REQ-LIST-001 AC-2 the Active filter lists only not-done to-dos")
    void REQ_LIST_001_AC2_active_lists_only_not_done() {
        Reply r = mixed().get(TODOS + "?status=active");

        assertThat(titles(r)).containsExactly("active two", "active one");
        assertThat(r.<Integer>json("$.totalItems")).isEqualTo(2);
    }

    @Test
    @DisplayName("REQ-LIST-001 AC-3 the Completed filter lists only done to-dos")
    void REQ_LIST_001_AC3_completed_lists_only_done() {
        Reply r = mixed().get(TODOS + "?status=completed");

        assertThat(titles(r)).containsExactly("done two", "done one");
        assertThat(r.<Integer>json("$.totalItems")).isEqualTo(2);
    }

    @Test
    @DisplayName("REQ-LIST-001 AC-4 a filter that matches nothing gives an empty result while the totals still show the to-dos exist")
    void REQ_LIST_001_AC4_filter_with_no_match_is_empty_but_totals_show_data_exists() {
        var me = signedIn();
        createTodoId(me, "only active");

        Reply r = me.get(TODOS + "?status=completed");

        assertThat(r.<List<Object>>json("$.items")).isEmpty();
        assertThat(r.<Integer>json("$.totalItems")).isZero();
        assertThat(r.<Integer>json("$.totals.all")).as("distinguishes 'no matches' from first use").isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-LIST-001 AC-5 with no filter chosen All applies")
    void REQ_LIST_001_AC5_default_filter_is_all() {
        assertThat(mixed().get(TODOS).<Integer>json("$.totalItems")).isEqualTo(4);
    }

    @ParameterizedTest(name = "[{index}] status={0}")
    @ValueSource(strings = {"bogus", "ALL", "Active", "done", "all,active"})
    @DisplayName("REQ-LIST-001 AC-1 an unknown status value is refused with a validation error naming status")
    void REQ_LIST_001_AC1_unknown_status_is_refused(String status) {
        Reply r = signedIn().getInvalid(TODOS + "?status=" + enc(status));

        r.assertProblem(400, "VALIDATION_FAILED");
        assertThat(r.errorFields()).contains("status");
    }

    @Test
    @DisplayName("REQ-LIST-001 AC-1 the totals ignore status, q and page")
    void REQ_LIST_001_AC1_totals_ignore_status_q_and_page() {
        var me = mixed();

        for (String query : List.of("", "?status=active", "?status=completed&q=one", "?q=nothing-matches", "?page=9")) {
            Reply r = me.get(TODOS + query);
            assertThat(r.<Integer>json("$.totals.all")).as(query).isEqualTo(4);
            assertThat(r.<Integer>json("$.totals.active")).as(query).isEqualTo(2);
            assertThat(r.<Integer>json("$.totals.completed")).as(query).isEqualTo(2);
        }
    }

    // ---- REQ-LIST-002 ----

    @Test
    @DisplayName("REQ-LIST-002 AC-1 search matches a title case-insensitively (contains)")
    void REQ_LIST_002_AC1_title_search_is_case_insensitive_contains() {
        var me = signedIn();
        createTodoId(me, "Buy Milk");
        createTodoId(me, "Walk dog");

        assertThat(titles(me.get(TODOS + "?q=milk"))).containsExactly("Buy Milk");
        assertThat(titles(me.get(TODOS + "?q=MILK"))).containsExactly("Buy Milk");
        assertThat(titles(me.get(TODOS + "?q=uy%20Mi"))).containsExactly("Buy Milk");
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-1 accented letters match case-insensitively")
    void REQ_LIST_002_AC1_accented_letters_match_case_insensitively() {
        var me = signedIn();
        createTodoId(me, "école du soir");

        assertThat(titles(me.get(TODOS + "?q=" + enc("ÉCOLE")))).containsExactly("école du soir");
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-2 search also matches the description")
    void REQ_LIST_002_AC2_search_matches_the_description() {
        var me = signedIn();
        me.post(TODOS, todoBody("Plain title", "2026-12-01", "LOW", "remember the Oat MILK"));
        createTodoId(me, "Unrelated");

        assertThat(titles(me.get(TODOS + "?q=oat%20milk"))).containsExactly("Plain title");
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-3 a search and a filter together list only to-dos matching both")
    void REQ_LIST_002_AC3_search_and_filter_combine_with_and() {
        var me = signedIn();
        createTodoId(me, "milk active");
        setDone(me, createTodoId(me, "milk done"), true);
        createTodoId(me, "bread active");

        assertThat(titles(me.get(TODOS + "?q=milk&status=active"))).containsExactly("milk active");
        assertThat(titles(me.get(TODOS + "?q=milk&status=completed"))).containsExactly("milk done");
        assertThat(me.get(TODOS + "?q=bread&status=completed").<Integer>json("$.totalItems")).isZero();
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-4 a search term that matches nothing gives an empty result")
    void REQ_LIST_002_AC4_search_with_no_match_is_empty() {
        var me = signedIn();
        createTodoId(me, "something");

        Reply r = me.get(TODOS + "?q=zzzz-no-such-text");

        assertThat(r.<List<Object>>json("$.items")).isEmpty();
        assertThat(r.<Integer>json("$.page")).isEqualTo(1);
        assertThat(r.<Integer>json("$.totalPages")).isZero();
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-5 an empty search applies no restriction")
    void REQ_LIST_002_AC5_empty_search_is_no_restriction() {
        var me = signedIn();
        createTodoId(me, "one");
        createTodoId(me, "two");

        assertThat(me.get(TODOS + "?q=").<Integer>json("$.totalItems")).isEqualTo(2);
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-6 %, _, backslash and quotes are matched literally and cause no error")
    void REQ_LIST_002_AC6_special_characters_are_matched_literally() {
        var me = signedIn();
        for (String title : List.of("100% sure", "100X sure", "a_b", "aXb", "back\\slash", "backXslash", "it's", "itXs", "say \"hi\"", "say Xhi")) {
            createTodoId(me, title);
        }

        assertThat(titles(me.get(TODOS + "?q=" + enc("100%")))).containsExactly("100% sure");
        assertThat(titles(me.get(TODOS + "?q=" + enc("%")))).containsExactly("100% sure");
        assertThat(titles(me.get(TODOS + "?q=" + enc("a_b")))).containsExactly("a_b");
        assertThat(titles(me.get(TODOS + "?q=" + enc("_")))).containsExactly("a_b");
        assertThat(titles(me.get(TODOS + "?q=" + enc("\\")))).containsExactly("back\\slash");
        assertThat(titles(me.get(TODOS + "?q=" + enc("it's")))).containsExactly("it's");
        assertThat(titles(me.get(TODOS + "?q=" + enc("'")))).containsExactly("it's");
        assertThat(titles(me.get(TODOS + "?q=" + enc("\"hi\"")))).containsExactly("say \"hi\"");
        assertThat(me.get(TODOS + "?q=" + enc("'; DROP TABLE todos; --")).<Integer>json("$.totalItems")).isZero();
        assertThat(me.get(TODOS).<Integer>json("$.totalItems")).isEqualTo(10);
    }

    @ParameterizedTest(name = "[{index}]")
    @ValueSource(strings = {" ", "   ", " ", "　", "  　\t"})
    @DisplayName("REQ-LIST-002 AC-7 search text of only whitespace (including U+00A0 and U+3000) behaves as an empty search")
    void REQ_LIST_002_AC7_whitespace_only_search_is_no_search(String q) {
        var me = signedIn();
        createTodoId(me, "one");
        createTodoId(me, "two");

        assertThat(me.get(TODOS + "?q=" + enc(q)).<Integer>json("$.totalItems")).isEqualTo(2);
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-7 leading and trailing whitespace around a search term is ignored")
    void REQ_LIST_002_AC7_search_term_is_trimmed() {
        var me = signedIn();
        createTodoId(me, "Buy Milk");
        createTodoId(me, "Other");

        assertThat(titles(me.get(TODOS + "?q=" + enc("  milk　 ")))).containsExactly("Buy Milk");
    }

    // ---- REQ-LIST-003 ----

    private ApiClient withTodos(int count) {
        var me = signedIn();
        for (int i = 1; i <= count; i++) {
            createTodoId(me, String.format("item %02d", i));
        }
        return me;
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-1 45 matching to-dos give pages of 20, 20 and 5, newest first")
    void REQ_LIST_003_AC1_45_items_give_pages_of_20_20_5() {
        var me = withTodos(45);

        Reply p1 = me.get(TODOS);
        Reply p2 = me.get(TODOS + "?page=2");
        Reply p3 = me.get(TODOS + "?page=3");

        assertThat(p1.<List<Object>>json("$.items")).hasSize(20);
        assertThat(p2.<List<Object>>json("$.items")).hasSize(20);
        assertThat(p3.<List<Object>>json("$.items")).hasSize(5);
        assertThat(p1.<Integer>json("$.totalItems")).isEqualTo(45);
        assertThat(p1.<Integer>json("$.totalPages")).isEqualTo(3);
        assertThat(p1.<Integer>json("$.pageSize")).isEqualTo(20);
        assertThat(p1.<Integer>json("$.page")).isEqualTo(1);
        assertThat(p2.<Integer>json("$.page")).isEqualTo(2);
        assertThat(p3.<Integer>json("$.page")).isEqualTo(3);
        assertThat(titles(p1).get(0)).isEqualTo("item 45");
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-2 the order is consistent across pages: no to-do repeated or skipped, even with identical timestamps")
    void REQ_LIST_003_AC2_no_repeats_or_gaps_across_pages() {
        var me = withTodos(45); // all created in the same millisecond on the fixed clock

        List<String> all = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            all.addAll(titles(me.get(TODOS + "?page=" + page)));
        }

        assertThat(all).hasSize(45);
        assertThat(new HashSet<>(all)).hasSize(45);
        List<String> expected = new ArrayList<>();
        for (int i = 45; i >= 1; i--) {
            expected.add(String.format("item %02d", i));
        }
        assertThat(all).isEqualTo(expected);
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-4 20 or fewer matching to-dos give a single page")
    void REQ_LIST_003_AC4_20_items_are_one_page_and_21_are_two() {
        var twenty = withTodos(20);
        Reply r20 = twenty.get(TODOS);
        assertThat(r20.<Integer>json("$.totalPages")).isEqualTo(1);
        assertThat(r20.<List<Object>>json("$.items")).hasSize(20);

        var twentyOne = withTodos(21);
        Reply r21 = twentyOne.get(TODOS);
        assertThat(r21.<Integer>json("$.totalPages")).isEqualTo(2);
        assertThat(twentyOne.get(TODOS + "?page=2").<List<Object>>json("$.items")).hasSize(1);
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-5 the page count reflects the filter and search matches")
    void REQ_LIST_003_AC5_page_count_reflects_the_matches() {
        var me = withTodos(45);
        for (int i = 0; i < 5; i++) {
            setDone(me, me.get(TODOS + "?status=active").<String>json("$.items[0].id"), true);
        }

        Reply completed = me.get(TODOS + "?status=completed");
        Reply search = me.get(TODOS + "?q=item%204");

        assertThat(completed.<Integer>json("$.totalItems")).isEqualTo(5);
        assertThat(completed.<Integer>json("$.totalPages")).isEqualTo(1);
        assertThat(search.<Integer>json("$.totalItems")).isEqualTo(6); // "item 40" .. "item 45"
    }

    @ParameterizedTest(name = "page={0}")
    @ValueSource(strings = {"4", "99", "999999999999999999999999999999", "00000000000000000000000000000000000005"})
    @DisplayName("REQ-LIST-003 AC-6 a page beyond the last, however large, shows the last valid page and reports it in page")
    void REQ_LIST_003_AC6_page_past_the_last_gives_the_last_page(String page) {
        var me = withTodos(45);

        Reply r = me.get(TODOS + "?page=" + page);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.<Integer>json("$.page")).isEqualTo(3);
        assertThat(r.<List<Object>>json("$.items")).hasSize(5);
        assertThat(titles(r)).containsExactly("item 05", "item 04", "item 03", "item 02", "item 01");
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-6 with no matches at all any page gives page 1 with no items")
    void REQ_LIST_003_AC6_no_matches_gives_page_one_empty() {
        var me = signedIn();
        createTodoId(me, "x");

        Reply r = me.get(TODOS + "?q=nomatch&page=5");

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.<Integer>json("$.page")).isEqualTo(1);
        assertThat(r.<List<Object>>json("$.items")).isEmpty();
        assertThat(r.<Integer>json("$.totalPages")).isZero();
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-6 leading zeros are a valid page number")
    void REQ_LIST_003_AC6_leading_zeros_are_accepted() {
        var me = withTodos(45);

        assertThat(me.get(TODOS + "?page=02").<Integer>json("$.page")).isEqualTo(2);
    }

    @ParameterizedTest(name = "page={0}")
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "1e2", "+1", "0x10", "00", "-0", "1 2"})
    @DisplayName("REQ-LIST-003 AC-7 a page number that is zero, negative or not a number is refused with a validation error naming page")
    void REQ_LIST_003_AC7_invalid_page_is_refused(String page) {
        Reply r = signedIn().getInvalid(TODOS + "?page=" + enc(page));

        r.assertProblem(400, "VALIDATION_FAILED");
        assertThat(r.errorFields()).contains("page");
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-8 the page size is fixed at 20: a pageSize parameter is ignored")
    void REQ_LIST_003_AC8_page_size_is_fixed_at_20() {
        var me = withTodos(25);

        for (String param : List.of("pageSize=5", "size=5", "limit=100", "per_page=1")) {
            Reply r = me.get(TODOS + "?" + param);
            assertThat(r.status()).as(param).isEqualTo(200);
            assertThat(r.<List<Object>>json("$.items")).as(param).hasSize(20);
            assertThat(r.<Integer>json("$.pageSize")).isEqualTo(20);
        }
    }

    @Test
    @DisplayName("REQ-LIST-003 AC-2 pages are stable when to-dos have distinct creation times too")
    void REQ_LIST_003_AC2_distinct_timestamps_also_page_without_gaps() {
        var me = signedIn();
        for (int i = 1; i <= 25; i++) {
            createTodoId(me, String.format("timed %02d", i));
            clock.advance(java.time.Duration.ofSeconds(1));
        }

        List<String> all = new ArrayList<>(titles(me.get(TODOS)));
        all.addAll(titles(me.get(TODOS + "?page=2")));

        List<String> sorted = new ArrayList<>(all);
        Collections.sort(sorted, Collections.reverseOrder());
        assertThat(all).hasSize(25).isEqualTo(sorted);
    }
}
