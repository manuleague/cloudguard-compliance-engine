package ro.uaic.feaa.cloudguard.domain.factory;

import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Factory pentru agregatul „Resursă cloud”: creează doar resurse valide. */
public interface CloudResourceFactory {

    CloudResource create(String externalId, String name, ResourceType type, Environment environment,
                         CloudProvider provider, ResourceConfiguration configuration, Team team);
}
