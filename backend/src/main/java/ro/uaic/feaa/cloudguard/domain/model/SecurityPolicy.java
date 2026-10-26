package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Component entity în agregatul „Cadru de conformitate”: o regulă tehnică,
 * ex. encryptionEnabled = true pentru bazele de date.
 */
@Entity
@Table(name = "security_policy")
public class SecurityPolicy {

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

    @NotBlank(message = "Atributul verificat este obligatoriu")
    @Pattern(regexp = "encryptionEnabled|publicAccess|openPorts",
            message = "Atributul verificat trebuie să fie encryptionEnabled, publicAccess sau openPorts")
    @Column(name = "rule_attribute", nullable = false, length = 50)
    private String ruleAttribute;

    @NotBlank(message = "Valoarea așteptată este obligatorie")
    @Column(name = "expected_value", nullable = false, length = 100)
    private String expectedValue;

    protected SecurityPolicy() {
        // pentru JPA
    }

    public SecurityPolicy(String code, String title, Severity baseSeverity, ResourceType applicableType,
                          String ruleAttribute, String expectedValue) {
        this.code = code;
        this.title = title;
        this.baseSeverity = baseSeverity;
        this.applicableType = applicableType;
        this.ruleAttribute = ruleAttribute;
        this.expectedValue = expectedValue;
    }

    void attachTo(RegulatoryFramework framework) {
        this.framework = framework;
    }

    public boolean appliesTo(CloudResource resource) {
        return applicableType == resource.getResourceType();
    }

    /** Adevărat dacă resursa respectă politica (sau politica nu se aplică tipului ei de resursă). */
    public boolean evaluate(CloudResource resource) {
        return !appliesTo(resource) || resource.getConfiguration().satisfies(ruleAttribute, expectedValue);
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

    public String getRuleAttribute() {
        return ruleAttribute;
    }

    public String getExpectedValue() {
        return expectedValue;
    }
}
