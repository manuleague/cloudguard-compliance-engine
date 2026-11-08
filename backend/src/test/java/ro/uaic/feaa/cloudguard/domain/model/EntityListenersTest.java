package ro.uaic.feaa.cloudguard.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.uaic.feaa.cloudguard.support.PersistenceTestSupport.assertThrowsInChain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ro.uaic.feaa.cloudguard.domain.model.vo.PortSet;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/**
 * Teste pentru regulile de calcul și de audit aplicate prin @EntityListeners
 * (@PrePersist, @PreUpdate, @PreRemove).
 */
@DataJpaTest
class EntityListenersTest {

    private static final LocalDateTime DETECTED = LocalDateTime.of(2026, 11, 1, 9, 0);

    @Autowired private TestEntityManager em;

    private Team platform;
    private CloudResource db;
    private SecurityPolicy policy;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        platform = em.persist(new Team("Platform"));
        owner = em.persist(new AppUser("emanuel", "emanuel@firma.ro", UserRole.RESOURCE_OWNER, platform));
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

    // @PrePersist: reguli de calcul

    @Test
    void prePersistComputesTheSlaWhenItIsMissing() {
        ComplianceViolation violation = new ComplianceViolation(db, policy, Severity.CRITICAL, DETECTED, null);

        em.persistAndFlush(violation);

        assertThat(violation.getSlaDueAt()).isEqualTo(DETECTED.plusHours(24));
    }

    @Test
    void prePersistKeepsAnSlaThatWasAlreadyComputed() {
        LocalDateTime custom = DETECTED.plusHours(10);
        ComplianceViolation violation = new ComplianceViolation(db, policy, Severity.CRITICAL, DETECTED, custom);

        em.persistAndFlush(violation);

        assertThat(violation.getSlaDueAt()).isEqualTo(custom);
    }

    @Test
    void prePersistNormalizesTheEmailOfAUser() {
        AppUser user = em.persistAndFlush(new AppUser("sebastian", "  Sebastian@Firma.RO ",
                UserRole.AUDITOR, null));

        assertThat(user.getEmail()).isEqualTo("sebastian@firma.ro");
    }

    // @PreUpdate: reguli de audit

    @Test
    void preUpdateRejectsAResolvedViolationWithoutResolutionTime() {
        ComplianceViolation violation = em.persistAndFlush(new ComplianceViolation(db, policy, Severity.HIGH,
                DETECTED, DETECTED.plusHours(72)));
        violation.startReview(owner);
        em.flush();

        // fraudă simulată: statusul e schimbat direct, fără metodele agregatului
        org.springframework.test.util.ReflectionTestUtils.setField(violation, "status", ViolationStatus.RESOLVED);

        assertThrowsInChain(em::flush, BusinessRuleException.class);
    }

    @Test
    void preUpdateRejectsResolutionBeforeDetection() {
        ComplianceViolation violation = em.persistAndFlush(new ComplianceViolation(db, policy, Severity.HIGH,
                DETECTED, DETECTED.plusHours(72)));
        violation.startReview(owner);
        em.flush();

        org.springframework.test.util.ReflectionTestUtils.setField(violation, "resolvedAt", DETECTED.minusHours(1));

        assertThrowsInChain(em::flush, BusinessRuleException.class);
    }

    // @PreRemove: reguli de audit

    @Test
    void preRemoveForbidsDeletingAViolation() {
        ComplianceViolation violation = em.persistAndFlush(new ComplianceViolation(db, policy, Severity.HIGH,
                DETECTED, DETECTED.plusHours(72)));

        assertThrowsInChain(() -> {
            em.remove(violation);
            em.flush();
        }, BusinessRuleException.class);
    }

    @Test
    void preRemoveForbidsDeletingAResourceThatHasViolations() {
        em.persistAndFlush(new ComplianceViolation(db, policy, Severity.HIGH, DETECTED, DETECTED.plusHours(72)));

        assertThrowsInChain(() -> {
            em.remove(db);
            em.flush();
        }, BusinessRuleException.class);
    }

    @Test
    void preRemoveAllowsDeletingAResourceWithoutViolations() {
        CloudResource clean = em.persistAndFlush(new CloudResource("gcs-assets", "assets-bucket",
                ResourceType.STORAGE_BUCKET, Environment.STAGING, CloudProvider.GCP,
                new ResourceConfiguration(true, false, PortSet.empty()), platform));
        clean.detachFromTeam();

        em.remove(clean);
        em.flush();

        assertThat(em.find(CloudResource.class, clean.getId())).isNull();
    }

    @Test
    void auditLogEntriesCannotBeRemovedThroughJpa() {
        AuditLog entry = em.persistAndFlush(new AuditLog(owner, AuditAction.VIOLATION_CREATED,
                "ComplianceViolation", 1L, "127.0.0.1", DETECTED));

        assertThrowsInChain(() -> em.remove(entry), BusinessRuleException.class);
    }
}
