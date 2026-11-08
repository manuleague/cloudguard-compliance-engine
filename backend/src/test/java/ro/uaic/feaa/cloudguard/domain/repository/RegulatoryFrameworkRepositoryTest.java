package ro.uaic.feaa.cloudguard.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ro.uaic.feaa.cloudguard.domain.model.BusinessRuleException;
import ro.uaic.feaa.cloudguard.domain.model.EncryptionPolicy;
import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.OpenPortsPolicy;
import ro.uaic.feaa.cloudguard.domain.model.PublicAccessPolicy;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Teste pentru agregatul „Cadru de conformitate”: cascadă, orphanRemoval și moștenirea politicilor. */
@DataJpaTest
class RegulatoryFrameworkRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private RegulatoryFrameworkRepository frameworkRepository;
    @Autowired private SecurityPolicyRepository policyRepository;

    @BeforeEach
    void setUp() {
        RegulatoryFramework nis2 = new RegulatoryFramework("NIS2", "Directiva NIS2", ObligationLevel.MANDATORY);
        nis2.addPolicy(SecurityPolicy.create("NIS2-ENC-001", "Baze de date criptate", Severity.HIGH,
                ResourceType.DATABASE, ResourceConfiguration.ENCRYPTION_ENABLED, "true"));
        nis2.addPolicy(SecurityPolicy.create("NIS2-NET-002", "Fără acces public", Severity.CRITICAL,
                ResourceType.STORAGE_BUCKET, ResourceConfiguration.PUBLIC_ACCESS, "false"));
        nis2.addPolicy(SecurityPolicy.create("NIS2-NET-003", "Doar portul 443", Severity.MEDIUM,
                ResourceType.VIRTUAL_MACHINE, ResourceConfiguration.OPEN_PORTS, "443"));
        frameworkRepository.saveAndFlush(nis2);

        RegulatoryFramework intern = new RegulatoryFramework("INT", "Politici interne", ObligationLevel.RECOMMENDED);
        frameworkRepository.saveAndFlush(intern);
        em.clear();
    }

    @Test
    void policiesAreSavedByCascadeWithTheirFramework() {
        RegulatoryFramework loaded = frameworkRepository.findByCodeWithPolicies("NIS2").orElseThrow();

        assertThat(loaded.getPolicies()).extracting(SecurityPolicy::getCode)
                .containsExactlyInAnyOrder("NIS2-ENC-001", "NIS2-NET-002", "NIS2-NET-003");
        assertThat(loaded.getPolicies()).allSatisfy(p -> assertThat(p.getId()).isNotNull());
        assertThat(loaded.getPolicies()).allSatisfy(p -> assertThat(p.getFramework().getCode()).isEqualTo("NIS2"));
    }

    @Test
    void loadedPoliciesAreInstancesOfTheMatchingSubclass() {
        assertThat(policyRepository.findByCode("NIS2-ENC-001").orElseThrow()).isInstanceOf(EncryptionPolicy.class);
        assertThat(policyRepository.findByCode("NIS2-NET-002").orElseThrow()).isInstanceOf(PublicAccessPolicy.class);
        assertThat(policyRepository.findByCode("NIS2-NET-003").orElseThrow()).isInstanceOf(OpenPortsPolicy.class);
        assertThat(policyRepository.findByCode("NIS2-ENC-001").orElseThrow().getRuleAttribute())
                .isEqualTo("encryptionEnabled");
    }

    @Test
    void policyQueriesAreByPropertyAndByPropertyPath() {
        assertThat(policyRepository.findByApplicableType(ResourceType.DATABASE))
                .extracting(SecurityPolicy::getCode).containsExactly("NIS2-ENC-001");
        assertThat(policyRepository.findByFrameworkCode("NIS2")).hasSize(3);
        assertThat(policyRepository.findByFrameworkCode("INT")).isEmpty();
        assertThat(policyRepository.existsByCode("NIS2-NET-002")).isTrue();
    }

    @Test
    void frameworkQueriesFilterByObligationLevel() {
        List<RegulatoryFramework> mandatory = frameworkRepository.findByObligationLevel(ObligationLevel.MANDATORY);

        assertThat(mandatory).extracting(RegulatoryFramework::getCode).containsExactly("NIS2");
        assertThat(frameworkRepository.findByCode("INT")).isPresent();
        assertThat(frameworkRepository.existsByCode("XYZ")).isFalse();
    }

    @Test
    void removedPolicyIsDeletedByOrphanRemoval() {
        RegulatoryFramework framework = frameworkRepository.findByCodeWithPolicies("NIS2").orElseThrow();

        framework.removePolicy("NIS2-NET-003");
        frameworkRepository.saveAndFlush(framework);
        em.clear();

        assertThat(policyRepository.existsByCode("NIS2-NET-003")).isFalse();
        assertThat(policyRepository.findByFrameworkCode("NIS2")).hasSize(2);
    }

    @Test
    void removingAnUnknownPolicyIsRejected() {
        RegulatoryFramework framework = frameworkRepository.findByCodeWithPolicies("NIS2").orElseThrow();

        assertThatThrownBy(() -> framework.removePolicy("NIS2-XXX-999"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deletingTheRootDeletesItsPolicies() {
        RegulatoryFramework framework = frameworkRepository.findByCode("NIS2").orElseThrow();

        frameworkRepository.delete(framework);
        frameworkRepository.flush();
        em.clear();

        assertThat(frameworkRepository.existsByCode("NIS2")).isFalse();
        assertThat(policyRepository.findAll()).isEmpty();
    }
}
