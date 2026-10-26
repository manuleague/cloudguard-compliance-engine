package ro.uaic.feaa.cloudguard.service.validation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.BusinessRuleException;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;
import ro.uaic.feaa.cloudguard.domain.repository.CloudResourceRepository;
import ro.uaic.feaa.cloudguard.domain.repository.ComplianceViolationRepository;
import ro.uaic.feaa.cloudguard.domain.repository.SecurityPolicyRepository;

@Service
public class ValidationServiceImpl implements ValidationService {

    private static final List<ViolationStatus> ACTIVE =
            List.of(ViolationStatus.OPEN, ViolationStatus.UNDER_REVIEW);

    private final Validator validator;
    private final CloudResourceRepository resourceRepository;
    private final SecurityPolicyRepository policyRepository;
    private final ComplianceViolationRepository violationRepository;

    public ValidationServiceImpl(Validator validator,
                                 CloudResourceRepository resourceRepository,
                                 SecurityPolicyRepository policyRepository,
                                 ComplianceViolationRepository violationRepository) {
        this.validator = validator;
        this.resourceRepository = resourceRepository;
        this.policyRepository = policyRepository;
        this.violationRepository = violationRepository;
    }

    @Override
    public <T> T validate(T object) {
        Objects.requireNonNull(object, "Obiectul de validat lipsește");
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            List<String> messages = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .toList();
            throw new DomainValidationException(messages);
        }
        return object;
    }

    @Override
    public void checkUniqueExternalId(String externalId) {
        if (externalId != null && resourceRepository.existsByExternalId(externalId)) {
            throw new DomainValidationException(List.of("Există deja o resursă cu ID-ul extern " + externalId));
        }
    }

    @Override
    public void checkUniquePolicyCode(String policyCode) {
        if (policyCode != null && policyRepository.existsByCode(policyCode)) {
            throw new DomainValidationException(List.of("Există deja o politică cu codul " + policyCode));
        }
    }

    @Override
    public void checkNoActiveViolation(CloudResource resource, SecurityPolicy policy) {
        if (violationRepository.existsByResourceAndPolicyAndStatusIn(resource, policy, ACTIVE)) {
            throw new BusinessRuleException("Există deja o încălcare activă pentru resursa "
                    + resource.getName() + " și politica " + policy.getCode());
        }
    }

    @Override
    public void checkRole(AppUser user, UserRole... allowedRoles) {
        if (user == null || Arrays.stream(allowedRoles).noneMatch(user::hasRole)) {
            throw new BusinessRuleException("Utilizatorul nu are dreptul să facă această operație");
        }
    }

    @Override
    public void checkTeamOwnership(AppUser user, CloudResource resource) {
        if (user.hasRole(UserRole.RESOURCE_OWNER)
                && (user.getTeam() == null || !Objects.equals(user.getTeam().getId(), resource.getTeam().getId()))) {
            throw new BusinessRuleException("Resursa " + resource.getName() + " nu aparține echipei tale");
        }
    }
}
