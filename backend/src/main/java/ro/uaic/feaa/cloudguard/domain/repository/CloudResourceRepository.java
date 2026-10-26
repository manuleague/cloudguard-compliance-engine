package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.Team;

public interface CloudResourceRepository extends JpaRepository<CloudResource, Long> {

    Optional<CloudResource> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<CloudResource> findByTeam(Team team);
}
