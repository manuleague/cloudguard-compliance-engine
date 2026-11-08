package ro.uaic.feaa.cloudguard.domain.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;

public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, Long> {

    /** Folosită de regula A1: există deja o încălcare activă pentru perechea resursă–politică? */
    boolean existsByResourceAndPolicyAndStatusIn(CloudResource resource, SecurityPolicy policy,
                                                 Collection<ViolationStatus> statuses);

    List<ComplianceViolation> findByResourceAndStatusIn(CloudResource resource, Collection<ViolationStatus> statuses);

    List<ComplianceViolation> findByStatusIn(Collection<ViolationStatus> statuses);

    List<ComplianceViolation> findBySeverityOrderByDetectedAtDesc(Severity severity);

    long countByStatus(ViolationStatus status);

    /** Filtru personalizat: încălcările din statusurile date al căror termen SLA a trecut deja. */
    @Query("select v from ComplianceViolation v "
            + "where v.status in :statuses and v.slaDueAt < :now order by v.slaDueAt asc")
    List<ComplianceViolation> findOverdue(@Param("statuses") Collection<ViolationStatus> statuses,
                                          @Param("now") LocalDateTime now);

    /** Filtru personalizat: încălcările tuturor resurselor unei echipe, într-o singură interogare. */
    @Query("select v from ComplianceViolation v "
            + "where v.resource.team = :team and v.status in :statuses")
    List<ComplianceViolation> findForTeam(@Param("team") Team team,
                                          @Param("statuses") Collection<ViolationStatus> statuses);
}
