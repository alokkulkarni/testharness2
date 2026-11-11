package app.todo.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one {@link Clock} of the application: the system UTC clock (ADR-0013). Every class that reads time takes it by
 * constructor injection. The only way to move it is the test-source-set clock of the end-to-end run, which is not part
 * of the application.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
