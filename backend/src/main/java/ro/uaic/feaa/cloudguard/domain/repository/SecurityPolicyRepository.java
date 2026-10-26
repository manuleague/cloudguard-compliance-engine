package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;

/** Politicile se modifică prin RegulatoryFramework; repository-ul servește la căutare. */
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {

    Optional<SecurityPolicy> findByCode(String code);

    boolean existsByCode(String code);
}
