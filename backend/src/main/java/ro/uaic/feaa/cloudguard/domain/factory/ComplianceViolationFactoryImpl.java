package ro.uaic.feaa.cloudguard.domain.factory;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.service.computation.ComplianceComputationService;
import ro.uaic.feaa.cloudguard.service.validation.ValidationService;

@Component
public class ComplianceViolationFactoryImpl implements ComplianceViolationFactory {

    private final ComplianceComputationService computationService;
    private final ValidationService validationService;

    public ComplianceViolationFactoryImpl(ComplianceComputationService computationService,
                                          ValidationService validationService) {
        this.computationService = computationService;
        this.validationService = validationService;
    }

    @Override
    public ComplianceViolation create(CloudResource resource, SecurityPolicy policy, LocalDateTime detectedAt) {
        validationService.checkNoActiveViolation(resource, policy);
        Severity severity = computationService.computeSeverity(policy, resource);
        LocalDateTime slaDueAt = computationService.computeSlaDueAt(severity, detectedAt);
        return validationService.validate(new ComplianceViolation(resource, policy, severity, detectedAt, slaDueAt));
    }
}
