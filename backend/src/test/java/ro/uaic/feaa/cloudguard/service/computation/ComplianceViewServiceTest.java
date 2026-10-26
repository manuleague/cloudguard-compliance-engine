package ro.uaic.feaa.cloudguard.service.computation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.repository.CloudResourceRepository;
import ro.uaic.feaa.cloudguard.domain.repository.ComplianceViolationRepository;
import ro.uaic.feaa.cloudguard.domain.repository.TeamRepository;
import ro.uaic.feaa.cloudguard.support.TestData;

class ComplianceViewServiceTest {

    @Test
    void c5_teamViewAggregatesResourcesViolationsAndAverageScore() {
        TeamRepository teamRepository = mock(TeamRepository.class);
        CloudResourceRepository resourceRepository = mock(CloudResourceRepository.class);
        ComplianceViolationRepository violationRepository = mock(ComplianceViolationRepository.class);
        ComplianceComputationService computation = new ComplianceComputationServiceImpl(violationRepository);
        ComplianceViewService viewService = new ComplianceViewServiceImpl(teamRepository, resourceRepository,
                violationRepository, computation);

        Team team = TestData.team();
        CloudResource clean = TestData.database(Environment.STAGING, true);
        CloudResource failing = TestData.database(Environment.PRODUCTION, false);
        ComplianceViolation critical = new ComplianceViolation(failing, TestData.encryptionPolicy(Severity.HIGH),
                Severity.CRITICAL, LocalDateTime.now(), LocalDateTime.now().plusHours(24));

        when(resourceRepository.findByTeam(team)).thenReturn(List.of(clean, failing));
        when(violationRepository.findByResourceAndStatusIn(eq(clean), any())).thenReturn(List.of());
        when(violationRepository.findByResourceAndStatusIn(eq(failing), any())).thenReturn(List.of(critical));

        TeamComplianceView view = viewService.teamCompliance(team);

        assertThat(view.teamName()).isEqualTo("Platform");
        assertThat(view.resourceCount()).isEqualTo(2);
        assertThat(view.activeViolations()).isEqualTo(1);
        assertThat(view.averageComplianceScore()).isEqualTo(80.0); // (100 + 60) / 2
    }
}
