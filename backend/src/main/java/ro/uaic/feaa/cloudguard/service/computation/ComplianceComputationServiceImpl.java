package ro.uaic.feaa.cloudguard.service.computation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;
import ro.uaic.feaa.cloudguard.domain.repository.ComplianceViolationRepository;

@Service
public class ComplianceComputationServiceImpl implements ComplianceComputationService {

    private static final int MAX_SCORE = 100;

    private final ComplianceViolationRepository violationRepository;

    public ComplianceComputationServiceImpl(ComplianceViolationRepository violationRepository) {
        this.violationRepository = violationRepository;
    }

    @Override
    public Severity computeSeverity(SecurityPolicy policy, CloudResource resource) {
        Severity base = policy.getBaseSeverity();
        return resource.isProduction() ? base.escalate() : base;
    }

    @Override
    public LocalDateTime computeSlaDueAt(Severity severity, LocalDateTime detectedAt) {
        return detectedAt.plusHours(severity.getSlaHours());
    }

    @Override
    public Long computeResolutionHours(ComplianceViolation violation) {
        if (violation.getResolvedAt() == null) {
            return null;
        }
        return Duration.between(violation.getDetectedAt(), violation.getResolvedAt()).toHours();
    }

    @Override
    public int computeComplianceScore(Collection<ComplianceViolation> violations) {
        int penalty = violations.stream()
                .filter(ComplianceViolation::isActive)
                .mapToInt(v -> v.getSeverity().getScorePenalty())
                .sum();
        return Math.max(0, MAX_SCORE - penalty);
    }

    @Override
    public int computeComplianceScore(CloudResource resource) {
        List<ComplianceViolation> active = violationRepository.findByResourceAndStatusIn(
                resource, List.of(ViolationStatus.OPEN, ViolationStatus.UNDER_REVIEW));
        return computeComplianceScore(active);
    }
}
