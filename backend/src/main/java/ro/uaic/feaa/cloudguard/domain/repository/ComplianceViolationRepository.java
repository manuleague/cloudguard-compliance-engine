package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;

public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, Long> {

    /** Folosită de regula A1: există deja o încălcare activă pentru perechea resursă–politică? */
    boolean existsByResourceAndPolicyAndStatusIn(CloudResource resource, SecurityPolicy policy,
                                                 Collection<ViolationStatus> statuses);

    List<ComplianceViolation> findByResourceAndStatusIn(CloudResource resource, Collection<ViolationStatus> statuses);

    List<ComplianceViolation> findByStatusIn(Collection<ViolationStatus> statuses);
}
