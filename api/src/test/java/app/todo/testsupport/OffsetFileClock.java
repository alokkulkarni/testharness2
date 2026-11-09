package app.todo.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * The end-to-end test clock (ADR-0013): the system UTC time plus an offset read on each call from a file. The file
 * holds an ISO-8601 duration such as {@code PT8H1M}; an absent or empty file means no offset; unreadable or invalid
 * content throws, so the test fails visibly. Lives in the test source set only, so it is absent from
 * {@code spring-boot:run} and from the packaged application.
 */
public final class OffsetFileClock extends Clock {

    private final Path offsetFile;

    public OffsetFileClock(Path offsetFile) {
        this.offsetFile = offsetFile;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return Clock.systemUTC().instant().plus(offset());
    }

    private Duration offset() {
        if (!Files.exists(offsetFile)) {
            return Duration.ZERO;
        }
        try {
            String content = Files.readString(offsetFile).trim();
            return content.isEmpty() ? Duration.ZERO : Duration.parse(content);
        } catch (IOException | DateTimeParseException e) {
            throw new IllegalStateException("The test clock offset file " + offsetFile + " cannot be used", e);
        }
    }
}
