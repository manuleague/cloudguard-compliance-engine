package ro.uaic.feaa.cloudguard.service.workflow;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.uaic.feaa.cloudguard.domain.factory.ComplianceViolationFactory;
import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.AuditAction;
import ro.uaic.feaa.cloudguard.domain.model.CloudResource;
import ro.uaic.feaa.cloudguard.domain.model.ComplianceViolation;
import ro.uaic.feaa.cloudguard.domain.model.RiskException;
import ro.uaic.feaa.cloudguard.domain.model.SecurityPolicy;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;
import ro.uaic.feaa.cloudguard.domain.model.ViolationStatus;
import ro.uaic.feaa.cloudguard.domain.model.vo.Justification;
import ro.uaic.feaa.cloudguard.domain.repository.CloudResourceRepository;
import ro.uaic.feaa.cloudguard.domain.repository.ComplianceViolationRepository;
import ro.uaic.feaa.cloudguard.service.audit.AuditService;
import ro.uaic.feaa.cloudguard.service.validation.DomainValidationException;
import ro.uaic.feaa.cloudguard.service.validation.ValidationService;

@Service
@Transactional
public class ComplianceWorkflowServiceImpl implements ComplianceWorkflowService {

    private static final String VIOLATION = "ComplianceViolation";
    private static final List<ViolationStatus> ACTIVE =
            List.of(ViolationStatus.OPEN, ViolationStatus.UNDER_REVIEW);

    private final CloudResourceRepository resourceRepository;
    private final ComplianceViolationRepository violationRepository;
    private final ComplianceViolationFactory violationFactory;
    private final ValidationService validationService;
    private final AuditService auditService;
    private final Clock clock;

    public ComplianceWorkflowServiceImpl(CloudResourceRepository resourceRepository,
                                         ComplianceViolationRepository violationRepository,
                                         ComplianceViolationFactory violationFactory,
                                         ValidationService validationService,
                                         AuditService auditService,
                                         Clock clock) {
        this.resourceRepository = resourceRepository;
        this.violationRepository = violationRepository;
        this.violationFactory = violationFactory;
        this.validationService = validationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public AssessmentResult runAssessment(AppUser officer) {
        validationService.checkRole(officer, UserRole.SECURITY_OFFICER);
        LocalDateTime now = LocalDateTime.now(clock);
        List<CloudResource> resources = resourceRepository.findAll();
        List<ComplianceViolation> created = new ArrayList<>();
        int checks = 0;

        for (CloudResource resource : resources) {
            for (SecurityPolicy policy : resource.getPolicies()) {
                checks++;
                boolean compliant = policy.evaluate(resource);
                boolean alreadyActive = violationRepository.existsByResourceAndPolicyAndStatusIn(
                        resource, policy, ACTIVE);
                if (!compliant && !alreadyActive) {
                    ComplianceViolation violation = violationRepository.save(
                            violationFactory.create(resource, policy, now));
                    auditService.record(officer, AuditAction.VIOLATION_CREATED, VIOLATION, violation.getId());
                    created.add(violation);
                }
            }
        }
        auditService.record(officer, AuditAction.ASSESSMENT_COMPLETED, "Assessment", null);
        return new AssessmentResult(resources.size(), checks, created);
    }

    @Override
    public ComplianceViolation startReview(AppUser user, Long violationId) {
        validationService.checkRole(user, UserRole.RESOURCE_OWNER, UserRole.SECURITY_OFFICER);
        ComplianceViolation violation = load(violationId);
        validationService.checkTeamOwnership(user, violation.getResource());
        violation.startReview(user);
        auditService.record(user, AuditAction.VIOLATION_REVIEW_STARTED, VIOLATION, violationId);
        return violation;
    }

    @Override
    public ComplianceViolation resolve(AppUser owner, Long violationId, String justification) {
        validationService.checkRole(owner, UserRole.RESOURCE_OWNER);
        ComplianceViolation violation = load(violationId);
        validationService.checkTeamOwnership(owner, violation.getResource());
        Justification text = validationService.validate(Justification.of(justification));
        violation.resolve(owner, text, LocalDateTime.now(clock));
        auditService.record(owner, AuditAction.VIOLATION_RESOLVED, VIOLATION, violationId);
        return violation;
    }

    @Override
    public RiskException requestRiskException(AppUser owner, Long violationId, String justification,
                                              LocalDate validUntil) {
        validationService.checkRole(owner, UserRole.RESOURCE_OWNER);
        ComplianceViolation violation = load(violationId);
        validationService.checkTeamOwnership(owner, violation.getResource());
        Justification text = validationService.validate(Justification.of(justification));
        if (validUntil == null || !validUntil.isAfter(LocalDate.now(clock))) {
            throw new DomainValidationException(
                    List.of("Data de expirare a excepției trebuie să fie în viitor"));
        }
        RiskException exception = violation.requestException(owner, text, validUntil);
        violationRepository.saveAndFlush(violation);
        auditService.record(owner, AuditAction.EXCEPTION_REQUESTED, VIOLATION, violationId);
        return exception;
    }

    @Override
    public ComplianceViolation approveRiskException(AppUser officer, Long violationId) {
        validationService.checkRole(officer, UserRole.SECURITY_OFFICER);
        ComplianceViolation violation = load(violationId);
        violation.approveException(officer, LocalDateTime.now(clock));
        auditService.record(officer, AuditAction.EXCEPTION_APPROVED, VIOLATION, violationId);
        return violation;
    }

    @Override
    public ComplianceViolation rejectRiskException(AppUser officer, Long violationId) {
        validationService.checkRole(officer, UserRole.SECURITY_OFFICER);
        ComplianceViolation violation = load(violationId);
        violation.rejectException(officer, LocalDateTime.now(clock));
        auditService.record(officer, AuditAction.EXCEPTION_REJECTED, VIOLATION, violationId);
        return violation;
    }

    private ComplianceViolation load(Long violationId) {
        return violationRepository.findById(violationId)
                .orElseThrow(() -> new NotFoundException("Încălcarea #" + violationId + " nu există"));
    }
}
