package app.todo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.IntegrationTestBase;
import app.todo.testsupport.LogCapture;
import app.todo.testsupport.Reply;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * NFR-004 AC-2 (sdd.md section 14): a forced internal error on the handler path returns the generic 500
 * INTERNAL_ERROR Problem with no stack trace or SQL, and its stack trace is logged once, with the request id
 * (sdd.md section 10: by ProblemDetailsAdvice when the exception reaches it). Distinct from the filter case
 * (FilterExceptionTest). Lives in package app.todo.todo because TodoRepository is package-private there.
 *
 * Assumption for the java-engineer: the request id is held in the logging context under the key {@code requestId}.
 */
@Import(HandlerExceptionTest.ThrowingTodoRepository.class)
class HandlerExceptionTest extends IntegrationTestBase {

    static final String SQL_LIKE_MESSAGE = "SQLITE_ERROR: no such table: todos; select * from todos where user_id = 7";
    static final String REQUEST_ID_KEY = "requestId";

    @TestConfiguration(proxyBeanMethods = false)
    static class ThrowingTodoRepository {
        @Bean
        @Primary
        TodoRepository throwingTodoRepository() {
            return Mockito.mock(TodoRepository.class, invocation -> {
                switch (invocation.getMethod().getName()) {
                    case "toString":
                        return "throwing-todo-repository";
                    case "hashCode":
                        return System.identityHashCode(invocation.getMock());
                    case "equals":
                        return invocation.getMock() == invocation.getArgument(0);
                    default:
                        throw new IllegalStateException(SQL_LIKE_MESSAGE);
                }
            });
        }
    }

    private static boolean carriesMessage(IThrowableProxy proxy) {
        for (IThrowableProxy p = proxy; p != null; p = p.getCause()) {
            if (p.getMessage() != null && p.getMessage().contains(SQL_LIKE_MESSAGE)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("NFR-004 AC-2 an unexpected exception in a handler gives the generic 500 INTERNAL_ERROR Problem with no stack trace, exception text or SQL")
    void NFR_004_AC2_handler_exception_gives_generic_500_problem() {
        ApiClient me = signedIn();

        Reply r = me.get(TODOS);

        r.assertProblem(500, "INTERNAL_ERROR");
        assertThat(r.body()).doesNotContain(SQL_LIKE_MESSAGE).doesNotContain("SQLITE").doesNotContain("select")
                .doesNotContain("todos where").doesNotContain("IllegalState").doesNotContain("Exception")
                .doesNotContain("at app.todo").doesNotContain("org.springframework").doesNotContain("whitelabel");
    }

    @Test
    @DisplayName("NFR-004 AC-2 the handler exception's stack trace is logged once with the request id and is not in the response")
    void NFR_004_AC2_handler_exception_stack_trace_logged_once_with_request_id() {
        ApiClient me = signedIn();
        try (LogCapture logs = new LogCapture()) {
            Reply r = me.get(TODOS);

            assertThat(r.status()).isEqualTo(500);
            List<ILoggingEvent> traces = logs.events().stream()
                    .filter(LogCapture::isApplicationEvent)
                    .filter(e -> carriesMessage(e.getThrowableProxy()))
                    .toList();
            assertThat(traces).as("stack trace logged once by the application's loggers").hasSize(1);
            String requestId = traces.get(0).getMDCPropertyMap().get(REQUEST_ID_KEY);
            assertThat(requestId).as("request id on the stack-trace entry").isNotBlank();
            assertThat(r.body()).doesNotContain(requestId);
        }
    }
}
