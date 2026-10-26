package ro.uaic.feaa.cloudguard.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Root entity pentru agregatul „Cadru de conformitate”
 * (lege, standard sau regulament intern). Componentă: SecurityPolicy.
 */
@Entity
@Table(name = "regulatory_framework")
public class RegulatoryFramework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Codul cadrului este obligatoriu")
    @Size(max = 20, message = "Codul cadrului poate avea maximum 20 de caractere")
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank(message = "Denumirea cadrului este obligatorie")
    @Column(nullable = false, length = 150)
    private String name;

    @NotNull(message = "Nivelul de obligativitate este obligatoriu")
    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_level", nullable = false, length = 20)
    private ObligationLevel obligationLevel;

    @Valid
    @OneToMany(mappedBy = "framework", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SecurityPolicy> policies = new ArrayList<>();

    protected RegulatoryFramework() {
        // pentru JPA
    }

    public RegulatoryFramework(String code, String name, ObligationLevel obligationLevel) {
        this.code = code;
        this.name = name;
        this.obligationLevel = obligationLevel;
    }

    /** Politicile se adaugă doar prin root, ca să nu existe două politici cu același cod. */
    public void addPolicy(SecurityPolicy policy) {
        boolean duplicate = policies.stream().anyMatch(p -> p.getCode().equals(policy.getCode()));
        if (duplicate) {
            throw new BusinessRuleException("Politica " + policy.getCode() + " există deja în cadrul " + code);
        }
        policy.attachTo(this);
        policies.add(policy);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public ObligationLevel getObligationLevel() {
        return obligationLevel;
    }

    public List<SecurityPolicy> getPolicies() {
        return Collections.unmodifiableList(policies);
    }
}
