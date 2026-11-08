package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;

public interface RegulatoryFrameworkRepository extends JpaRepository<RegulatoryFramework, Long> {

    Optional<RegulatoryFramework> findByCode(String code);

    boolean existsByCode(String code);

    List<RegulatoryFramework> findByObligationLevel(ObligationLevel obligationLevel);

    /** Încarcă cadrul împreună cu politicile lui, într-o singură interogare (join fetch). */
    @Query("select distinct f from RegulatoryFramework f left join fetch f.policies where f.code = :code")
    Optional<RegulatoryFramework> findByCodeWithPolicies(@Param("code") String code);
}
