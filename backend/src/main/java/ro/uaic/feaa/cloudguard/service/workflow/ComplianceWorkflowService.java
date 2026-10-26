package ro.uaic.feaa.cloudguard.service.workflow;

import java.time.LocalDate;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.RiskException;

/** Fluxul evaluare -> încălcare -> rezolvare sau excepție de risc (diagrama de activitate). */
public interface ComplianceWorkflowService {

    /** US-03: verifică toate resursele cu politicile asociate și creează încălcările noi. */
    AssessmentResult runAssessment(AppUser officer);

    /** OPEN -> UNDER_REVIEW */
    ComplianceViolation startReview(AppUser user, Long violationId);

    /** US-04: UNDER_REVIEW -> RESOLVED cu justificare. */
    ComplianceViolation resolve(AppUser owner, Long violationId, String justification);

    /** FR-07: administratorul cere o excepție de risc. */
    RiskException requestRiskException(AppUser owner, Long violationId, String justification, LocalDate validUntil);

    /** FR-07: ofițerul aprobă excepția -> RISK_ACCEPTED. */
    ComplianceViolation approveRiskException(AppUser officer, Long violationId);

    /** FR-07: ofițerul respinge excepția; încălcarea rămâne UNDER_REVIEW. */
    ComplianceViolation rejectRiskException(AppUser officer, Long violationId);
}
