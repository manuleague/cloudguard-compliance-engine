package ro.uaic.feaa.cloudguard.domain.factory;

import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;

/** Factory pentru agregatul „Cadru de conformitate” și pentru componentele lui (politicile). */
public interface RegulatoryFrameworkFactory {

    RegulatoryFramework createFramework(String code, String name, ObligationLevel obligationLevel);

    /** Creează politica și o adaugă în cadru, prin root entity. */
    SecurityPolicy createPolicy(RegulatoryFramework framework, String code, String title, Severity baseSeverity,
                                ResourceType applicableType, String ruleAttribute, String expectedValue);
}
