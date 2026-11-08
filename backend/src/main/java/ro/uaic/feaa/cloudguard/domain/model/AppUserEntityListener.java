package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/** Normalizează adresa de e-mail a utilizatorului la salvare. */
public class AppUserEntityListener {

    @PrePersist
    public void beforeInsert(AppUser user) {
        user.normalizeEmail();
    }

    @PreUpdate
    public void beforeUpdate(AppUser user) {
        user.normalizeEmail();
    }
}
