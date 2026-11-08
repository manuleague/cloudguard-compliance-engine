package ro.uaic.feaa.cloudguard.service.computation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;
import ro.uaic.feaa.cloudguard.domain.repository.CloudResourceRepository;
import ro.uaic.feaa.cloudguard.domain.repository.ComplianceViolationRepository;
import ro.uaic.feaa.cloudguard.domain.repository.TeamRepository;

@Service
@Transactional(readOnly = true)
public class ComplianceViewServiceImpl implements ComplianceViewService {

    private static final List<ViolationStatus> ACTIVE =
            List.of(ViolationStatus.OPEN, ViolationStatus.UNDER_REVIEW);

    private final TeamRepository teamRepository;
    private final CloudResourceRepository resourceRepository;
    private final ComplianceViolationRepository violationRepository;
    private final ComplianceComputationService computationService;

    public ComplianceViewServiceImpl(TeamRepository teamRepository,
                                     CloudResourceRepository resourceRepository,
                                     ComplianceViolationRepository violationRepository,
                                     ComplianceComputationService computationService) {
        this.teamRepository = teamRepository;
        this.resourceRepository = resourceRepository;
        this.violationRepository = violationRepository;
        this.computationService = computationService;
    }

    @Override
    public TeamComplianceView teamCompliance(Team team) {
        List<CloudResource> resources = resourceRepository.findByTeam(team);
        // O singură interogare pentru toate încălcările active ale echipei, grupate apoi pe resursă.
        List<ComplianceViolation> active = violationRepository.findForTeam(team, ACTIVE);
        Map<CloudResource, List<ComplianceViolation>> byResource =
                active.stream().collect(Collectors.groupingBy(ComplianceViolation::getResource));
        long activeViolations = active.size();
        double scoreSum = 0;
        for (CloudResource resource : resources) {
            scoreSum += computationService.computeComplianceScore(byResource.getOrDefault(resource, List.of()));
        }
        double average = resources.isEmpty() ? 100.0 : scoreSum / resources.size();
        return new TeamComplianceView(team.getId(), team.getName(), resources.size(),
                activeViolations, Math.round(average * 10) / 10.0);
    }

    @Override
    public List<TeamComplianceView> allTeamsCompliance() {
        return teamRepository.findAll().stream().map(this::teamCompliance).toList();
    }
}
