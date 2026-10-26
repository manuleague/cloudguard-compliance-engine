package ro.uaic.feaa.cloudguard.domain.model;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
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
 * Component entity în agregatul „Încălcare”: excepția de risc cerută pentru o încălcare.
 * Se creează doar prin ComplianceViolation.requestException; solicitantul este
 * responsabilul încălcării (assignee).
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

    /** Ofițerul care a decis asupra excepției (relația „aprobat de”). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Valid
    @NotNull
    @Embedded
    @AttributeOverride(name = "text", column = @Column(name = "justification", nullable = false, length = 1000))
    private Justification justification;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExceptionStatus status;

    @NotNull(message = "Data de expirare a excepției este obligatorie")
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    protected RiskException() {
        // pentru JPA
    }

    RiskException(ComplianceViolation violation, Justification justification, LocalDate validUntil) {
        this.violation = violation;
        this.justification = justification;
        this.validUntil = validUntil;
        this.status = ExceptionStatus.PENDING;
    }

    /** Aprobare: încălcarea devine RISK_ACCEPTED (regula A3). */
    public void approve(AppUser officer) {
        checkDecider(officer);
        this.status = ExceptionStatus.APPROVED;
        this.approvedBy = officer;
        violation.acceptRisk();
    }

    /** Respingere: încălcarea rămâne UNDER_REVIEW și trebuie rezolvată. */
    public void reject(AppUser officer) {
        checkDecider(officer);
        this.status = ExceptionStatus.REJECTED;
        this.approvedBy = officer;
    }

    /** A3: decide doar un SECURITY_OFFICER, diferit de cel care a cerut excepția. */
    private void checkDecider(AppUser officer) {
        if (status != ExceptionStatus.PENDING) {
            throw new BusinessRuleException("Excepția de risc a fost deja " + status);
        }
        if (officer == null || !officer.hasRole(UserRole.SECURITY_OFFICER)) {
            throw new BusinessRuleException("Doar un ofițer de securitate poate decide asupra unei excepții de risc");
        }
        if (isSameUser(officer, violation.getAssignee())) {
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

    public AppUser getApprovedBy() {
        return approvedBy;
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
}
