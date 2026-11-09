package app.todo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.Json;
import app.todo.testsupport.LogCapture;
import app.todo.testsupport.Reply;
import app.todo.testsupport.SpringContextTestBase;
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
 * NFR-004 AC-2 / REQ-AUTH-003 AC-3 (sdd.md section 14, section 10): a forced internal error inside a filter.
 * Runs on the embedded container because only a real container performs the ERROR dispatch that
 * ProblemErrorController answers. The session lookup is replaced by a double that throws a fixed message.
 * Lives in package app.todo.auth because SessionRepository is package-private there.
 */
@Import(FilterExceptionTest.ThrowingSessionLookup.class)
class FilterExceptionTest extends SpringContextTestBase {

    static final String FIXED_MESSAGE = "boom-fixed-message-7731 select * from users";

    @TestConfiguration(proxyBeanMethods = false)
    static class ThrowingSessionLookup {
        @Bean
        @Primary
        SessionRepository throwingSessionRepository() {
            return Mockito.mock(SessionRepository.class, invocation -> {
                switch (invocation.getMethod().getName()) {
                    case "toString":
                        return "throwing-session-repository";
                    case "hashCode":
                        return System.identityHashCode(invocation.getMock());
                    case "equals":
                        return invocation.getMock() == invocation.getArgument(0);
                    default:
                        throw new IllegalStateException(FIXED_MESSAGE);
                }
            });
        }
    }

    private static boolean carriesFixedMessage(IThrowableProxy proxy) {
        for (IThrowableProxy p = proxy; p != null; p = p.getCause()) {
            if (p.getMessage() != null && p.getMessage().contains(FIXED_MESSAGE)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("NFR-004 AC-2 an exception inside a filter becomes the generic 500 INTERNAL_ERROR Problem with no exception text or SQL")
    void NFR_004_AC2_filter_exception_gives_generic_500_problem() {
        String sentinelId = "sentinel-identifier-" + uid();
        String sentinelPassword = "sentinel-password-" + uid();
        String sentinelToken = "sentinel-token-" + uid();
        ApiClient client = anonymous().withSession(sentinelToken);

        Reply r = client.post("/api/v1/auth/login", Json.object("identifier", sentinelId, "password", sentinelPassword));

        r.assertProblem(500, "INTERNAL_ERROR");
        assertThat(r.body()).doesNotContain(FIXED_MESSAGE).doesNotContain("select").doesNotContain("IllegalState")
                .doesNotContain("at app.todo").doesNotContain("Exception").doesNotContain("whitelabel")
                .doesNotContain(sentinelId).doesNotContain(sentinelPassword).doesNotContain(sentinelToken);
    }

    @Test
    @DisplayName("NFR-004 AC-2 the thrown exception's stack trace is logged exactly once and nothing sensitive is logged")
    void NFR_004_AC2_stack_trace_logged_once_and_no_sensitive_value_logged() {
        String sentinelId = "sentinel-identifier-" + uid();
        String sentinelPassword = "sentinel-password-" + uid();
        String sentinelToken = "sentinel-token-" + uid();
        try (LogCapture logs = new LogCapture()) {
            anonymous().withSession(sentinelToken)
                    .post("/api/v1/auth/login", Json.object("identifier", sentinelId, "password", sentinelPassword));

            List<ILoggingEvent> events = logs.events();
            long withStackTrace = events.stream().filter(e -> carriesFixedMessage(e.getThrowableProxy())).count();
            assertThat(withStackTrace).as("stack trace of the thrown exception is logged once").isEqualTo(1);
            for (ILoggingEvent event : events) {
                String text = LogCapture.text(event);
                assertThat(text).doesNotContain(sentinelId).doesNotContain(sentinelPassword).doesNotContain(sentinelToken);
            }
        }
    }
}
