# CloudGuard & Compliance Engine

Aplicație web multi-tier care verifică dacă resursele cloud ale unei organizații respectă
politicile de securitate definite în cadre de conformitate (lege, standard sau regulament intern).

Stack: Java 17 · Spring Boot 3 · Spring Data JPA · PostgreSQL · React (frontend, în lucru).

## Structura

```
backend/    aplicația Spring Boot (domeniu, servicii de business, teste)
frontend/   interfața React (urmează)
docs/       documentele de sprint și diagramele
```

## Rulare

```bash
docker compose up -d          # pornește PostgreSQL pe localhost:5432
cd backend
mvn spring-boot:run           # pornește aplicația
mvn test                      # rulează testele (folosesc H2 în memorie, nu au nevoie de PostgreSQL)
```

## Sprint 2 — implementarea modelului de business

Pachetul de bază: `ro.uaic.feaa.cloudguard`

| Livrabil | Unde se află |
| --- | --- |
| Clasa de configurare Spring | `config/AppConfig` |
| Entități și relații | `domain/model` |
| Value objects (inclusiv enum-uri și tipul propriu `PortSet`) | `domain/model`, `domain/model/vo` |
| Servicii de suport: repository-uri | `domain/repository` |
| Servicii de suport: factory-uri pentru agregate | `domain/factory` |
| Servicii de workflow (use case) | `service/workflow` |
| Serviciu de validare | `service/validation` |
| Servicii de calcul (câmpuri și vederi) | `service/computation` |
| Serviciu de audit | `service/audit` |
| Teste JUnit | `src/test/java/...` |

### Agregate

| Agregat | Root entity | Componente |
| --- | --- | --- |
| Resursă cloud | `CloudResource` | `ResourceConfiguration` (VO), `PortSet` (VO custom data type) |
| Cadru de conformitate | `RegulatoryFramework` | `SecurityPolicy` (entitate) |
| Încălcare | `ComplianceViolation` | `RiskException` (entitate), `Justification` (VO) |

Entități de suport: `Team`, `AppUser`, `AuditLog`.

### Reguli de business → cod

| Regulă | Implementare |
| --- | --- |
| V1–V5 (validare pe câmpuri) | adnotări Jakarta Validation în entități și VO, rulate de `ValidationService.validate` |
| V2 unicitate externalId | `ValidationService.checkUniqueExternalId` + constrângere UNIQUE |
| A1 o singură încălcare activă | `ValidationService.checkNoActiveViolation`, apelată din `ComplianceViolationFactory` |
| A2 tranzițiile de status | metodele din `ComplianceViolation` + `ViolationStatus.canMoveTo` |
| A3 aprobarea excepției | `RiskException.approve` / `reject` (doar SECURITY_OFFICER, alt utilizator decât solicitantul) |
| C1 severitatea | `ComplianceComputationService.computeSeverity` |
| C2 termenul SLA | `ComplianceComputationService.computeSlaDueAt` |
| C3 timpul de rezolvare | câmpul calculat `ComplianceViolation.timeToResolveHours()` |
| C4 scorul de conformitate | câmpul calculat `CloudResource.complianceScore()` |
| C5 vederea pe echipă | `ComplianceViewService` → `TeamComplianceView` |

### Workflow-uri (user stories)

| User story | Metodă |
| --- | --- |
| US-01 adăugare resursă | `ResourceWorkflowService.registerResource` |
| FR-02 modificare / ștergere resursă | `ResourceWorkflowService.updateResource`, `deleteResource` |
| US-02 definire și asociere politică | `ResourceWorkflowService.definePolicy`, `assignPolicy` |
| US-03 evaluare de conformitate | `ComplianceWorkflowService.runAssessment` |
| US-04 rezolvare încălcare | `ComplianceWorkflowService.startReview`, `resolve` |
| FR-07 excepție de risc | `ComplianceWorkflowService.requestRiskException`, `approveRiskException`, `rejectRiskException` |
| US-05 jurnal de audit | `AuditService.findAll`, `historyOf` |

