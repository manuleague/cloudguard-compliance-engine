package ro.uaic.feaa.cloudguard.domain.factory;

import java.time.LocalDateTime;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;

/** Factory pentru agregatul „Încălcare”: calculează severitatea și termenul SLA. */
public interface ComplianceViolationFactory {

    ComplianceViolation create(CloudResource resource, SecurityPolicy policy, LocalDateTime detectedAt);
}
