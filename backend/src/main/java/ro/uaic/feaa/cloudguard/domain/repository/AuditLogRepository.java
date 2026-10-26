package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import ro.uaic.feaa.cloudguard.domain.model.AuditLog;

/**
 * Repository restrâns intenționat: permite doar adăugare și citire.
 * Nu expune update sau delete, deci jurnalul nu poate fi modificat din aplicație (NFR-02).
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog entry);

    List<AuditLog> findAllByOrderByOccurredAtDesc();

    List<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, Long entityId);
}
