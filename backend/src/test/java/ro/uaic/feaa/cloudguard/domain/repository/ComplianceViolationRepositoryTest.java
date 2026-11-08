package ro.uaic.feaa.cloudguard.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ExceptionStatus;
import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;
import ro.uaic.feaa.cloudguard.domain.model.vo.Justification;
import ro.uaic.feaa.cloudguard.domain.model.vo.PortSet;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Teste pentru agregatul „Încălcare”: relații, cascadă, orphanRemoval și interogări personalizate. */
@DataJpaTest
class ComplianceViolationRepositoryTest {

    private static final LocalDateTime DETECTED = LocalDateTime.of(2026, 11, 1, 9, 0);
    private static final List<ViolationStatus> ACTIVE = List.of(ViolationStatus.OPEN, ViolationStatus.UNDER_REVIEW);

    @Autowired private TestEntityManager em;
    @Autowired private ComplianceViolationRepository repository;

    private Team platform;
    private CloudResource db;
    private SecurityPolicy policy;
    private AppUser owner;
    private AppUser officer;

    @BeforeEach
    void setUp() {
        platform = em.persist(new Team("Platform"));
        owner = em.persist(new AppUser("emanuel", "emanuel@firma.ro", UserRole.RESOURCE_OWNER, platform));
        officer = em.persist(new AppUser("delia", "delia@firma.ro", UserRole.SECURITY_OFFICER, null));
        db = em.persist(new CloudResource("aws-rds-orders", "orders-db", ResourceType.DATABASE,
                Environment.PRODUCTION, CloudProvider.AWS,
                new ResourceConfiguration(false, false, PortSet.of(5432)), platform));
        RegulatoryFramework framework = em.persist(new RegulatoryFramework("NIS2", "Directiva NIS2",
                ObligationLevel.MANDATORY));
        policy = SecurityPolicy.create("NIS2-ENC-001", "Criptare", Severity.HIGH, ResourceType.DATABASE,
                ResourceConfiguration.ENCRYPTION_ENABLED, "true");
        framework.addPolicy(policy);
        em.flush();
    }

    private ComplianceViolation persistViolation(Severity severity, LocalDateTime detectedAt) {
        ComplianceViolation violation = new ComplianceViolation(db, policy, severity, detectedAt,
                detectedAt.plusHours(severity.getSlaHours()));
        return em.persistAndFlush(violation);
    }

    @Test
    void savedViolationKeepsEnumsAndAssociations() {
        ComplianceViolation saved = persistViolation(Severity.CRITICAL, DETECTED);
        em.clear();

        ComplianceViolation loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(ViolationStatus.OPEN);
        assertThat(loaded.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(loaded.getResource().getExternalId()).isEqualTo("aws-rds-orders");
        assertThat(loaded.getPolicy().getCode()).isEqualTo("NIS2-ENC-001");
    }

    @Test
    void riskExceptionIsSavedByCascadeFromTheRoot() {
        ComplianceViolation violation = persistViolation(Severity.HIGH, DETECTED);
        violation.startReview(owner);
        violation.requestException(owner, Justification.of("Migrarea criptării e planificată în Q1"),
                DETECTED.toLocalDate().plusDays(30));

        repository.saveAndFlush(violation);
        em.clear();

        ComplianceViolation loaded = repository.findById(violation.getId()).orElseThrow();
        assertThat(loaded.getRiskException()).isNotNull();
        assertThat(loaded.getRiskException().getId()).isNotNull();
        assertThat(loaded.getRiskException().getStatus()).isEqualTo(ExceptionStatus.PENDING);
        assertThat(loaded.getRiskException().getJustification().getText())
                .isEqualTo("Migrarea criptării e planificată în Q1");
    }

    @Test
    void approvingTheExceptionPersistsRiskAccepted() {
        ComplianceViolation violation = persistViolation(Severity.HIGH, DETECTED);
        violation.startReview(owner);
        violation.requestException(owner, Justification.of("Migrarea criptării e planificată în Q1"),
                DETECTED.toLocalDate().plusDays(30));
        repository.saveAndFlush(violation);

        violation.getRiskException().approve(officer);
        repository.saveAndFlush(violation);
        em.clear();

        ComplianceViolation loaded = repository.findById(violation.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(ViolationStatus.RISK_ACCEPTED);
        assertThat(loaded.getRiskException().getApprovedBy().getUsername()).isEqualTo("delia");
    }

    @Test
    void resolvedViolationStoresResolutionTimeAndJustification() {
        ComplianceViolation violation = persistViolation(Severity.HIGH, DETECTED);
        violation.startReview(owner);
        violation.resolve(owner, Justification.of("Am activat criptarea discului"), DETECTED.plusHours(5));
        repository.saveAndFlush(violation);
        em.clear();

        ComplianceViolation loaded = repository.findById(violation.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(ViolationStatus.RESOLVED);
        assertThat(loaded.timeToResolveHours()).isEqualTo(5L);
        assertThat(loaded.getResolution().getText()).isEqualTo("Am activat criptarea discului");
    }

    @Test
    void customQueryReturnsOnlyOverdueActiveViolations() {
        ComplianceViolation overdue = persistViolation(Severity.CRITICAL, DETECTED);              // SLA 24 h
        em.clear();

        LocalDateTime twoDaysLater = DETECTED.plusDays(2);
        LocalDateTime sameDay = DETECTED.plusHours(2);

        assertThat(repository.findOverdue(ACTIVE, twoDaysLater)).extracting(ComplianceViolation::getId)
                .containsExactly(overdue.getId());
        assertThat(repository.findOverdue(ACTIVE, sameDay)).isEmpty();
    }

    @Test
    void customQueryFindsViolationsOfATeamInOneQuery() {
        ComplianceViolation violation = persistViolation(Severity.HIGH, DETECTED);
        Team marketing = em.persist(new Team("Marketing"));
        em.flush();
        em.clear();

        Team platformRef = em.find(Team.class, platform.getId());
        Team marketingRef = em.find(Team.class, marketing.getId());

        assertThat(repository.findForTeam(platformRef, ACTIVE)).extracting(ComplianceViolation::getId)
                .containsExactly(violation.getId());
        assertThat(repository.findForTeam(marketingRef, ACTIVE)).isEmpty();
    }

    @Test
    void derivedQueriesCountAndFilterBySeverityAndStatus() {
        persistViolation(Severity.CRITICAL, DETECTED);
        em.clear();

        assertThat(repository.countByStatus(ViolationStatus.OPEN)).isEqualTo(1);
        assertThat(repository.countByStatus(ViolationStatus.RESOLVED)).isZero();
        assertThat(repository.findBySeverityOrderByDetectedAtDesc(Severity.CRITICAL)).hasSize(1);
        assertThat(repository.findByStatusIn(ACTIVE)).hasSize(1);
        assertThat(repository.existsByResourceAndPolicyAndStatusIn(
                em.find(CloudResource.class, db.getId()), em.find(SecurityPolicy.class, policy.getId()), ACTIVE))
                .isTrue();
    }
}
