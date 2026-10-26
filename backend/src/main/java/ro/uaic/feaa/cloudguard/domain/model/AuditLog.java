package ro.uaic.feaa.cloudguard.domain.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entitate de suport: o acțiune importantă din aplicație.
 * Este imutabilă (NFR-02): nu are setteri, iar Hibernate nu emite UPDATE pentru ea.
 */
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Numele acțiunii, ex. VIOLATION_RESOLVED (valorile posibile sunt în AuditAction). */
    @NotBlank
    @Column(nullable = false, length = 40)
    private String action;

    @NotBlank
    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    protected AuditLog() {
        // pentru JPA
    }

    public AuditLog(AppUser user, AuditAction action, String entityType, Long entityId,
                    String ipAddress, LocalDateTime occurredAt) {
        this.user = user;
        this.action = action.name();
        this.entityType = entityType;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public AppUser getUser() {
        return user;
    }
}
