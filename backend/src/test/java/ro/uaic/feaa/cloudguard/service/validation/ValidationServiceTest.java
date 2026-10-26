package ro.uaic.feaa.cloudguard.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.BusinessRuleException;
import ro.uaic.feaa.cloudguard.domain.model.CloudProvider;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.Environment;
import ro.uaic.feaa.cloudguard.domain.model.ResourceType;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.Severity;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.model.vo.Justification;
import ro.uaic.feaa.cloudguard.domain.model.vo.PortSet;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;
import ro.uaic.feaa.cloudguard.domain.repository.CloudResourceRepository;
import ro.uaic.feaa.cloudguard.domain.repository.ComplianceViolationRepository;
import ro.uaic.feaa.cloudguard.domain.repository.SecurityPolicyRepository;
import ro.uaic.feaa.cloudguard.support.TestData;

class ValidationServiceTest {

    private static ValidatorFactory validatorFactory;

    private CloudResourceRepository resourceRepository;
    private ComplianceViolationRepository violationRepository;
    private ValidationService validationService;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        resourceRepository = mock(CloudResourceRepository.class);
        violationRepository = mock(ComplianceViolationRepository.class);
        validationService = new ValidationServiceImpl(validatorFactory.getValidator(), resourceRepository,
                mock(SecurityPolicyRepository.class), violationRepository);
    }

    private CloudResource resourceNamed(String name) {
        return new CloudResource("ext-1", name, ResourceType.DATABASE, Environment.PRODUCTION, CloudProvider.AWS,
                new ResourceConfiguration(false, false, PortSet.empty()), TestData.team());
    }

    @Test
    void validResourcePasses() {
        CloudResource resource = resourceNamed("orders-db");
        assertThat(validationService.validate(resource)).isSameAs(resource);
    }

    @Test
    void v1_resourceWithoutNameIsRejected() {
        assertThatThrownBy(() -> validationService.validate(resourceNamed(null)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Numele resursei este obligatoriu");
    }

    @Test
    void v1_resourceNameTooShortIsRejected() {
        assertThatThrownBy(() -> validationService.validate(resourceNamed("db")))
                .hasMessageContaining("între 3 și 100 de caractere");
    }

    @Test
    void v2_duplicateExternalIdIsRejected() {
        when(resourceRepository.existsByExternalId("ext-1")).thenReturn(true);
        assertThatThrownBy(() -> validationService.checkUniqueExternalId("ext-1"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void v3_policyCodeMustFollowFormat() {
        SecurityPolicy policy = new SecurityPolicy("nis2-enc-1", "Criptare", Severity.HIGH, ResourceType.DATABASE,
                ResourceConfiguration.ENCRYPTION_ENABLED, "true");
        assertThatThrownBy(() -> validationService.validate(policy))
                .hasMessageContaining("CADRU-DOMENIU-NNN");
    }

    @Test
    void v4_justificationMustHaveTwentyCharacters() {
        assertThatThrownBy(() -> validationService.validate(Justification.of("gata")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Justificarea trebuie să aibă minimum 20 de caractere");
    }

    @Test
    void v5_emailMustBeValid() {
        AppUser user = new AppUser("emanuel", "nu-e-email", UserRole.RESOURCE_OWNER, null);
        assertThatThrownBy(() -> validationService.validate(user))
                .hasMessageContaining("Adresa de e-mail nu este validă");
    }

    @Test
    void a1_secondActiveViolationForSamePairIsRejected() {
        CloudResource resource = TestData.database(Environment.PRODUCTION, false);
        SecurityPolicy policy = TestData.encryptionPolicy(Severity.HIGH);
        when(violationRepository.existsByResourceAndPolicyAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> validationService.checkNoActiveViolation(resource, policy))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("încălcare activă");
    }

    @Test
    void roleCheckRejectsUserWithoutPermission() {
        AppUser auditor = new AppUser("auditor", "auditor@firma.ro", UserRole.AUDITOR, null);
        assertThatThrownBy(() -> validationService.checkRole(auditor, UserRole.SECURITY_OFFICER))
                .isInstanceOf(BusinessRuleException.class);
    }
}
