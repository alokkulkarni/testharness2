package app.todo.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.Db;
import app.todo.testsupport.Json;
import app.todo.testsupport.Reply;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * REQ-DATA-001 Persistence, REQ-ACC-001 AC-3. Starts the real application twice on one database file, in two
 * separate application contexts, to show that a restart keeps accounts and to-dos (sdd.md section 14).
 */
class PersistenceTest {

    private Path dir;
    private Path dbFile;

    @BeforeEach
    void freshDirectory() throws IOException {
        dir = Files.createTempDirectory("todo-persistence-");
        // neither the file nor its parent directory exists: the application must create both (AC-3)
        dbFile = dir.resolve("not-yet").resolve("nested").resolve("todo.db");
    }

    @AfterEach
    void cleanUp() throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private ConfigurableApplicationContext start() throws Exception {
        // The application class is the one class under app.todo that carries @SpringBootApplication.
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpringBootApplication.class));
        var found = scanner.findCandidateComponents("app.todo");
        assertThat(found).as("exactly one @SpringBootApplication class under app.todo").hasSize(1);
        Class<?> application = Class.forName(found.iterator().next().getBeanClassName());
        SpringApplication app = new SpringApplication(application);
        app.setDefaultProperties(Map.of(
                "server.port", "0",
                "spring.main.banner-mode", "off",
                "TODO_DB_PATH", dbFile.toString(),
                "todo.db.path", dbFile.toString(),
                "TODO_COOKIE_SECURE", "false",
                "todo.cookie.secure", "false"));
        return app.run();
    }

    private static ApiClient clientOf(ConfigurableApplicationContext ctx) {
        return ApiClient.forPort(Integer.parseInt(ctx.getEnvironment().getRequiredProperty("local.server.port")));
    }

    @Test
    @DisplayName("REQ-DATA-001 AC-3 with no database file and no directory the application creates both and works without manual setup")
    void REQ_DATA_001_AC3_first_start_creates_directory_and_file() throws Exception {
        assertThat(dbFile.getParent()).doesNotExist();

        try (ConfigurableApplicationContext ctx = start()) {
            assertThat(dbFile).exists().isRegularFile();
            ApiClient api = clientOf(ctx);
            assertThat(api.post("/api/v1/auth/signup", Json.object("identifier", "first-start", "password", "correct-horse-battery")).status())
                    .isEqualTo(201);
        }
    }

    @Test
    @DisplayName("REQ-DATA-001 AC-2 the data is held in a SQLite file database")
    void REQ_DATA_001_AC2_storage_is_a_sqlite_file() throws Exception {
        try (ConfigurableApplicationContext ctx = start()) {
            clientOf(ctx).post("/api/v1/auth/signup", Json.object("identifier", "sqlite-user", "password", "correct-horse-battery"));
        }

        byte[] header = new byte[16];
        try (InputStream in = Files.newInputStream(dbFile)) {
            assertThat(in.readNBytes(header, 0, 16)).isEqualTo(16);
        }
        assertThat(new String(header, StandardCharsets.US_ASCII)).startsWith("SQLite format 3");
        assertThat(new Db(dbFile).count("SELECT count(*) FROM users")).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-DATA-001 AC-1 after a restart all accounts and to-dos are still present and unchanged")
    void REQ_DATA_001_AC1_accounts_and_todos_survive_a_restart() throws Exception {
        String password = "correct-horse-battery";
        String firstTodo;
        String secondTodo;
        Reply beforeFirst;
        Reply beforeSecond;
        try (ConfigurableApplicationContext ctx = start()) {
            ApiClient api = clientOf(ctx);
            api.post("/api/v1/auth/signup", Json.object("identifier", "Persist.User", "password", password));
            ApiClient me = api.withSession(api.post("/api/v1/auth/login", Json.object("identifier", "Persist.User", "password", password)).sessionToken());
            firstTodo = me.post("/api/v1/todos", "{\"title\":\"kept one\",\"dueDate\":\"2026-12-24\",\"priority\":\"HIGH\",\"description\":\"with words\"}").json("$.id");
            secondTodo = me.post("/api/v1/todos", "{\"title\":\"kept two\",\"dueDate\":\"2020-01-01\"}").json("$.id");
            me.put("/api/v1/todos/" + secondTodo + "/done", Json.object("done", true));
            beforeFirst = me.get("/api/v1/todos/" + firstTodo);
            beforeSecond = me.get("/api/v1/todos/" + secondTodo);
        }

        try (ConfigurableApplicationContext ctx = start()) {
            ApiClient api = clientOf(ctx);
            Reply login = api.post("/api/v1/auth/login", Json.object("identifier", "persist.user", "password", password));
            assertThat(login.status()).as("the account survived").isEqualTo(200);
            assertThat(login.<String>json("$.user.identifier")).isEqualTo("Persist.User");
            ApiClient me = api.withSession(login.sessionToken());

            assertThat(me.get("/api/v1/todos/" + firstTodo).body()).isEqualTo(beforeFirst.body());
            assertThat(me.get("/api/v1/todos/" + secondTodo).body()).isEqualTo(beforeSecond.body());
            Reply list = me.get("/api/v1/todos");
            assertThat(list.<Integer>json("$.totalItems")).isEqualTo(2);
            assertThat(list.<Integer>json("$.totals.completed")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("REQ-ACC-001 AC-3 after the application is restarted the first user logs in again and their to-dos are still there")
    void REQ_ACC_001_AC3_first_user_finds_their_todos_after_a_restart() throws Exception {
        String password = "correct-horse-battery";
        try (ConfigurableApplicationContext ctx = start()) {
            ApiClient api = clientOf(ctx);
            api.post("/api/v1/auth/signup", Json.object("identifier", "acc-user", "password", password));
            ApiClient me = api.withSession(api.post("/api/v1/auth/login", Json.object("identifier", "acc-user", "password", password)).sessionToken());
            me.post("/api/v1/todos", "{\"title\":\"survives restart\",\"dueDate\":\"2026-12-01\"}");
        }

        try (ConfigurableApplicationContext ctx = start()) {
            ApiClient api = clientOf(ctx);
            ApiClient me = api.withSession(api.post("/api/v1/auth/login", Json.object("identifier", "acc-user", "password", password)).sessionToken());

            assertThat(me.get("/api/v1/todos").<java.util.List<String>>json("$.items[*].title")).containsExactly("survives restart");
        }
    }

    @Test
    @DisplayName("REQ-DATA-001 AC-1 a session survives an API restart until it expires (sessions are stored in SQLite)")
    void REQ_DATA_001_AC1_session_survives_a_restart() throws Exception {
        String token;
        try (ConfigurableApplicationContext ctx = start()) {
            ApiClient api = clientOf(ctx);
            api.post("/api/v1/auth/signup", Json.object("identifier", "session-user", "password", "correct-horse-battery"));
            token = api.post("/api/v1/auth/login", Json.object("identifier", "session-user", "password", "correct-horse-battery")).sessionToken();
        }

        try (ConfigurableApplicationContext ctx = start()) {
            Reply session = clientOf(ctx).withSession(token).get("/api/v1/auth/session");

            assertThat(session.status()).isEqualTo(200);
            assertThat(session.<String>json("$.user.identifier")).isEqualTo("session-user");
        }
    }
}
