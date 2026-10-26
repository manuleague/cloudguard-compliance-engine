package ro.uaic.feaa.cloudguard.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import ro.uaic.feaa.cloudguard.domain.model.vo.Justification;

/**
 * Component entity în agregatul „Încălcare”: excepția de risc cerută
 * pentru o încălcare. Se creează doar prin ComplianceViolation.requestException.
 */
@Entity
@Table(name = "risk_exception")
public class RiskException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One-to-one: cheie străină unică spre încălcare. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "violation_id", nullable = false, unique = true)
    private ComplianceViolation violation;

    @Valid
    @NotNull
    @Embedded
    private Justification justification;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExceptionStatus status;

    @NotNull(message = "Data de expirare a excepției este obligatorie")
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private AppUser requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private AppUser decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    protected RiskException() {
        // pentru JPA
    }

    RiskException(ComplianceViolation violation, AppUser requestedBy,
                  Justification justification, LocalDate validUntil) {
        this.violation = violation;
        this.requestedBy = requestedBy;
        this.justification = justification;
        this.validUntil = validUntil;
        this.status = ExceptionStatus.PENDING;
    }

    /** A3: aprobă doar un SECURITY_OFFICER, diferit de cel care a cerut excepția. */
    void approve(AppUser officer, LocalDateTime when) {
        checkDecider(officer);
        this.status = ExceptionStatus.APPROVED;
        this.decidedBy = officer;
        this.decidedAt = when;
    }

    void reject(AppUser officer, LocalDateTime when) {
        checkDecider(officer);
        this.status = ExceptionStatus.REJECTED;
        this.decidedBy = officer;
        this.decidedAt = when;
    }

    private void checkDecider(AppUser officer) {
        if (officer == null || !officer.hasRole(UserRole.SECURITY_OFFICER)) {
            throw new BusinessRuleException("Doar un ofițer de securitate poate decide asupra unei excepții de risc");
        }
        if (isSameUser(officer, requestedBy)) {
            throw new BusinessRuleException("Excepția trebuie aprobată de alt utilizator decât cel care a cerut-o");
        }
    }

    private static boolean isSameUser(AppUser a, AppUser b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getId() != null && Objects.equals(a.getId(), b.getId());
    }

    public Long getId() {
        return id;
    }

    public ComplianceViolation getViolation() {
        return violation;
    }

    public Justification getJustification() {
        return justification;
    }

    public ExceptionStatus getStatus() {
        return status;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public AppUser getRequestedBy() {
        return requestedBy;
    }

    public AppUser getDecidedBy() {
        return decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }
}
