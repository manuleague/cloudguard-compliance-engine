package ro.uaic.feaa.cloudguard.service.computation;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.stereotype.Service;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;

/**
 * Regulile C1–C2 se calculează aici, la crearea încălcării.
 * C3 și C4 sunt câmpuri calculate ale entităților (timeToResolveHours, complianceScore);
 * serviciul le expune ca să poată fi folosite de celelalte servicii și de vederi.
 */
@Service
public class ComplianceComputationServiceImpl implements ComplianceComputationService {

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
        return violation.getResolvedAt() == null ? null : violation.timeToResolveHours();
    }

    @Override
    public int computeComplianceScore(Collection<ComplianceViolation> violations) {
        return CloudResource.scoreFor(violations);
    }

    @Override
    public int computeComplianceScore(CloudResource resource) {
        return resource.complianceScore();
    }
}
