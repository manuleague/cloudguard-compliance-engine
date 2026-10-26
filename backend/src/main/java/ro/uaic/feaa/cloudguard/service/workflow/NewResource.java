package ro.uaic.feaa.cloudguard.service.workflow;

import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/** Datele introduse de administratorul cloud pentru o resursă nouă (US-01). */
public record NewResource(String externalId,
                          String name,
                          ResourceType type,
                          Environment environment,
                          CloudProvider provider,
                          ResourceConfiguration configuration) {
}
