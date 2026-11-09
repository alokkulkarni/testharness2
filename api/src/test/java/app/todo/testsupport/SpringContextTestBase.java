package app.todo.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spring Boot + MockMvc against a fresh temporary SQLite file per test class (sdd.md section 14, integration level).
 * The context is discarded after the class, so the next class starts on a new, empty database file. The application
 * class is found by Spring Boot from the test class's package (no class is named here). The embedded container is
 * used only where sdd.md section 14 says so (the NFR-004 filter-exception test, which re-declares
 * {@code @SpringBootTest(webEnvironment = RANDOM_PORT)}).
 *
 * Assumption for the java-engineer: the database path is read from the property {@code TODO_DB_PATH}
 * (environment variable name, relaxed binding {@code todo.db.path}); both spellings are set here.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class SpringContextTestBase {

    private static final AtomicLong COUNTER = new AtomicLong();
    private static volatile Path dbFile;

    @BeforeAll
    static void freshDatabaseFile() throws IOException {
        Path dir = Files.createTempDirectory("todo-it-");
        dbFile = dir.resolve("todo.db");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(dir)));
    }

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        // evaluated when the class's context is created, after freshDatabaseFile() of the same class
        registry.add("TODO_DB_PATH", () -> dbFile.toString());
        registry.add("todo.db.path", () -> dbFile.toString());
        registry.add("TODO_COOKIE_SECURE", () -> "false");
        registry.add("todo.cookie.secure", () -> "false");
    }

    @Autowired
    protected Environment environment;

    @Autowired
    protected MockMvc mockMvc;

    protected ApiClient anonymous() {
        return ApiClient.forMockMvc(mockMvc);
    }

    protected Db db() {
        return new Db(dbFile);
    }

    protected static Path dbFile() {
        return dbFile;
    }

    /** A short token that is unique within the JVM, safe in a username (letters, digits). */
    protected static String uid() {
        return Long.toString(System.nanoTime() % 1_000_000_000L, 36) + COUNTER.incrementAndGet();
    }

    private static void deleteQuietly(Path dir) {
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException | RuntimeException ignored) {
            // best effort clean-up of a temporary directory
        }
    }
}
