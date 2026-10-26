package ro.uaic.feaa.cloudguard.service.workflow;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;

/** Fluxurile pentru resurse, cadre și politici (US-01, US-02). */
public interface ResourceWorkflowService {

    /** US-01: administratorul cloud adaugă o resursă în echipa sa. */
    CloudResource registerResource(AppUser owner, NewResource data);

    RegulatoryFramework defineFramework(AppUser officer, String code, String name, ObligationLevel level);

    /** US-02: ofițerul de securitate definește o politică într-un cadru. */
    SecurityPolicy definePolicy(AppUser officer, String frameworkCode, NewPolicy data);

    /** US-02: politica se asociază unei resurse (many-to-many). */
    CloudResource assignPolicy(AppUser officer, String resourceExternalId, String policyCode);
}
