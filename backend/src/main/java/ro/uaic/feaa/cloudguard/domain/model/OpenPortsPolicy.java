package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import ro.uaic.feaa.cloudguard.domain.model.vo.PortSet;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Politică despre porturi: resursa poate avea deschise doar porturile din lista așteptată, ex. "443". */
@Entity
@DiscriminatorValue(ResourceConfiguration.OPEN_PORTS)
public class OpenPortsPolicy extends SecurityPolicy {

    protected OpenPortsPolicy() {
        // pentru JPA
    }

    public OpenPortsPolicy(String code, String title, Severity baseSeverity, ResourceType applicableType,
                           String expectedValue) {
        super(code, title, baseSeverity, applicableType, expectedValue);
    }

    @Override
    public String getRuleAttribute() {
        return ResourceConfiguration.OPEN_PORTS;
    }

    @Override
    protected boolean isSatisfiedBy(ResourceConfiguration configuration) {
        return configuration.getOpenPorts().isSubsetOf(PortSet.parse(getExpectedValue()));
    }
}
