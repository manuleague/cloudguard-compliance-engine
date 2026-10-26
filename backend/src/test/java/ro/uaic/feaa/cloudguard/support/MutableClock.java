package ro.uaic.feaa.cloudguard.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Ceas de test pe care îl putem muta înainte, ca să verificăm calculele de timp. */
public class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(LocalDateTime start, ZoneId zone) {
        this.instant = start.atZone(zone).toInstant();
        this.zone = zone;
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(LocalDateTime.ofInstant(instant, newZone), newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
