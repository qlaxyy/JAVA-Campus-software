package edu.seu.vcampus.client;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/** Mutable clock used by socket integration tests that cross schedule boundaries. */
public final class TestClock extends Clock {

    private volatile Instant instant;
    private final ZoneId zone;

    private TestClock(Instant instant, ZoneId zone) {
        this.instant = Objects.requireNonNull(instant, "instant must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    public static TestClock startingNow() {
        Clock system = Clock.systemDefaultZone();
        return new TestClock(system.instant(), system.getZone());
    }

    public void advanceToFirstSeedSchedule() {
        set(LocalDate.now(this).plusDays(1).atTime(8, 31));
    }

    public void set(LocalDateTime dateTime) {
        instant = dateTime.atZone(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId requestedZone) {
        return new TestClock(instant, requestedZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