Maparea JPA folosește exact tabelele și coloanele din diagrama SQL a documentului Sprint 1 (`docs/`).

## Sprint 3 — modelul de persistență (JPA)

| Livrabil | Unde se află |
| --- | --- |
| Entități `@Entity` | `domain/model` |
| Moștenire `@Inheritance` (SINGLE_TABLE) | `SecurityPolicy` (abstractă) → `EncryptionPolicy`, `PublicAccessPolicy`, `OpenPortsPolicy`; coloana discriminator este `rule_attribute` |
| Value objects `@Enumerated`, `@Convert`, `@Embeddable` | enum-urile din `domain/model`; `PortSetConverter`, `ResourceConfiguration`, `Justification` în `domain/model/vo` |
| Relațiile agregatelor (`mappedBy`, `fetch`, `cascade = ALL`, `orphanRemoval`) | `RegulatoryFramework.policies`, `ComplianceViolation.riskException`; `removePolicy` demonstrează `orphanRemoval` |
| Repository-uri Spring `JpaRepository` cu operații personalizate | `domain/repository` (interogări derivate și `@Query`) |
| (Bonus) interfață CRUD proprie + implementare cu `EntityManager` | `IRepository`, `TeamCrudRepository`, `domain/repository/jpa/TeamRepositoryJPA` |
| Reguli de calcul și de audit cu `@EntityListeners` | `*EntityListener` din `domain/model` |
| Teste noi pentru repository-uri | `src/test/java/.../domain/repository`, `.../domain/model/EntityListenersTest` |
| Teste actualizate pentru serviciile de business | `ComplianceViewServiceTest`, `ComplianceWorkflowServiceTest`, `ValidationServiceTest` |

### Interogări personalizate

| Repository | Metodă | Folosită de |
| --- | --- | --- |
| `ComplianceViolationRepository` | `findOverdue` (`@Query`, termen SLA depășit) | `ComplianceWorkflowService.findOverdueViolations` |
| `ComplianceViolationRepository` | `findForTeam` (`@Query`, o singură interogare pe echipă) | `ComplianceViewService` (vederea C5) |
| `CloudResourceRepository` | `findUnencrypted`, `findWithViolationsIn` (`@Query`), `findByTeamName`, `findByEnvironment`, `findByProvider` | teste |
| `RegulatoryFrameworkRepository` | `findByCodeWithPolicies` (`join fetch`), `findByObligationLevel` | teste |
| `SecurityPolicyRepository` | `findByApplicableType`, `findByFrameworkCode` (polimorfe) | teste |
| `AppUserRepository`, `TeamRepository` | `findByRole`, `findByEmail`, `findByNameContainingIgnoreCase` | teste |

### Reguli aplicate prin `@EntityListeners`

| Listener | Callback | Regula |
| --- | --- | --- |
| `ComplianceViolationEntityListener` | `@PrePersist` | C2 de rezervă: dacă termenul SLA lipsește, se calculează din severitate |
| `ComplianceViolationEntityListener` | `@PrePersist`, `@PreUpdate` | A2 la nivel de bază de date: RESOLVED cere moment și justificare; rezolvarea nu poate fi înaintea detectării |
| `ComplianceViolationEntityListener` | `@PreRemove` | încălcările nu se șterg (trasabilitate) |
| `CloudResourceEntityListener` | `@PrePersist`, `@PreUpdate` | numele și ID-ul extern se curăță de spații |
| `CloudResourceEntityListener` | `@PreRemove` | o resursă cu încălcări nu se șterge |
| `AppUserEntityListener` | `@PrePersist`, `@PreUpdate` | e-mailul se salvează cu litere mici |
| `AuditLogEntityListener` | `@PreUpdate`, `@PreRemove` | jurnalul de audit este imutabil (NFR-02) |

Jurnalul de audit nu poate fi modificat din aplicație: `AuditLog` este `@Immutable`,
iar `AuditLogRepository` expune doar salvare și citire.
