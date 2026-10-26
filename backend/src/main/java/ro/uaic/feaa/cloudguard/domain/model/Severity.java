package ro.uaic.feaa.cloudguard.domain.model;

/**
 * Severitatea unei încălcări. Fiecare nivel are termenul SLA (regula C2)
 * și penalizarea din scorul de conformitate (regula C4).
 */
public enum Severity {
    LOW(30 * 24, 5),
    MEDIUM(7 * 24, 10),
    HIGH(72, 20),
    CRITICAL(24, 40);

    private final int slaHours;
    private final int scorePenalty;

    Severity(int slaHours, int scorePenalty) {
        this.slaHours = slaHours;
        this.scorePenalty = scorePenalty;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public int getScorePenalty() {
        return scorePenalty;
    }

    /** Nivelul următor, fără a depăși CRITICAL. */
    public Severity escalate() {
        return this == CRITICAL ? CRITICAL : values()[ordinal() + 1];
    }
}
