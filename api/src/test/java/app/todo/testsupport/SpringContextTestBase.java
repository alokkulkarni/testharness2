package app.todo.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The application started on its embedded container at a random port, on a temporary SQLite file shared by the
 * JVM's test contexts (so every test creates its own uniquely named users and never assumes an empty database).
 * The application class is found by Spring Boot from the test class's package (no class is named here).
 *
 * Assumption for the java-engineer: the database path is read from the property {@code TODO_DB_PATH}
 * (environment variable name, relaxed binding {@code todo.db.path}); both spellings are set here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class SpringContextTestBase {

    private static final Path DB_FILE;
    private static final AtomicLong COUNTER = new AtomicLong();

    static {
        try {
            Path dir = Files.createTempDirectory("todo-it-");
            DB_FILE = dir.resolve("todo.db");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(dir)));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("TODO_DB_PATH", DB_FILE::toString);
        registry.add("todo.db.path", DB_FILE::toString);
        registry.add("TODO_COOKIE_SECURE", () -> "false");
        registry.add("todo.cookie.secure", () -> "false");
    }

    @Autowired
    protected Environment environment;

    protected int port() {
        return Integer.parseInt(environment.getRequiredProperty("local.server.port"));
    }

    protected ApiClient anonymous() {
        return ApiClient.forPort(port());
    }

    protected Db db() {
        return new Db(DB_FILE);
    }

    protected static Path dbFile() {
        return DB_FILE;
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
