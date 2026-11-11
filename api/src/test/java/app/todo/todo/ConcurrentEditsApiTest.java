package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** REQ-DATA-002 Concurrent edits: last write wins (ADR-0006, ADR-0016). */
class ConcurrentEditsApiTest extends IntegrationTestBase {

    @Test
    @DisplayName("REQ-DATA-002 AC-1 two edits one after the other: the later write is stored and no conflict error is shown")
    void REQ_DATA_002_AC1_the_later_write_wins_without_a_conflict() {
        var me = signedIn();
        String id = createTodoId(me, "start");

        Reply firstTab = me.put(TODOS + "/" + id, todoBody("from tab one", "2026-12-01", "LOW", "one"));
        Reply secondTab = me.put(TODOS + "/" + id, todoBody("from tab two", "2027-02-02", "HIGH", null));

        assertThat(firstTab.status()).isEqualTo(200);
        assertThat(secondTab.status()).isEqualTo(200);
        Reply stored = me.get(TODOS + "/" + id);
        assertThat(stored.<String>json("$.title")).isEqualTo("from tab two");
        assertThat(stored.<String>json("$.dueDate")).isEqualTo("2027-02-02");
        assertThat(stored.<String>json("$.priority")).isEqualTo("HIGH");
        assertThat(stored.<Object>json("$.description")).isNull();
    }

    @Test
    @DisplayName("REQ-DATA-002 AC-1 a stale edit made after a newer one still wins: there is no version check")
    void REQ_DATA_002_AC1_no_version_check_a_stale_copy_overwrites() {
        var me = signedIn();
        String id = createTodoId(me, "start");
        Reply loadedByTabTwo = me.get(TODOS + "/" + id); // tab two loads the to-do and keeps it open

        me.put(TODOS + "/" + id, todoBody("tab one edit", "2026-12-01", "LOW", null));
        Reply stale = me.put(TODOS + "/" + id, todoBody("tab two edit", "2026-12-01", "LOW", null));

        assertThat(loadedByTabTwo.status()).isEqualTo(200);
        assertThat(stale.status()).isEqualTo(200);
        assertThat(me.get(TODOS + "/" + id).<String>json("$.title")).isEqualTo("tab two edit");
    }

    @Test
    @DisplayName("REQ-DATA-002 AC-1 a done change and a content edit from two tabs do not undo each other")
    void REQ_DATA_002_AC1_done_and_content_writes_do_not_undo_each_other() {
        var me = signedIn();
        String id = createTodoId(me, "start");

        setDone(me, id, true);
        me.put(TODOS + "/" + id, todoBody("renamed in tab two", "2026-12-01", "LOW", null));
        Reply afterContent = me.get(TODOS + "/" + id);
        assertThat(afterContent.<Boolean>json("$.done")).as("content write keeps done").isTrue();
        assertThat(afterContent.<String>json("$.title")).isEqualTo("renamed in tab two");

        setDone(me, id, false);
        Reply afterDone = me.get(TODOS + "/" + id);
        assertThat(afterDone.<Boolean>json("$.done")).isFalse();
        assertThat(afterDone.<String>json("$.title")).as("done write keeps content").isEqualTo("renamed in tab two");
    }

    @Test
    @DisplayName("REQ-DATA-002 AC-2 concurrent edits to different to-dos are each stored with no data lost")
    void REQ_DATA_002_AC2_concurrent_edits_to_different_todos_are_all_stored() throws Exception {
        var me = signedIn();
        int n = 12;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add(createTodoId(me, "start " + i));
        }
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < n; i++) {
                final int index = i;
                results.add(pool.submit(() -> {
                    go.await();
                    ApiClient client = me;
                    String path = TODOS + "/" + ids.get(index);
                    int status = client.put(path, todoBody("edited " + index, "2026-12-01", "HIGH", "d" + index)).status();
                    int doneStatus = index % 2 == 0
                            ? client.put(path + "/done", Json.object("done", true)).status()
                            : 200;
                    return status == 200 && doneStatus == 200 ? 200 : Math.max(status, doneStatus);
                }));
            }
            go.countDown();
            for (Future<Integer> f : results) {
                assertThat(f.get()).isEqualTo(200);
            }
        } finally {
            pool.shutdownNow();
        }

        for (int i = 0; i < n; i++) {
            Reply stored = me.get(TODOS + "/" + ids.get(i));
            assertThat(stored.<String>json("$.title")).isEqualTo("edited " + i);
            assertThat(stored.<String>json("$.description")).isEqualTo("d" + i);
            assertThat(stored.<Boolean>json("$.done")).isEqualTo(i % 2 == 0);
        }
    }

    @Test
    @DisplayName("REQ-DATA-002 AC-2 concurrent creates by one signed-in user are all stored")
    void REQ_DATA_002_AC2_concurrent_creates_are_all_stored() throws Exception {
        var me = signedIn();
        int n = 15;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < n; i++) {
                final int index = i;
                results.add(pool.submit(() -> {
                    go.await();
                    return me.post(TODOS, todoBody("parallel " + index)).status();
                }));
            }
            go.countDown();
            for (Future<Integer> f : results) {
                assertThat(f.get()).isEqualTo(201);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(me.get(TODOS).<Integer>json("$.totalItems")).isEqualTo(n);
    }
}
