package app.todo.common;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Fixed-width UTC text timestamps {@code yyyy-MM-ddTHH:mm:ss.SSSZ} (ADR-0017). Every stored and returned instant goes
 * through this one formatter, so text order equals time order.
 */
public final class UtcTimestamps {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private UtcTimestamps() {}

    public static String format(Instant instant) {
        return FORMAT.format(instant.truncatedTo(ChronoUnit.MILLIS));
    }

    public static Instant parse(String text) {
        return Instant.parse(text);
    }
}
