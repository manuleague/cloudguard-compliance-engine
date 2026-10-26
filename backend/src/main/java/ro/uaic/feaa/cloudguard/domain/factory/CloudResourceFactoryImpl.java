package ro.uaic.feaa.cloudguard.domain.factory;

import org.springframework.stereotype.Component;

import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;
import ro.uaic.feaa.cloudguard.service.validation.ValidationService;

@Component
public class CloudResourceFactoryImpl implements CloudResourceFactory {

    private final ValidationService validationService;

    public CloudResourceFactoryImpl(ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public CloudResource create(String externalId, String name, ResourceType type, Environment environment,
                                CloudProvider provider, ResourceConfiguration configuration, Team team) {
        CloudResource resource = new CloudResource(trim(externalId), trim(name), type, environment, provider,
                configuration, team);
        validationService.validate(resource);
        validationService.checkUniqueExternalId(resource.getExternalId());
        return resource;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
