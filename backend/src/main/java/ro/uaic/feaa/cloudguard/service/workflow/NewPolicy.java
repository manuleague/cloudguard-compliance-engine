package ro.uaic.feaa.cloudguard.service.workflow;

import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.Severity;

/** Datele unei politici noi (US-02). applicableType null = toate tipurile de resurse. */
public record NewPolicy(String code,
                        String title,
                        Severity baseSeverity,
                        ResourceType applicableType,
                        String ruleAttribute,
                        String expectedValue) {
}
