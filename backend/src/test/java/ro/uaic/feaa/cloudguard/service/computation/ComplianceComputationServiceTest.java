package ro.uaic.feaa.cloudguard.service.computation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.model.vo.Justification;
import ro.uaic.feaa.cloudguard.support.TestData;

class ComplianceComputationServiceTest {

    private static final LocalDateTime DETECTED = LocalDateTime.of(2026, 10, 27, 10, 0);

    private ComplianceComputationService computation;

    @BeforeEach
    void setUp() {
        computation = new ComplianceComputationServiceImpl();
    }

    @Test
    void c1_productionRaisesSeverityByOneLevel() {
        CloudResource prod = TestData.database(Environment.PRODUCTION, false);
        assertThat(computation.computeSeverity(TestData.encryptionPolicy(Severity.HIGH), prod))
                .isEqualTo(Severity.CRITICAL);
        assertThat(computation.computeSeverity(TestData.encryptionPolicy(Severity.LOW), prod))
                .isEqualTo(Severity.MEDIUM);
    }

    @Test
    void c1_severityNeverExceedsCritical() {
        CloudResource prod = TestData.database(Environment.PRODUCTION, false);
        assertThat(computation.computeSeverity(TestData.encryptionPolicy(Severity.CRITICAL), prod))
                .isEqualTo(Severity.CRITICAL);
    }

    @Test
    void c1_nonProductionKeepsBaseSeverity() {
        CloudResource dev = TestData.database(Environment.DEVELOPMENT, false);
        assertThat(computation.computeSeverity(TestData.encryptionPolicy(Severity.MEDIUM), dev))
                .isEqualTo(Severity.MEDIUM);
    }

    @Test
    void c2_slaDependsOnSeverity() {
        assertThat(computation.computeSlaDueAt(Severity.CRITICAL, DETECTED)).isEqualTo(DETECTED.plusHours(24));
        assertThat(computation.computeSlaDueAt(Severity.HIGH, DETECTED)).isEqualTo(DETECTED.plusHours(72));
        assertThat(computation.computeSlaDueAt(Severity.MEDIUM, DETECTED)).isEqualTo(DETECTED.plusDays(7));
        assertThat(computation.computeSlaDueAt(Severity.LOW, DETECTED)).isEqualTo(DETECTED.plusDays(30));
    }

    @Test
    void c3_resolutionHoursAreComputedOnlyForClosedViolations() {
        ComplianceViolation violation = violation(Severity.HIGH);
        assertThat(computation.computeResolutionHours(violation)).isNull();

        AppUser owner = new AppUser("emanuel", "emanuel@firma.ro", UserRole.RESOURCE_OWNER, null);
        violation.startReview(owner);
        violation.resolve(owner, Justification.of("Am activat criptarea discului pentru orders-db"),
                DETECTED.plusHours(30));

        assertThat(computation.computeResolutionHours(violation)).isEqualTo(30L);
    }

    @Test
    void c3_entityComputesTimeToResolve() {
        ComplianceViolation violation = violation(Severity.MEDIUM);
        AppUser owner = new AppUser("emanuel", "emanuel@firma.ro", UserRole.RESOURCE_OWNER, null);
        violation.startReview(owner);
        violation.resolve(owner, Justification.of("Am închis accesul public la bucket"), DETECTED.plusHours(49));

        assertThat(violation.timeToResolveHours()).isEqualTo(49L);
    }

    @Test
    void c4_resourceComputesItsOwnComplianceScore() {
        CloudResource resource = TestData.database(Environment.PRODUCTION, false);
        new ComplianceViolation(resource, TestData.encryptionPolicy(Severity.HIGH), Severity.CRITICAL,
                DETECTED, DETECTED.plusHours(24));

        assertThat(resource.complianceScore()).isEqualTo(60);
        assertThat(computation.computeComplianceScore(resource)).isEqualTo(60);
    }

    @Test
    void c4_scoreSubtractsPenaltiesOfActiveViolations() {
        int score = computation.computeComplianceScore(List.of(violation(Severity.CRITICAL), violation(Severity.HIGH)));
        assertThat(score).isEqualTo(40);
    }

    @Test
    void c4_scoreNeverGoesBelowZero() {
        int score = computation.computeComplianceScore(List.of(
                violation(Severity.CRITICAL), violation(Severity.CRITICAL), violation(Severity.CRITICAL)));
        assertThat(score).isZero();
    }

    @Test
    void c4_resolvedViolationsDoNotCount() {
        ComplianceViolation resolved = violation(Severity.CRITICAL);
        AppUser owner = new AppUser("emanuel", "emanuel@firma.ro", UserRole.RESOURCE_OWNER, null);
        resolved.startReview(owner);
        resolved.resolve(owner, Justification.of("Am activat criptarea discului pentru orders-db"),
                DETECTED.plusHours(2));

        assertThat(computation.computeComplianceScore(List.of(resolved, violation(Severity.LOW)))).isEqualTo(95);
    }

    private ComplianceViolation violation(Severity severity) {
        return new ComplianceViolation(TestData.database(Environment.PRODUCTION, false),
                TestData.encryptionPolicy(severity), severity, DETECTED,
                computation.computeSlaDueAt(severity, DETECTED));
    }
}
