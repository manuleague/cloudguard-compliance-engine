package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/**
 * Component entity în agregatul „Cadru de conformitate”: o regulă tehnică,
 * ex. encryptionEnabled = true pentru bazele de date.
 *
 * <p>Sprint 3: clasa este abstractă și are câte o subclasă pentru fiecare atribut verificat
 * (moștenire SINGLE_TABLE). Coloana existentă rule_attribute este acum coloana discriminator,
 * deci schema din diagrama SQL nu se schimbă.
 */
@Entity
@Table(name = "security_policy")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rule_attribute", discriminatorType = DiscriminatorType.STRING, length = 50)
public abstract class SecurityPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "framework_id", nullable = false)
    private RegulatoryFramework framework;

    /** V3 */
    @NotBlank(message = "Codul politicii este obligatoriu")
    @Pattern(regexp = "^[A-Z0-9]+-[A-Z]+-\\d{3}$",
            message = "Codul politicii trebuie să aibă formatul CADRU-DOMENIU-NNN (ex. NIS2-ENC-001)")
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @NotBlank(message = "Titlul politicii este obligatoriu")
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull(message = "Severitatea de bază este obligatorie")
    @Enumerated(EnumType.STRING)
    @Column(name = "base_severity", nullable = false, length = 10)
    private Severity baseSeverity;

    /** Tipul de resursă căruia i se aplică politica. */
    @NotNull(message = "Tipul de resursă vizat este obligatoriu")
    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_type", nullable = false, length = 30)
    private ResourceType applicableType;

    @NotBlank(message = "Valoarea așteptată este obligatorie")
    @Column(name = "expected_value", nullable = false, length = 100)
    private String expectedValue;

    protected SecurityPolicy() {
        // pentru JPA
    }

    protected SecurityPolicy(String code, String title, Severity baseSeverity, ResourceType applicableType,
                             String expectedValue) {
        this.code = code;
        this.title = title;
        this.baseSeverity = baseSeverity;
        this.applicableType = applicableType;
        this.expectedValue = expectedValue;
    }

    /**
     * Creează subclasa potrivită atributului verificat.
     *
     * @throws IllegalArgumentException dacă atributul nu este unul dintre cele trei cunoscute
     */
    public static SecurityPolicy create(String code, String title, Severity baseSeverity,
                                        ResourceType applicableType, String ruleAttribute,
                                        String expectedValue) {
        if (ruleAttribute == null) {
            throw new IllegalArgumentException("Atributul verificat este obligatoriu");
        }
        return switch (ruleAttribute) {
            case ResourceConfiguration.ENCRYPTION_ENABLED ->
                    new EncryptionPolicy(code, title, baseSeverity, applicableType, expectedValue);
            case ResourceConfiguration.PUBLIC_ACCESS ->
                    new PublicAccessPolicy(code, title, baseSeverity, applicableType, expectedValue);
            case ResourceConfiguration.OPEN_PORTS ->
                    new OpenPortsPolicy(code, title, baseSeverity, applicableType, expectedValue);
            default -> throw new IllegalArgumentException(
                    "Atributul verificat trebuie să fie encryptionEnabled, publicAccess sau openPorts");
        };
    }

    /** Atributul din ResourceConfiguration pe care îl verifică politica (valoarea discriminatorului). */
    public abstract String getRuleAttribute();

    /** Adevărat dacă configurația respectă valoarea așteptată de politică. */
    protected abstract boolean isSatisfiedBy(ResourceConfiguration configuration);

    void attachTo(RegulatoryFramework framework) {
        this.framework = framework;
    }

    public boolean appliesTo(CloudResource resource) {
        return applicableType == resource.getResourceType();
    }

    /** Adevărat dacă resursa respectă politica (sau politica nu se aplică tipului ei de resursă). */
    public boolean evaluate(CloudResource resource) {
        return !appliesTo(resource) || isSatisfiedBy(resource.getConfiguration());
    }

    public Long getId() {
        return id;
    }

    public RegulatoryFramework getFramework() {
        return framework;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public Severity getBaseSeverity() {
        return baseSeverity;
    }

    public ResourceType getApplicableType() {
        return applicableType;
    }

    public String getExpectedValue() {
        return expectedValue;
    }
}
