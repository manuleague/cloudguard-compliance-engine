package ro.uaic.feaa.cloudguard.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
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
 * Root entity pentru agregatul „Încălcare de conformitate”.
 * Componente: RiskException (entitate) și Justification (value object).
 * Toate schimbările de status trec prin metodele de aici (regula A2).
 */
@Entity
@Table(name = "compliance_violation")
public class ComplianceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private CloudResource resource;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private SecurityPolicy policy;

    /** C1, calculată la creare. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ViolationStatus status;

    @NotNull
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    /** C2, calculată la creare. */
    @NotNull
    @Column(name = "sla_due_at", nullable = false)
    private LocalDateTime slaDueAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Valid
    @Embedded
    @AttributeOverride(name = "text", column = @Column(name = "resolution_text", length = 1000))
    private Justification resolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private AppUser responsible;

    @Valid
    @OneToOne(mappedBy = "violation", cascade = CascadeType.ALL, orphanRemoval = true)
    private RiskException riskException;

    protected ComplianceViolation() {
        // pentru JPA
    }

    public ComplianceViolation(CloudResource resource, SecurityPolicy policy, Severity severity,
                               LocalDateTime detectedAt, LocalDateTime slaDueAt) {
        this.resource = resource;
        this.policy = policy;
        this.severity = severity;
        this.detectedAt = detectedAt;
        this.slaDueAt = slaDueAt;
        this.status = ViolationStatus.OPEN;
    }

    /** OPEN -> UNDER_REVIEW: utilizatorul preia încălcarea. */
    public void startReview(AppUser user) {
        moveTo(ViolationStatus.UNDER_REVIEW);
        this.responsible = user;
    }

    /** UNDER_REVIEW -> RESOLVED, doar cu justificare. */
    public void resolve(AppUser user, Justification justification, LocalDateTime when) {
        requireJustification(justification);
        if (hasPendingException()) {
            throw new BusinessRuleException("Încălcarea are o excepție de risc în curs de aprobare");
        }
        moveTo(ViolationStatus.RESOLVED);
        this.resolution = justification;
        this.resolvedAt = when;
        this.responsible = user;
    }

    /** Cererea unei excepții de risc; o încălcare are cel mult o excepție. */
    public RiskException requestException(AppUser requester, Justification justification, LocalDate validUntil) {
        requireJustification(justification);
        if (status != ViolationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("Excepția se poate cere doar pentru o încălcare UNDER_REVIEW");
        }
        if (riskException != null) {
            throw new BusinessRuleException("Pentru această încălcare a fost deja cerută o excepție de risc");
        }
        this.riskException = new RiskException(this, requester, justification, validUntil);
        return riskException;
    }

    /** Aprobarea excepției: UNDER_REVIEW -> RISK_ACCEPTED (regula A3 se verifică în RiskException). */
    public void approveException(AppUser officer, LocalDateTime when) {
        requirePendingException();
        riskException.approve(officer, when);
        moveTo(ViolationStatus.RISK_ACCEPTED);
        this.resolvedAt = when;
    }

    /** Respingerea excepției: încălcarea rămâne UNDER_REVIEW și trebuie rezolvată. */
    public void rejectException(AppUser officer, LocalDateTime when) {
        requirePendingException();
        riskException.reject(officer, when);
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean hasPendingException() {
        return riskException != null && riskException.getStatus() == ExceptionStatus.PENDING;
    }

    private void moveTo(ViolationStatus target) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleException("Statusul nu se poate schimba din " + status + " în " + target);
        }
        this.status = target;
    }

    private void requireJustification(Justification justification) {
        if (justification == null || justification.getText() == null || justification.getText().isBlank()) {
            throw new BusinessRuleException("Schimbarea statusului necesită o justificare");
        }
    }

    private void requirePendingException() {
        if (!hasPendingException()) {
            throw new BusinessRuleException("Nu există o excepție de risc în așteptare");
        }
    }

    public Long getId() {
        return id;
    }

    public CloudResource getResource() {
        return resource;
    }

    public SecurityPolicy getPolicy() {
        return policy;
    }

    public Severity getSeverity() {
        return severity;
    }

    public ViolationStatus getStatus() {
        return status;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public LocalDateTime getSlaDueAt() {
        return slaDueAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public Justification getResolution() {
        return resolution;
    }

    public AppUser getResponsible() {
        return responsible;
    }

    public RiskException getRiskException() {
        return riskException;
    }
}
