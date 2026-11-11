package app.todo.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A deterministic, manually advanced UTC clock (ADR-0013: tests inject a fixed or advanced Clock). */
public final class MutableClock extends Clock {

    public static final Instant START = Instant.parse("2026-11-08T10:00:00Z");

    private volatile Instant now = START;

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
        return now;
    }

    public void advance(Duration d) {
        now = now.plus(d);
    }

    public void reset() {
        now = START;
    }
}
