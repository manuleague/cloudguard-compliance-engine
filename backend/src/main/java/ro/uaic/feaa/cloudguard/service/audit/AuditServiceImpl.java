package ro.uaic.feaa.cloudguard.service.audit;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.AuditAction;
import ro.uaic.feaa.cloudguard.domain.model.AuditLog;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.repository.AuditLogRepository;
import ro.uaic.feaa.cloudguard.service.validation.ValidationService;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ValidationService validationService;
    private final ClientIpProvider clientIpProvider;
    private final Clock clock;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, ValidationService validationService,
                            ClientIpProvider clientIpProvider, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.validationService = validationService;
        this.clientIpProvider = clientIpProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuditLog record(AppUser actor, AuditAction action, String entityType, Long entityId) {
        AuditLog entry = new AuditLog(actor, action, entityType, entityId,
                clientIpProvider.currentIp(), LocalDateTime.now(clock));
        return auditLogRepository.save(validationService.validate(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findAll(AppUser reader) {
        validationService.checkRole(reader, UserRole.AUDITOR, UserRole.SECURITY_OFFICER);
        return auditLogRepository.findAllByOrderByOccurredAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> historyOf(AppUser reader, String entityType, Long entityId) {
        validationService.checkRole(reader, UserRole.AUDITOR, UserRole.SECURITY_OFFICER);
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(entityType, entityId);
    }
}
