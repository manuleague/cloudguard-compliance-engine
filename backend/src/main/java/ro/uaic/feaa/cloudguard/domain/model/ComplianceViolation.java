package ro.uaic.feaa.cloudguard.domain.model;

import java.time.Duration;
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
 * Toate schimbările de status trec prin metodele agregatului (regula A2).
 */
@Entity
@Table(name = "compliance_violation")
public class ComplianceViolation {

    /** Valabilitatea implicită a unei excepții de risc, dacă nu se dă o dată. */
    public static final int DEFAULT_EXCEPTION_DAYS = 90;

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

    /** Responsabilul încălcării (relația „responsabil” din diagrama de clase). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    /** C1, calculată la creare. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
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
    @AttributeOverride(name = "text", column = @Column(name = "resolution_note", length = 1000))
    private Justification resolution;

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
        if (resource != null) {
            resource.addViolation(this);
        }
    }

    /** OPEN -> UNDER_REVIEW: utilizatorul preia încălcarea și devine responsabil. */
    public void startReview(AppUser u) {
        moveTo(ViolationStatus.UNDER_REVIEW);
        this.assignee = u;
    }

    /** UNDER_REVIEW -> RESOLVED, doar cu justificare. */
    public void resolve(AppUser u, Justification j) {
        resolve(u, j, LocalDateTime.now());
    }

    /** Varianta cu momentul rezolvării dat explicit (serviciile folosesc ceasul aplicației). */
    public void resolve(AppUser u, Justification j, LocalDateTime when) {
        requireJustification(j);
        if (hasPendingException()) {
            throw new BusinessRuleException("Încălcarea are o excepție de risc în curs de aprobare");
        }
        moveTo(ViolationStatus.RESOLVED);
        this.resolution = j;
        this.resolvedAt = when;
        this.assignee = u;
    }

    /** Cererea unei excepții de risc, valabilă implicit 90 de zile. */
    public RiskException requestException(AppUser u, Justification j) {
        return requestException(u, j, LocalDate.now().plusDays(DEFAULT_EXCEPTION_DAYS));
    }

    /** Cererea unei excepții de risc; o încălcare are cel mult o excepție (one-to-one). */
    public RiskException requestException(AppUser u, Justification j, LocalDate validUntil) {
        requireJustification(j);
        if (status != ViolationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("Excepția se poate cere doar pentru o încălcare UNDER_REVIEW");
        }
        if (riskException != null) {
            throw new BusinessRuleException("Pentru această încălcare a fost deja cerută o excepție de risc");
        }
        this.assignee = u;
        this.riskException = new RiskException(this, j, validUntil);
        return riskException;
    }

    /** C3 (câmp calculat): orele dintre detectare și rezolvare; 0 dacă nu e rezolvată. */
    public long timeToResolveHours() {
        return resolvedAt == null ? 0 : Duration.between(detectedAt, resolvedAt).toHours();
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean hasPendingException() {
        return riskException != null && riskException.getStatus() == ExceptionStatus.PENDING;
    }

    /** Apelată de RiskException.approve: UNDER_REVIEW -> RISK_ACCEPTED. */
    void acceptRisk() {
        moveTo(ViolationStatus.RISK_ACCEPTED);
    }

    private void moveTo(ViolationStatus target) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleException("Statusul nu se poate schimba din " + status + " în " + target);
        }
        this.status = target;
    }

    private void requireJustification(Justification j) {
        if (j == null || j.getText() == null || j.getText().isBlank()) {
            throw new BusinessRuleException("Schimbarea statusului necesită o justificare");
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

    public AppUser getAssignee() {
        return assignee;
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

    public RiskException getRiskException() {
        return riskException;
    }
}
