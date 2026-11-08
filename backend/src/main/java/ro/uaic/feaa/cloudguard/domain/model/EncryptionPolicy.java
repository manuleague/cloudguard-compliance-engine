package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Politică despre criptare: encryptionEnabled trebuie să aibă valoarea așteptată (true/false). */
@Entity
@DiscriminatorValue(ResourceConfiguration.ENCRYPTION_ENABLED)
public class EncryptionPolicy extends SecurityPolicy {

    protected EncryptionPolicy() {
        // pentru JPA
    }

    public EncryptionPolicy(String code, String title, Severity baseSeverity, ResourceType applicableType,
                            String expectedValue) {
        super(code, title, baseSeverity, applicableType, expectedValue);
    }

    @Override
    public String getRuleAttribute() {
        return ResourceConfiguration.ENCRYPTION_ENABLED;
    }

    @Override
    protected boolean isSatisfiedBy(ResourceConfiguration configuration) {
        return configuration.isEncryptionEnabled() == Boolean.parseBoolean(getExpectedValue());
    }
}
