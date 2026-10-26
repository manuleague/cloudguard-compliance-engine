package ro.uaic.feaa.cloudguard.service.computation;

import java.time.LocalDateTime;
import java.util.Collection;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;

/** Serviciu de calcul pentru câmpurile entităților (regulile C1–C4). */
public interface ComplianceComputationService {

    /** C1: severitatea de bază, crescută cu un nivel în PRODUCTION (maximum CRITICAL). */
    Severity computeSeverity(SecurityPolicy policy, CloudResource resource);

    /** C2: detectedAt + 24 h / 72 h / 7 zile / 30 de zile, după severitate. */
    LocalDateTime computeSlaDueAt(Severity severity, LocalDateTime detectedAt);

    /** C3: orele dintre detectare și rezolvare; null dacă încălcarea nu e închisă. */
    Long computeResolutionHours(ComplianceViolation violation);

    /** C4: 100 minus penalizările încălcărilor active, minimum 0. */
    int computeComplianceScore(Collection<ComplianceViolation> violations);

    /** C4 pentru o resursă, citind încălcările ei active din baza de date. */
    int computeComplianceScore(CloudResource resource);
}
