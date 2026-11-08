package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/** Regulă de audit (NFR-02): jurnalul nu se modifică și nu se șterge, nici măcar direct prin JPA. */
public class AuditLogEntityListener {

    @PreUpdate
    public void beforeUpdate(AuditLog entry) {
        throw new BusinessRuleException("Jurnalul de audit este imutabil");
    }

    @PreRemove
    public void beforeDelete(AuditLog entry) {
        throw new BusinessRuleException("Jurnalul de audit nu se poate șterge");
    }
}
