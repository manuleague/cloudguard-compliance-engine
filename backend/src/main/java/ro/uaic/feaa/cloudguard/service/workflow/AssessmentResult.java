package ro.uaic.feaa.cloudguard.service.workflow;

import java.util.List;

import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;

/** Rezultatul unei evaluări de conformitate (US-03). */
public record AssessmentResult(int resourcesChecked,
                               int checksPerformed,
                               List<ComplianceViolation> newViolations) {
}
