package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;

/**
 * Politicile se modifică prin RegulatoryFramework; repository-ul servește la căutare.
 * Interogările sunt polimorfe: returnează subclasele EncryptionPolicy, PublicAccessPolicy, OpenPortsPolicy.
 */
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {

    Optional<SecurityPolicy> findByCode(String code);

    boolean existsByCode(String code);

    List<SecurityPolicy> findByApplicableType(ResourceType applicableType);

    /** Cale de proprietăți: framework.code. */
    List<SecurityPolicy> findByFrameworkCode(String frameworkCode);
}
