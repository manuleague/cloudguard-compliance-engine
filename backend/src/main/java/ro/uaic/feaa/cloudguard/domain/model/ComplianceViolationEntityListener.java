package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * Reguli aplicate automat de JPA pentru încălcări:
 * calcul (C2 de rezervă) și audit (invariantul A2, fără ștergeri).
 */
public class ComplianceViolationEntityListener {

    @PrePersist
    public void beforeInsert(ComplianceViolation violation) {
        violation.fillSlaIfMissing();
        violation.checkPersistenceInvariants();
    }

    @PreUpdate
    public void beforeUpdate(ComplianceViolation violation) {
        violation.checkPersistenceInvariants();
    }

    /** Încălcările fac parte din istoricul de conformitate, deci nu se șterg. */
    @PreRemove
    public void beforeDelete(ComplianceViolation violation) {
        throw new BusinessRuleException("Încălcările de conformitate nu se pot șterge, pentru trasabilitate");
    }
}
