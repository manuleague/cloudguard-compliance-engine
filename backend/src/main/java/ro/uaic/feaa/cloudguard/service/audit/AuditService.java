package ro.uaic.feaa.cloudguard.service.audit;

import java.util.List;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.AuditAction;
import ro.uaic.feaa.cloudguard.domain.model.AuditLog;

/** Serviciul de audit (FR-08): salvează acțiunile importante și le oferă auditorului. */
public interface AuditService {

    AuditLog record(AppUser actor, AuditAction action, String entityType, Long entityId);

    /** US-05: doar AUDITOR și SECURITY_OFFICER pot citi jurnalul. */
    List<AuditLog> findAll(AppUser reader);

    List<AuditLog> historyOf(AppUser reader, String entityType, Long entityId);
}
