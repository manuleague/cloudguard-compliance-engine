package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Politică despre accesul public: publicAccess trebuie să aibă valoarea așteptată (true/false). */
@Entity
@DiscriminatorValue(ResourceConfiguration.PUBLIC_ACCESS)
public class PublicAccessPolicy extends SecurityPolicy {

    protected PublicAccessPolicy() {
        // pentru JPA
    }

    public PublicAccessPolicy(String code, String title, Severity baseSeverity, ResourceType applicableType,
                              String expectedValue) {
        super(code, title, baseSeverity, applicableType, expectedValue);
    }

    @Override
    public String getRuleAttribute() {
        return ResourceConfiguration.PUBLIC_ACCESS;
    }

    @Override
    protected boolean isSatisfiedBy(ResourceConfiguration configuration) {
        return configuration.isPublicAccess() == Boolean.parseBoolean(getExpectedValue());
    }
}
