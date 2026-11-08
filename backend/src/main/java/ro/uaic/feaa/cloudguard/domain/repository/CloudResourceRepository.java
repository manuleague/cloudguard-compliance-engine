package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;

public interface CloudResourceRepository extends JpaRepository<CloudResource, Long> {

    Optional<CloudResource> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<CloudResource> findByTeam(Team team);

    /** Căutare după proprietatea „name” a echipei (cale de proprietăți: team.name). */
    List<CloudResource> findByTeamName(String teamName);

    List<CloudResource> findByEnvironment(Environment environment);

    List<CloudResource> findByProvider(CloudProvider provider);

    /** Filtru personalizat: resursele care au cel puțin o încălcare într-unul dintre statusurile date. */
    @Query("select distinct r from CloudResource r join r.violations v where v.status in :statuses")
    List<CloudResource> findWithViolationsIn(@Param("statuses") Collection<ViolationStatus> statuses);

    /** Filtru personalizat pe value object-ul embeddat: resursele fără criptare. */
    @Query("select r from CloudResource r where r.configuration.encryptionEnabled = false")
    List<CloudResource> findUnencrypted();
}
