package ro.uaic.feaa.cloudguard.service.workflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.uaic.feaa.cloudguard.domain.factory.CloudResourceFactory;
import ro.uaic.feaa.cloudguard.domain.factory.RegulatoryFrameworkFactory;
import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.AuditAction;
import ro.uaic.feaa.cloudguard.domain.model.BusinessRuleException;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ObligationLevel;
import ro.uaic.feaa.cloudguard.domain.model.RegulatoryFramework;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.repository.CloudResourceRepository;
import ro.uaic.feaa.cloudguard.domain.repository.RegulatoryFrameworkRepository;
import ro.uaic.feaa.cloudguard.domain.repository.SecurityPolicyRepository;
import ro.uaic.feaa.cloudguard.service.audit.AuditService;
import ro.uaic.feaa.cloudguard.service.validation.ValidationService;

@Service
@Transactional
public class ResourceWorkflowServiceImpl implements ResourceWorkflowService {

    private final CloudResourceFactory resourceFactory;
    private final RegulatoryFrameworkFactory frameworkFactory;
    private final CloudResourceRepository resourceRepository;
    private final RegulatoryFrameworkRepository frameworkRepository;
    private final SecurityPolicyRepository policyRepository;
    private final ValidationService validationService;
    private final AuditService auditService;

    public ResourceWorkflowServiceImpl(CloudResourceFactory resourceFactory,
                                       RegulatoryFrameworkFactory frameworkFactory,
                                       CloudResourceRepository resourceRepository,
                                       RegulatoryFrameworkRepository frameworkRepository,
                                       SecurityPolicyRepository policyRepository,
                                       ValidationService validationService,
                                       AuditService auditService) {
        this.resourceFactory = resourceFactory;
        this.frameworkFactory = frameworkFactory;
        this.resourceRepository = resourceRepository;
        this.frameworkRepository = frameworkRepository;
        this.policyRepository = policyRepository;
        this.validationService = validationService;
        this.auditService = auditService;
    }

    @Override
    public CloudResource registerResource(AppUser owner, NewResource data) {
        validationService.checkRole(owner, UserRole.RESOURCE_OWNER);
        if (owner.getTeam() == null) {
            throw new BusinessRuleException("Utilizatorul nu face parte dintr-o echipă");
        }
        CloudResource resource = resourceFactory.create(data.externalId(), data.name(), data.type(),
                data.environment(), data.provider(), data.configuration(), owner.getTeam());
        CloudResource saved = resourceRepository.save(resource);
        auditService.record(owner, AuditAction.RESOURCE_CREATED, "CloudResource", saved.getId());
        return saved;
    }

    @Override
    public CloudResource updateResource(AppUser owner, NewResource data) {
        validationService.checkRole(owner, UserRole.RESOURCE_OWNER);
        CloudResource resource = findResource(data.externalId());
        validationService.checkTeamOwnership(owner, resource);
        resource.update(data.name() == null ? null : data.name().trim(), data.type(), data.environment(),
                data.provider(), data.configuration());
        validationService.validate(resource);
        auditService.record(owner, AuditAction.RESOURCE_UPDATED, "CloudResource", resource.getId());
        return resource;
    }

    @Override
    public void deleteResource(AppUser owner, String externalId) {
        validationService.checkRole(owner, UserRole.RESOURCE_OWNER);
        CloudResource resource = findResource(externalId);
        validationService.checkTeamOwnership(owner, resource);
        if (resource.hasViolations()) {
            throw new BusinessRuleException("Resursa " + resource.getName()
                    + " are încălcări înregistrate și nu poate fi ștearsă");
        }
        Long id = resource.getId();
        resource.detachFromTeam();
        resourceRepository.delete(resource);
        auditService.record(owner, AuditAction.RESOURCE_DELETED, "CloudResource", id);
    }

    @Override
    public RegulatoryFramework defineFramework(AppUser officer, String code, String name, ObligationLevel level) {
        validationService.checkRole(officer, UserRole.SECURITY_OFFICER);
        RegulatoryFramework framework = frameworkFactory.createFramework(code, name, level);
        if (frameworkRepository.existsByCode(framework.getCode())) {
            throw new BusinessRuleException("Există deja un cadru cu codul " + framework.getCode());
        }
        RegulatoryFramework saved = frameworkRepository.save(framework);
        auditService.record(officer, AuditAction.FRAMEWORK_CREATED, "RegulatoryFramework", saved.getId());
        return saved;
    }

    @Override
    public SecurityPolicy definePolicy(AppUser officer, String frameworkCode, NewPolicy data) {
        validationService.checkRole(officer, UserRole.SECURITY_OFFICER);
        RegulatoryFramework framework = frameworkRepository.findByCode(frameworkCode)
                .orElseThrow(() -> new NotFoundException("Cadrul " + frameworkCode + " nu există"));
        frameworkFactory.createPolicy(framework, data.code(), data.title(), data.baseSeverity(),
                data.applicableType(), data.ruleAttribute(), data.expectedValue());
        frameworkRepository.saveAndFlush(framework);
        SecurityPolicy saved = policyRepository.findByCode(data.code()).orElseThrow();
        auditService.record(officer, AuditAction.POLICY_CREATED, "SecurityPolicy", saved.getId());
        return saved;
    }

    @Override
    public CloudResource assignPolicy(AppUser officer, String resourceExternalId, String policyCode) {
        validationService.checkRole(officer, UserRole.SECURITY_OFFICER);
        CloudResource resource = findResource(resourceExternalId);
        SecurityPolicy policy = policyRepository.findByCode(policyCode)
                .orElseThrow(() -> new NotFoundException("Politica " + policyCode + " nu există"));
        resource.assignPolicy(policy);
        auditService.record(officer, AuditAction.POLICY_ASSIGNED, "CloudResource", resource.getId());
        return resource;
    }

    private CloudResource findResource(String externalId) {
        return resourceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NotFoundException("Resursa " + externalId + " nu există"));
    }
}
