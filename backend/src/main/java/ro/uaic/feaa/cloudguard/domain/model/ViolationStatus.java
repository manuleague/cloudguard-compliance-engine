package ro.uaic.feaa.cloudguard.domain.model;

/**
 * Statusul unei încălcări (regula A2):
 * OPEN -> UNDER_REVIEW -> RESOLVED sau RISK_ACCEPTED.
 */
public enum ViolationStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED,
    RISK_ACCEPTED;

    /** O încălcare activă blochează crearea unui duplicat (regula A1). */
    public boolean isActive() {
        return this == OPEN || this == UNDER_REVIEW;
    }

    public boolean canMoveTo(ViolationStatus target) {
        return switch (this) {
            case OPEN -> target == UNDER_REVIEW;
            case UNDER_REVIEW -> target == RESOLVED || target == RISK_ACCEPTED;
            case RESOLVED, RISK_ACCEPTED -> false;
        };
    }
}
