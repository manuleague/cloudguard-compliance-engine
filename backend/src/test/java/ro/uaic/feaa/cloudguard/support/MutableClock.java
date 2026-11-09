package ro.uaic.feaa.cloudguard.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Ceas de test pe care îl putem muta înainte, ca să verificăm calculele de timp. */
public class MutableClock extends Clock {

    private final Instant start;
    private Instant instant;
    private final ZoneId zone;

    public MutableClock(LocalDateTime start, ZoneId zone) {
        this.start = start.atZone(zone).toInstant();
        this.instant = this.start;
        this.zone = zone;
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    /** Readuce ceasul la momentul de start. Ceasul este un bean comun tuturor testelor din același context. */
    public void reset() {
        instant = start;
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
