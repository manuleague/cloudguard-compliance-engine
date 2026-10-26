package ro.uaic.feaa.cloudguard.service.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.AuditAction;
import ro.uaic.feaa.cloudguard.domain.model.AuditLog;
import ro.uaic.feaa.cloudguard.domain.model.BusinessRuleException;
import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ExceptionStatus;
import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;
import ro.uaic.feaa.cloudguard.domain.model.vo.Justification;
import ro.uaic.feaa.cloudguard.domain.model.vo.PortSet;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;
import ro.uaic.feaa.cloudguard.domain.repository.AppUserRepository;
import ro.uaic.feaa.cloudguard.domain.repository.TeamRepository;
import ro.uaic.feaa.cloudguard.service.audit.AuditService;
import ro.uaic.feaa.cloudguard.service.computation.ComplianceComputationService;
import ro.uaic.feaa.cloudguard.service.validation.DomainValidationException;
import ro.uaic.feaa.cloudguard.support.MutableClock;

/**
 * Teste de integrare pentru serviciile de workflow, pe o bază H2 în memorie.
 * Scenariile urmăresc user stories US-01 … US-05 din documentul Sprint 1.
 */
@SpringBootTest
@Transactional
class ComplianceWorkflowServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 27, 10, 0);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NOW, ZoneId.of("Europe/Bucharest"));
        }
    }

    @Autowired private ResourceWorkflowService resourceWorkflow;
    @Autowired private ComplianceWorkflowService complianceWorkflow;
    @Autowired private ComplianceComputationService computation;
    @Autowired private AuditService auditService;
    @Autowired private TeamRepository teamRepository;
    @Autowired private AppUserRepository userRepository;
    @Autowired private MutableClock clock;

    private AppUser owner;
    private AppUser otherTeamOwner;
    private AppUser officer;
    private AppUser auditor;

    @BeforeEach
    void setUp() {
        Team platform = teamRepository.save(new Team("Platform"));
        Team marketing = teamRepository.save(new Team("Marketing"));
        owner = userRepository.save(new AppUser("emanuel", "emanuel@firma.ro", UserRole.RESOURCE_OWNER, platform));
        otherTeamOwner = userRepository.save(new AppUser("sebastian", "sebastian@firma.ro", UserRole.RESOURCE_OWNER, marketing));
        officer = userRepository.save(new AppUser("delia", "delia@firma.ro", UserRole.SECURITY_OFFICER, null));
        auditor = userRepository.save(new AppUser("auditor", "auditor@firma.ro", UserRole.AUDITOR, null));

        resourceWorkflow.defineFramework(officer, "NIS2", "Directiva NIS2", ObligationLevel.MANDATORY);
        resourceWorkflow.definePolicy(officer, "NIS2", new NewPolicy("NIS2-ENC-001",
                "Bazele de date trebuie să fie criptate", Severity.HIGH, ResourceType.DATABASE,
                ResourceConfiguration.ENCRYPTION_ENABLED, "true"));
    }

    private CloudResource registerOrdersDb(boolean encrypted) {
        return resourceWorkflow.registerResource(owner, new NewResource("aws-rds-orders", "orders-db",
                ResourceType.DATABASE, Environment.PRODUCTION, CloudProvider.AWS,
                new ResourceConfiguration(encrypted, false, PortSet.of(5432))));
    }

    /** Pregătește scenariul din US-03: orders-db necriptată, cu politica NIS2-ENC-001. */
    private ComplianceViolation detectViolation() {
        registerOrdersDb(false);
        resourceWorkflow.assignPolicy(officer, "aws-rds-orders", "NIS2-ENC-001");
        return complianceWorkflow.runAssessment(officer).newViolations().get(0);
    }

    // US-01

    @Test
    void us01_ownerAddsResourceToHisTeam() {
        CloudResource resource = registerOrdersDb(false);

        assertThat(resource.getId()).isNotNull();
        assertThat(resource.getTeam().getName()).isEqualTo("Platform");
    }

    @Test
    void us01_resourceWithoutNameIsRejected() {
        NewResource withoutName = new NewResource("aws-x", "", ResourceType.DATABASE, Environment.STAGING,
                CloudProvider.AWS, new ResourceConfiguration(true, false, PortSet.empty()));

        assertThatThrownBy(() -> resourceWorkflow.registerResource(owner, withoutName))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Numele resursei este obligatoriu");
    }

    @Test
    void us01_duplicateExternalIdIsRejected() {
        registerOrdersDb(false);

        assertThatThrownBy(() -> registerOrdersDb(true))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("aws-rds-orders");
    }

    // US-02

    @Test
    void us02_assignedPolicyAppearsOnResource() {
        registerOrdersDb(false);
        CloudResource resource = resourceWorkflow.assignPolicy(officer, "aws-rds-orders", "NIS2-ENC-001");

        assertThat(resource.getPolicies()).extracting("code").containsExactly("NIS2-ENC-001");
    }

    @Test
    void us02_onlySecurityOfficerCanDefinePolicies() {
        NewPolicy policy = new NewPolicy("NIS2-NET-001", "Fără acces public", Severity.MEDIUM, null,
                ResourceConfiguration.PUBLIC_ACCESS, "false");

        assertThatThrownBy(() -> resourceWorkflow.definePolicy(owner, "NIS2", policy))
                .isInstanceOf(BusinessRuleException.class);
    }

    // US-03

    @Test
    void us03_assessmentCreatesCriticalViolationWith24hSla() {
        ComplianceViolation violation = detectViolation();

        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.OPEN);
        assertThat(violation.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(violation.getSlaDueAt()).isEqualTo(NOW.plusHours(24));
    }

    @Test
    void us03_secondAssessmentDoesNotDuplicateActiveViolation() {
        detectViolation();

        AssessmentResult second = complianceWorkflow.runAssessment(officer);

        assertThat(second.checksPerformed()).isEqualTo(1);
        assertThat(second.newViolations()).isEmpty();
    }

    @Test
    void us03_compliantResourceProducesNoViolation() {
        registerOrdersDb(true);
        resourceWorkflow.assignPolicy(officer, "aws-rds-orders", "NIS2-ENC-001");

        assertThat(complianceWorkflow.runAssessment(officer).newViolations()).isEmpty();
    }

    @Test
    void us03_resourceOwnerCannotRunAssessment() {
        assertThatThrownBy(() -> complianceWorkflow.runAssessment(owner))
                .isInstanceOf(BusinessRuleException.class);
    }

    // US-04

    @Test
    void us04_resolveWithJustificationComputesResolutionTime() {
        ComplianceViolation violation = detectViolation();
        complianceWorkflow.startReview(owner, violation.getId());
        clock.advance(Duration.ofHours(5));

        ComplianceViolation resolved = complianceWorkflow.resolve(owner, violation.getId(),
                "Am activat criptarea discului pentru orders-db");

        assertThat(resolved.getStatus()).isEqualTo(ViolationStatus.RESOLVED);
        assertThat(computation.computeResolutionHours(resolved)).isEqualTo(5L);
    }

    @Test
    void us04_shortJustificationIsRejected() {
        ComplianceViolation violation = detectViolation();
        complianceWorkflow.startReview(owner, violation.getId());

        assertThatThrownBy(() -> complianceWorkflow.resolve(owner, violation.getId(), "gata"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Justificarea trebuie să aibă minimum 20 de caractere");
        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.UNDER_REVIEW);
    }

    @Test
    void a2_openViolationCannotBeResolvedWithoutReview() {
        ComplianceViolation violation = detectViolation();

        assertThatThrownBy(() -> complianceWorkflow.resolve(owner, violation.getId(),
                "Am activat criptarea discului pentru orders-db"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("OPEN");
    }

    @Test
    void ownerOfAnotherTeamCannotTouchTheViolation() {
        ComplianceViolation violation = detectViolation();

        assertThatThrownBy(() -> complianceWorkflow.startReview(otherTeamOwner, violation.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nu aparține echipei tale");
    }

    // FR-07, regula A3

    @Test
    void fr07_officerApprovesRiskException() {
        ComplianceViolation violation = detectViolation();
        complianceWorkflow.startReview(owner, violation.getId());
        complianceWorkflow.requestRiskException(owner, violation.getId(),
                "Migrarea pe o instanță criptată este planificată în noiembrie", LocalDate.of(2026, 12, 1));

        ComplianceViolation approved = complianceWorkflow.approveRiskException(officer, violation.getId());

        assertThat(approved.getStatus()).isEqualTo(ViolationStatus.RISK_ACCEPTED);
        assertThat(approved.getRiskException().getStatus()).isEqualTo(ExceptionStatus.APPROVED);
    }

    @Test
    void fr07_rejectedExceptionKeepsViolationUnderReview() {
        ComplianceViolation violation = detectViolation();
        complianceWorkflow.startReview(owner, violation.getId());
        complianceWorkflow.requestRiskException(owner, violation.getId(),
                "Migrarea pe o instanță criptată este planificată în noiembrie", LocalDate.of(2026, 12, 1));

        ComplianceViolation rejected = complianceWorkflow.rejectRiskException(officer, violation.getId());

        assertThat(rejected.getStatus()).isEqualTo(ViolationStatus.UNDER_REVIEW);
        assertThat(rejected.getRiskException().getStatus()).isEqualTo(ExceptionStatus.REJECTED);
    }

    @Test
    void a3_officerCannotApproveExceptionHeRequested() {
        ComplianceViolation violation = detectViolation();
        complianceWorkflow.startReview(officer, violation.getId());
        violation.requestException(officer,
                Justification.of("Cerere făcută chiar de ofițer, pentru test"),
                LocalDate.of(2026, 12, 1));

        assertThatThrownBy(() -> complianceWorkflow.approveRiskException(officer, violation.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("alt utilizator");
    }

    // US-05

    @Test
    void us05_auditorSeesResolutionInAuditLog() {
        ComplianceViolation violation = detectViolation();
        complianceWorkflow.startReview(owner, violation.getId());
        complianceWorkflow.resolve(owner, violation.getId(), "Am activat criptarea discului pentru orders-db");

        List<AuditLog> history = auditService.historyOf(auditor, "ComplianceViolation", violation.getId());

        assertThat(history).extracting(AuditLog::getAction).containsExactly(
                AuditAction.VIOLATION_CREATED, AuditAction.VIOLATION_REVIEW_STARTED, AuditAction.VIOLATION_RESOLVED);
        AuditLog last = history.get(history.size() - 1);
        assertThat(last.getUser().getUsername()).isEqualTo("emanuel");
        assertThat(last.getIpAddress()).isNotBlank();
    }

    @Test
    void us05_resourceOwnerCannotReadAuditLog() {
        assertThatThrownBy(() -> auditService.findAll(owner))
                .isInstanceOf(BusinessRuleException.class);
    }
}
