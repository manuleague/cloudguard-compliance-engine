package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/** Reguli aplicate automat de JPA pentru resurse: normalizare la salvare și protecția istoricului la ștergere. */
public class CloudResourceEntityListener {

    @PrePersist
    public void beforeInsert(CloudResource resource) {
        resource.normalizeIdentity();
    }

    @PreUpdate
    public void beforeUpdate(CloudResource resource) {
        resource.normalizeIdentity();
    }

    /** O resursă cu încălcări înregistrate nu se șterge, ca să nu se piardă istoricul (FR-02). */
    @PreRemove
    public void beforeDelete(CloudResource resource) {
        if (resource.hasViolations()) {
            throw new BusinessRuleException("Resursa are încălcări înregistrate și nu poate fi ștearsă");
        }
    }
}
