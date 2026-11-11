package app.todo.testsupport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Captures every log event of the JVM (root logger) while open. */
public final class LogCapture implements AutoCloseable {

    private final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public LogCapture() {
        appender.start();
        root.addAppender(appender);
    }

    public List<ILoggingEvent> events() {
        return new ArrayList<>(appender.list);
    }

    /** Message, arguments and stack trace of one event, as one searchable string. */
    public static String text(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(String.valueOf(event.getFormattedMessage()));
        if (event.getThrowableProxy() != null) {
            sb.append('\n').append(ThrowableProxyUtil.asString(event.getThrowableProxy()));
        }
        sb.append('\n').append(event.getMDCPropertyMap());
        if (event.getKeyValuePairs() != null) {
            event.getKeyValuePairs().forEach(kv -> sb.append('\n').append(kv.key).append('=').append(kv.value));
        }
        if (event.getArgumentArray() != null) {
            for (Object argument : event.getArgumentArray()) {
                sb.append('\n').append(argument);
            }
        }
        return sb.toString();
    }

    /** True for an event written by one of the application's own loggers (package {@code app.todo}). */
    public static boolean isApplicationEvent(ILoggingEvent event) {
        return event.getLoggerName() != null && event.getLoggerName().startsWith("app.todo");
    }

    @Override
    public void close() {
        root.detachAppender(appender);
        appender.stop();
    }
}
