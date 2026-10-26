package ro.uaic.feaa.cloudguard.support;

import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.vo.PortSet;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Obiecte de domeniu folosite în testele unitare (fără bază de date). */
public final class TestData {

    private TestData() {
    }

    public static Team team() {
        return new Team("Platform");
    }

    public static CloudResource database(Environment environment, boolean encrypted) {
        return new CloudResource("db-" + environment.name().toLowerCase(), "orders-db", ResourceType.DATABASE,
                environment, CloudProvider.AWS, new ResourceConfiguration(encrypted, false, PortSet.of(5432)),
                team());
    }

    public static SecurityPolicy encryptionPolicy(Severity severity) {
        RegulatoryFramework framework = new RegulatoryFramework("NIS2", "Directiva NIS2", ObligationLevel.MANDATORY);
        return framework.addPolicy(new SecurityPolicy("NIS2-ENC-001", "Baze de date criptate", severity,
                ResourceType.DATABASE, ResourceConfiguration.ENCRYPTION_ENABLED, "true"));
    }
}
