package app.todo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.ApiClient;
import app.todo.testsupport.Json;
import app.todo.testsupport.LogCapture;
import app.todo.testsupport.Reply;
import app.todo.testsupport.SpringContextTestBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * NFR-004 AC-2 / REQ-AUTH-003 AC-3 (sdd.md section 14, section 10): a forced internal error inside a filter.
 * The one integration test that runs on the embedded container at a random port, because MockMvc performs no ERROR
 * dispatch, which ProblemErrorController answers (sdd.md section 14). Its exchange is validated with the core
 * validator's OpenApiInteractionValidator, against the {@code logIn} operation. The session lookup is replaced by a
 * double that throws a fixed message. Lives in package app.todo.auth because SessionRepository is package-private
 * there.
 *
 * Assumption for the java-engineer (sdd.md section 10 does not name it): the request id is held in the logging
 * context (MDC) under the key {@value #REQUEST_ID_KEY}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FilterExceptionTest.ThrowingSessionLookup.class)
class FilterExceptionTest extends SpringContextTestBase {

    static final String FIXED_MESSAGE = "boom-fixed-message-7731 select * from users";
    static final String REQUEST_ID_KEY = "requestId";

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

    @Autowired
    private Environment env;

    private ApiClient container() {
        return ApiClient.forPort(Integer.parseInt(env.getRequiredProperty("local.server.port")));
    }

    private static boolean carriesFixedMessage(IThrowableProxy proxy) {
        for (IThrowableProxy p = proxy; p != null; p = p.getCause()) {
            if (p.getMessage() != null && p.getMessage().contains(FIXED_MESSAGE)) {
                return true;
            }
        }
        return false;
    }

    private static String sha256Hex(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("NFR-004 AC-2 an exception inside a filter becomes the generic 500 INTERNAL_ERROR Problem with no exception text or SQL")
    void NFR_004_AC2_filter_exception_gives_generic_500_problem() {
        String sentinelId = "sentinel-identifier-" + uid();
        String sentinelPassword = "sentinel-password-" + uid();
        String sentinelToken = "sentinel-token-" + uid();
        ApiClient client = container().withSession(sentinelToken);

        Reply r = client.post("/api/v1/auth/login", Json.object("identifier", sentinelId, "password", sentinelPassword));

        r.assertProblem(500, "INTERNAL_ERROR");
        assertThat(r.body()).doesNotContain(FIXED_MESSAGE).doesNotContain("select").doesNotContain("IllegalState")
                .doesNotContain("at app.todo").doesNotContain("Exception").doesNotContain("whitelabel")
                .doesNotContain(sentinelId).doesNotContain(sentinelPassword).doesNotContain(sentinelToken);
    }

    @Test
    @DisplayName("NFR-004 AC-2 the log holds one per-request line with status 500 and one stack trace, both with the request id, and nothing sensitive")
    void NFR_004_AC2_log_has_one_request_line_and_one_stack_trace_with_the_request_id() throws Exception {
        String sentinelId = "sentinel-identifier-" + uid();
        String sentinelPassword = "sentinel-password-" + uid();
        String sentinelToken = "sentinel-token-" + uid();
        try (LogCapture logs = new LogCapture()) {
            container().withSession(sentinelToken)
                    .post("/api/v1/auth/login", Json.object("identifier", sentinelId, "password", sentinelPassword));

            List<ILoggingEvent> events = logs.events();
            List<ILoggingEvent> applicationEvents = events.stream().filter(LogCapture::isApplicationEvent).toList();

            // the stack trace of the thrown exception, logged once by the application's own loggers
            List<ILoggingEvent> stackTraces = applicationEvents.stream()
                    .filter(e -> carriesFixedMessage(e.getThrowableProxy())).toList();
            assertThat(stackTraces).as("stack trace of the thrown exception is logged once by app.todo").hasSize(1);
            String requestId = stackTraces.get(0).getMDCPropertyMap().get(REQUEST_ID_KEY);
            assertThat(requestId).as("the stack-trace entry carries the request id").isNotBlank();

            // exactly one per-request line for this request, with status 500 and the request id
            List<ILoggingEvent> requestLines = applicationEvents.stream()
                    .filter(e -> e.getThrowableProxy() == null)
                    .filter(e -> requestId.equals(e.getMDCPropertyMap().get(REQUEST_ID_KEY)))
                    .filter(e -> LogCapture.text(e).contains("500"))
                    .toList();
            assertThat(requestLines).as("one per-request line with status 500").hasSize(1);

            // every entry of the application's own loggers for the request carries that id
            for (ILoggingEvent event : applicationEvents) {
                assertThat(event.getMDCPropertyMap().get(REQUEST_ID_KEY))
                        .as("request id on %s: %s", event.getLoggerName(), event.getFormattedMessage())
                        .isEqualTo(requestId);
            }

            // no entry of any logger, the container's own included, holds a sentinel, the token or its hash
            String tokenHash = sha256Hex(sentinelToken);
            assertThat(events).isNotEmpty();
            for (ILoggingEvent event : events) {
                String text = LogCapture.text(event).toLowerCase(java.util.Locale.ROOT);
                assertThat(text).doesNotContain(sentinelId).doesNotContain(sentinelPassword).doesNotContain(sentinelToken)
                        .doesNotContain(tokenHash);
            }
        }
    }
}
