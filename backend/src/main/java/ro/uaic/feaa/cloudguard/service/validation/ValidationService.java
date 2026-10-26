package ro.uaic.feaa.cloudguard.service.validation;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;

/**
 * Serviciul de validare a domeniului: regulile JavaBean Validation din entități
 * plus regulile care au nevoie de baza de date sau de utilizatorul curent.
 */
public interface ValidationService {

    /** Rulează adnotările de validare (V1–V5) și returnează obiectul dacă e valid. */
    <T> T validate(T object);

    /** V2: externalId trebuie să fie unic. */
    void checkUniqueExternalId(String externalId);

    /** V3 (unicitate): codul politicii trebuie să fie unic. */
    void checkUniquePolicyCode(String policyCode);

    /** A1: cel mult o încălcare activă pentru o pereche resursă–politică. */
    void checkNoActiveViolation(CloudResource resource, SecurityPolicy policy);

    /** FR-01: utilizatorul trebuie să aibă unul dintre rolurile permise. */
    void checkRole(AppUser user, UserRole... allowedRoles);

    /** Un administrator cloud lucrează doar cu resursele echipei sale. */
    void checkTeamOwnership(AppUser user, CloudResource resource);
}
