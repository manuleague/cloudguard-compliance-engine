package ro.uaic.feaa.cloudguard.domain.factory;

import org.springframework.stereotype.Component;

import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.service.validation.ValidationService;

@Component
public class RegulatoryFrameworkFactoryImpl implements RegulatoryFrameworkFactory {

    private final ValidationService validationService;

    public RegulatoryFrameworkFactoryImpl(ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public RegulatoryFramework createFramework(String code, String name, ObligationLevel obligationLevel) {
        String normalizedCode = code == null ? null : code.trim().toUpperCase();
        return validationService.validate(new RegulatoryFramework(normalizedCode, name, obligationLevel));
    }

    @Override
    public SecurityPolicy createPolicy(RegulatoryFramework framework, String code, String title,
                                       Severity baseSeverity, ResourceType applicableType,
                                       String ruleAttribute, String expectedValue) {
        SecurityPolicy policy = new SecurityPolicy(code, title, baseSeverity, applicableType,
                ruleAttribute, expectedValue);
        validationService.validate(policy);
        validationService.checkUniquePolicyCode(code);
        framework.addPolicy(policy);
        return policy;
    }
}
