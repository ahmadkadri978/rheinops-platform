package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.incident.IncidentOperations;
import dev.rheinops.platform.servicecatalog.ServiceCatalog;
import dev.rheinops.platform.shared.ActorProvider;
import dev.rheinops.platform.shared.InvalidRequestException;
import dev.rheinops.platform.shared.ResourceNotFoundException;
import dev.rheinops.platform.support.AddDiagnosticCommand;
import dev.rheinops.platform.support.CreateSupportCaseCommand;
import dev.rheinops.platform.support.EscalationLevel;
import dev.rheinops.platform.support.SlaAssessment;
import dev.rheinops.platform.support.SupportCaseDiagnosticSnapshot;
import dev.rheinops.platform.support.SupportCaseOperations;
import dev.rheinops.platform.support.SupportCaseSnapshot;
import dev.rheinops.platform.support.SupportCaseStatus;
import dev.rheinops.platform.support.SupportPriority;
import dev.rheinops.platform.support.TransitionSupportCaseCommand;
import dev.rheinops.platform.support.UpdateRunbookCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SupportCaseService implements SupportCaseOperations {

    private static final DateTimeFormatter REFERENCE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final SupportCaseRepository cases;
    private final SupportCaseDiagnosticRepository diagnostics;
    private final CustomerOrganisationRepository organisations;
    private final SlaPolicyRepository policies;
    private final ServiceCatalog serviceCatalog;
    private final IncidentOperations incidents;
    private final ActorProvider actorProvider;
    private final SlaCalculator slaCalculator;
    private final SlaEscalationService escalationService;
    private final Clock clock;

    SupportCaseService(
            SupportCaseRepository cases,
            SupportCaseDiagnosticRepository diagnostics,
            CustomerOrganisationRepository organisations,
            SlaPolicyRepository policies,
            ServiceCatalog serviceCatalog,
            IncidentOperations incidents,
            ActorProvider actorProvider,
            SlaCalculator slaCalculator,
            SlaEscalationService escalationService,
            Clock clock) {
        this.cases = cases;
        this.diagnostics = diagnostics;
        this.organisations = organisations;
        this.policies = policies;
        this.serviceCatalog = serviceCatalog;
        this.incidents = incidents;
        this.actorProvider = actorProvider;
        this.slaCalculator = slaCalculator;
        this.escalationService = escalationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SupportCaseSnapshot create(CreateSupportCaseCommand command) {
        requireOrganisation(command.organisationId());
        serviceCatalog.getRequired(command.serviceId());
        if (command.incidentId() != null) {
            var incident = incidents.getRequired(command.incidentId());
            if (!incident.affectedServiceId().equals(command.serviceId())) {
                throw new InvalidRequestException(
                        "incident_service_mismatch",
                        "The linked incident does not affect the support case service.");
            }
        }
        SlaPolicyEntity policy = policies
                .findByOrganisationIdAndPriorityAndActiveTrue(
                        command.organisationId(), command.priority())
                .orElseThrow(() -> new InvalidRequestException(
                        "sla_policy_missing",
                        "No active %s SLA policy exists for the customer organisation."
                                .formatted(command.priority())));
        Instant now = clock.instant();
        UUID id = UUID.randomUUID();
        SupportCaseEntity supportCase = SupportCaseEntity.create(
                id,
                caseReference(id, now),
                command.organisationId(),
                command.serviceId(),
                command.incidentId(),
                command.requesterName().trim(),
                command.requesterEmail().trim().toLowerCase(),
                command.subject().trim(),
                command.description().trim(),
                command.priority(),
                policy,
                normalizeNullable(command.runbookUrl()),
                now);
        return toSnapshot(cases.saveAndFlush(supportCase), now);
    }

    @Override
    @Transactional
    public SupportCaseSnapshot getRequired(UUID supportCaseId) {
        Instant now = clock.instant();
        escalationService.refresh(now);
        SupportCaseEntity supportCase = findEntity(supportCaseId);
        return toSnapshot(supportCase, now);
    }

    @Override
    @Transactional
    public Page<SupportCaseSnapshot> findAll(
            UUID organisationId,
            SupportCaseStatus status,
            SupportPriority priority,
            EscalationLevel escalationLevel,
            Pageable pageable) {
        Instant now = clock.instant();
        escalationService.refresh(now);
        Page<SupportCaseEntity> page = cases.findAll(
                SupportCaseRepository.matches(organisationId, status, priority, escalationLevel), pageable);
        return page.map(supportCase -> toSnapshot(supportCase, now));
    }

    @Override
    @Transactional
    public SupportCaseSnapshot transition(UUID supportCaseId, TransitionSupportCaseCommand command) {
        Instant now = clock.instant();
        SupportCaseEntity supportCase = findEntity(supportCaseId);
        supportCase.transitionTo(
                command.status(), command.expectedVersion(), command.resolutionNotes(), now);
        applyEscalation(supportCase, now);
        return toSnapshot(cases.saveAndFlush(supportCase), now);
    }

    @Override
    @Transactional
    public SupportCaseSnapshot updateRunbook(UUID supportCaseId, UpdateRunbookCommand command) {
        Instant now = clock.instant();
        SupportCaseEntity supportCase = findEntity(supportCaseId);
        supportCase.updateRunbook(normalizeNullable(command.runbookUrl()), command.expectedVersion(), now);
        applyEscalation(supportCase, now);
        return toSnapshot(cases.saveAndFlush(supportCase), now);
    }

    @Override
    @Transactional
    public SupportCaseDiagnosticSnapshot addDiagnostic(
            UUID supportCaseId, AddDiagnosticCommand command) {
        SupportCaseEntity supportCase = findEntity(supportCaseId);
        if (supportCase.status().isTerminal()) {
            throw new InvalidRequestException(
                    "support_case_closed", "Diagnostics cannot be added to a closed support case.");
        }
        Instant now = clock.instant();
        SupportCaseDiagnosticEntity diagnostic = SupportCaseDiagnosticEntity.create(
                UUID.randomUUID(),
                supportCaseId,
                command.type(),
                command.summary().trim(),
                command.details().trim(),
                actorProvider.currentActor(),
                now);
        return toSnapshot(diagnostics.save(diagnostic));
    }

    @Override
    public Page<SupportCaseDiagnosticSnapshot> diagnostics(UUID supportCaseId, Pageable pageable) {
        findEntity(supportCaseId);
        Pageable chronological = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "recordedAt"));
        return diagnostics.findBySupportCaseId(supportCaseId, chronological)
                .map(SupportCaseService::toSnapshot);
    }

    private void applyEscalation(SupportCaseEntity supportCase, Instant now) {
        supportCase.applyEscalation(slaCalculator.assess(supportCase, now).health(), now);
    }

    private SupportCaseEntity findEntity(UUID supportCaseId) {
        return cases.findById(supportCaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "support_case_not_found",
                        "Support case %s was not found.".formatted(supportCaseId)));
    }

    private void requireOrganisation(UUID organisationId) {
        if (!organisations.existsById(organisationId)) {
            throw new ResourceNotFoundException(
                    "customer_organisation_not_found",
                    "Customer organisation %s was not found.".formatted(organisationId));
        }
    }

    private SupportCaseSnapshot toSnapshot(SupportCaseEntity supportCase, Instant now) {
        SlaAssessment sla = slaCalculator.assess(supportCase, now);
        return new SupportCaseSnapshot(
                supportCase.id(),
                supportCase.caseReference(),
                supportCase.organisationId(),
                supportCase.serviceId(),
                supportCase.incidentId(),
                supportCase.requesterName(),
                supportCase.requesterEmail(),
                supportCase.subject(),
                supportCase.description(),
                supportCase.priority(),
                supportCase.status(),
                supportCase.slaPolicyId(),
                sla,
                supportCase.escalationLevel(),
                supportCase.escalatedAt(),
                supportCase.runbookUrl(),
                supportCase.resolutionNotes(),
                supportCase.version(),
                supportCase.responseDueAt(),
                supportCase.resolutionDueAt(),
                supportCase.firstRespondedAt(),
                supportCase.resolvedAt(),
                supportCase.createdAt(),
                supportCase.updatedAt());
    }

    private static SupportCaseDiagnosticSnapshot toSnapshot(SupportCaseDiagnosticEntity diagnostic) {
        return new SupportCaseDiagnosticSnapshot(
                diagnostic.id(),
                diagnostic.supportCaseId(),
                diagnostic.type(),
                diagnostic.summary(),
                diagnostic.details(),
                diagnostic.actor(),
                diagnostic.recordedAt());
    }

    private static String caseReference(UUID id, Instant now) {
        return "SUP-%s-%s".formatted(
                REFERENCE_TIME.format(now), id.toString().substring(0, 6).toUpperCase());
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
