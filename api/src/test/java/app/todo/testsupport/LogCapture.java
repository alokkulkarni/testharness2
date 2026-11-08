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
        return sb.toString();
    }

    @Override
    public void close() {
        root.detachAppender(appender);
        appender.stop();
    }
}
