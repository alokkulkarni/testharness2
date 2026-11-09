package app.todo.testsupport;

import java.nio.file.Path;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Registers the {@link OffsetFileClock} as the {@code @Primary} {@code Clock} (ADR-0013). Only for the session-expiry
 * end-to-end run: profile {@code e2e}, offset file named by {@code TODO_E2E_CLOCK_OFFSET_FILE}
 * ({@code todo.e2e.clock-offset-file}), started by {@code app.todo.TestTodoApplication}. It is in the test source set,
 * so it is not part of the application, and it has no HTTP endpoint.
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("e2e")
public class E2eClockConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(E2eClockConfiguration.class);

    @Bean
    @Primary
    Clock e2eClock(@Value("${todo.e2e.clock-offset-file}") String offsetFile) {
        LOG.warn("The end-to-end TEST CLOCK is active: time is moved by the offset file {}. This must never run for real use.",
                offsetFile);
        return new OffsetFileClock(Path.of(offsetFile));
    }
}
