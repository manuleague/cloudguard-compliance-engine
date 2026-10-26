package ro.uaic.feaa.cloudguard.domain.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.uaic.feaa.cloudguard.domain.model.vo.ResourceConfiguration;

/**
 * Root entity pentru agregatul „Resursă cloud”.
 * Componentă: value object-ul ResourceConfiguration.
 */
@Entity
@Table(name = "cloud_resource")
public class CloudResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** V2: obligatoriu și unic (unicitatea e garantată de constrângerea din baza de date). */
    @NotBlank(message = "ID-ul extern al resursei este obligatoriu")
    @Column(name = "external_id", nullable = false, unique = true, length = 255)
    private String externalId;

    /** V1 */
    @NotBlank(message = "Numele resursei este obligatoriu")
    @Size(min = 3, max = 100, message = "Numele resursei trebuie să aibă între 3 și 100 de caractere")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Tipul resursei este obligatoriu")
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @NotNull(message = "Mediul resursei este obligatoriu")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Environment environment;

    @NotNull(message = "Furnizorul cloud este obligatoriu")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CloudProvider provider;

    @Valid
    @NotNull(message = "Configurația resursei este obligatorie")
    @Embedded
    private ResourceConfiguration configuration;

    @NotNull(message = "Resursa trebuie să aparțină unei echipe")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToMany
    @JoinTable(name = "resource_policy",
            joinColumns = @JoinColumn(name = "resource_id"),
            inverseJoinColumns = @JoinColumn(name = "policy_id"))
    private Set<SecurityPolicy> policies = new LinkedHashSet<>();

    /** Încălcările resursei (partea inversă a relației ComplianceViolation -> CloudResource). */
    @OneToMany(mappedBy = "resource")
    private List<ComplianceViolation> violations = new ArrayList<>();

    protected CloudResource() {
        // pentru JPA
    }

    public CloudResource(String externalId, String name, ResourceType resourceType,
                         Environment environment, CloudProvider provider,
                         ResourceConfiguration configuration, Team team) {
        this.externalId = externalId;
        this.name = name;
        this.resourceType = resourceType;
        this.environment = environment;
        this.provider = provider;
        this.configuration = configuration;
        this.team = team;
        if (team != null) {
            team.addResource(this);
        }
    }

    public void assignPolicy(SecurityPolicy policy) {
        policies.add(policy);
    }

    public void removePolicy(SecurityPolicy policy) {
        policies.remove(policy);
    }

    /** FR-02: modificarea datelor resursei; externalId și echipa rămân aceleași. */
    public void update(String name, ResourceType resourceType, Environment environment,
                       CloudProvider provider, ResourceConfiguration configuration) {
        this.name = name;
        this.resourceType = resourceType;
        this.environment = environment;
        this.provider = provider;
        this.configuration = configuration;
    }

    public boolean isProduction() {
        return environment == Environment.PRODUCTION;
    }

    /** C4 (câmp calculat): 100 minus penalizările încălcărilor active, minimum 0. */
    public int complianceScore() {
        return scoreFor(violations);
    }

    /** Formula C4, aplicată pe orice listă de încălcări. */
    public static int scoreFor(Collection<ComplianceViolation> violations) {
        int penalty = violations.stream()
                .filter(ComplianceViolation::isActive)
                .mapToInt(v -> v.getSeverity().getScorePenalty())
                .sum();
        return Math.max(0, 100 - penalty);
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    /** Desprinde resursa de echipă înainte de ștergere. */
    public void detachFromTeam() {
        team.removeResource(this);
    }

    void addViolation(ComplianceViolation violation) {
        violations.add(violation);
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public ResourceConfiguration getConfiguration() {
        return configuration;
    }

    public Team getTeam() {
        return team;
    }

    public List<ComplianceViolation> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    public Set<SecurityPolicy> getPolicies() {
        return Collections.unmodifiableSet(policies);
    }
}
