package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;

public interface RegulatoryFrameworkRepository extends JpaRepository<RegulatoryFramework, Long> {

    Optional<RegulatoryFramework> findByCode(String code);

    boolean existsByCode(String code);
}
