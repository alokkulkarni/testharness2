package app.todo;

import app.todo.testsupport.E2eClockConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * Starts the production application with the end-to-end test clock added (ADR-0013, sdd.md section 11). Started only
 * by the session-expiry Playwright configuration, with {@code ./mvnw -f api/pom.xml spring-boot:test-run},
 * {@code SPRING_PROFILES_ACTIVE=e2e}, a temporary {@code TODO_DB_PATH} and {@code TODO_E2E_CLOCK_OFFSET_FILE}. It lives
 * in the test source set, so the application and {@code spring-boot:run} never contain it.
 */
public class TestTodoApplication {

    public static void main(String[] args) {
        SpringApplication.from(TodoApplication::main).with(E2eClockConfiguration.class).run(args);
    }
}
