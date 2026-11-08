package ro.uaic.feaa.cloudguard.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;
import ro.uaic.feaa.cloudguard.domain.model.vo.PortSet;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Teste pentru CloudResourceRepository: interogări derivate, @Query și value objects. */
@DataJpaTest
class CloudResourceRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private CloudResourceRepository repository;

    private Team platform;
    private Team marketing;
    private CloudResource ordersDb;
    private CloudResource assetsBucket;

    @BeforeEach
    void setUp() {
        platform = em.persist(new Team("Platform"));
        marketing = em.persist(new Team("Marketing"));
        ordersDb = em.persist(new CloudResource("aws-rds-orders", "orders-db", ResourceType.DATABASE,
                Environment.PRODUCTION, CloudProvider.AWS,
                new ResourceConfiguration(false, false, PortSet.of(5432, 22)), platform));
        assetsBucket = em.persist(new CloudResource("gcs-assets", "assets-bucket", ResourceType.STORAGE_BUCKET,
                Environment.STAGING, CloudProvider.GCP,
                new ResourceConfiguration(true, true, PortSet.empty()), marketing));
        em.flush();
        em.clear();
    }

    @Test
    void valueObjectIsStoredInTheParentTableAndRestored() {
        CloudResource loaded = repository.findByExternalId("aws-rds-orders").orElseThrow();

        assertThat(loaded.getConfiguration().isEncryptionEnabled()).isFalse();
        assertThat(loaded.getConfiguration().getOpenPorts()).isEqualTo(PortSet.of(5432, 22));
        assertThat(loaded.getResourceType()).isEqualTo(ResourceType.DATABASE);
    }

    @Test
    void derivedQueriesFilterByProperties() {
        assertThat(repository.findByEnvironment(Environment.PRODUCTION))
                .extracting(CloudResource::getExternalId).containsExactly("aws-rds-orders");
        assertThat(repository.findByProvider(CloudProvider.GCP))
                .extracting(CloudResource::getExternalId).containsExactly("gcs-assets");
        assertThat(repository.existsByExternalId("gcs-assets")).isTrue();
        assertThat(repository.existsByExternalId("nu-exista")).isFalse();
    }

    @Test
    void queryByNestedPropertyPathUsesTheTeamName() {
        assertThat(repository.findByTeamName("Marketing"))
                .extracting(CloudResource::getExternalId).containsExactly("gcs-assets");
    }

    @Test
    void customQueryFindsResourcesWithoutEncryption() {
        assertThat(repository.findUnencrypted())
                .extracting(CloudResource::getExternalId).containsExactly("aws-rds-orders");
    }

    @Test
    void customQueryFindsResourcesWithViolationsInGivenStatuses() {
        CloudResource db = em.find(CloudResource.class, ordersDb.getId());
        RegulatoryFramework framework = em.persist(new RegulatoryFramework("NIS2", "Directiva NIS2",
                ObligationLevel.MANDATORY));
        SecurityPolicy policy = SecurityPolicy.create("NIS2-ENC-001", "Criptare", Severity.HIGH,
                ResourceType.DATABASE, ResourceConfiguration.ENCRYPTION_ENABLED, "true");
        framework.addPolicy(policy);
        em.flush();
        em.persist(new ComplianceViolation(db, policy, Severity.CRITICAL,
                LocalDateTime.of(2026, 11, 1, 9, 0), LocalDateTime.of(2026, 11, 2, 9, 0)));
        em.flush();
        em.clear();

        List<CloudResource> withOpen = repository.findWithViolationsIn(List.of(ViolationStatus.OPEN));
        List<CloudResource> withResolved = repository.findWithViolationsIn(List.of(ViolationStatus.RESOLVED));

        assertThat(withOpen).extracting(CloudResource::getExternalId).containsExactly("aws-rds-orders");
        assertThat(withResolved).isEmpty();
    }

    @Test
    void identityIsTrimmedByTheEntityListenerOnInsert() {
        Team team = em.find(Team.class, platform.getId());
        CloudResource saved = repository.saveAndFlush(new CloudResource("  gcp-vm-1  ", "  worker  ",
                ResourceType.VIRTUAL_MACHINE, Environment.DEVELOPMENT, CloudProvider.GCP,
                new ResourceConfiguration(true, false, PortSet.of(443)), team));

        assertThat(saved.getExternalId()).isEqualTo("gcp-vm-1");
        assertThat(saved.getName()).isEqualTo("worker");
    }
}
