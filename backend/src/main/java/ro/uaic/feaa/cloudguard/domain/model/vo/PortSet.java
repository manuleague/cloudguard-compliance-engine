package ro.uaic.feaa.cloudguard.domain.model.vo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Tip de date propriu (VO custom data type): mulțimea porturilor deschise
 * ale unei resurse. Imutabil; fiecare port trebuie să fie între 1 și 65535.
 * Se salvează într-o singură coloană text, ex. "22,443" (vezi PortSetConverter).
 */
public final class PortSet {

    private static final PortSet EMPTY = new PortSet(Collections.emptySet());

    private final SortedSet<Integer> ports;

    private PortSet(Set<Integer> ports) {
        for (Integer port : ports) {
            if (port == null || port < 1 || port > 65535) {
                throw new IllegalArgumentException("Portul " + port + " nu este valid (1–65535)");
            }
        }
        this.ports = Collections.unmodifiableSortedSet(new TreeSet<>(ports));
    }

    public static PortSet empty() {
        return EMPTY;
    }

    public static PortSet of(Integer... ports) {
        return new PortSet(Set.of(ports));
    }

    /** Citește formatul "22,443"; textul gol înseamnă nicio port deschis. */
    public static PortSet parse(String text) {
        if (text == null || text.isBlank()) {
            return EMPTY;
        }
        Set<Integer> parsed = Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
        return new PortSet(parsed);
    }

    public Set<Integer> values() {
        return ports;
    }

    public boolean isEmpty() {
        return ports.isEmpty();
    }

    /** Adevărat dacă toate porturile deschise se află în lista celor permise. */
    public boolean isSubsetOf(PortSet allowed) {
        return allowed.ports.containsAll(ports);
    }

    @Override
    public String toString() {
        return ports.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PortSet other && ports.equals(other.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ports);
    }
}
