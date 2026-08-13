package dev.rheinops.platform.incident.internal;

import dev.rheinops.platform.incident.CreateIncidentCommand;
import dev.rheinops.platform.incident.IncidentOperations;
import dev.rheinops.platform.incident.IncidentSeverity;
import dev.rheinops.platform.incident.IncidentSnapshot;
import dev.rheinops.platform.incident.IncidentStatus;
import dev.rheinops.platform.incident.IncidentTimelineEntry;
import dev.rheinops.platform.incident.TransitionIncidentCommand;
import dev.rheinops.platform.servicecatalog.ServiceCatalog;
import dev.rheinops.platform.shared.ActorProvider;
import dev.rheinops.platform.shared.AuthenticatedActor;
import dev.rheinops.platform.shared.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class IncidentService implements IncidentOperations {

    private final IncidentRepository repository;
    private final IncidentTimelineRepository timelineRepository;
    private final ServiceCatalog serviceCatalog;
    private final ActorProvider actorProvider;
    private final Clock clock;

    IncidentService(
            IncidentRepository repository,
            IncidentTimelineRepository timelineRepository,
            ServiceCatalog serviceCatalog,
            ActorProvider actorProvider,
            Clock clock) {
        this.repository = repository;
        this.timelineRepository = timelineRepository;
        this.serviceCatalog = serviceCatalog;
        this.actorProvider = actorProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IncidentSnapshot create(CreateIncidentCommand command) {
        serviceCatalog.getRequired(command.affectedServiceId());
        Instant now = clock.instant();
        IncidentEntity incident = IncidentEntity.create(
                UUID.randomUUID(),
                command.title().trim(),
                command.description().trim(),
                command.severity(),
                command.affectedServiceId(),
                now);
        IncidentEntity saved = repository.saveAndFlush(incident);
        timelineRepository.save(
                IncidentTimelineEntity.incidentCreated(saved.id(), actorProvider.currentActor(), now));
        return toSnapshot(saved);
    }

    @Override
    public IncidentSnapshot getRequired(UUID incidentId) {
        return toSnapshot(findEntity(incidentId));
    }

    @Override
    public Page<IncidentSnapshot> findAll(
            IncidentStatus status, IncidentSeverity severity, Pageable pageable) {
        Page<IncidentEntity> incidents;
        if (status != null && severity != null) {
            incidents = repository.findByStatusAndSeverity(status, severity, pageable);
        } else if (status != null) {
            incidents = repository.findByStatus(status, pageable);
        } else if (severity != null) {
            incidents = repository.findBySeverity(severity, pageable);
        } else {
            incidents = repository.findAll(pageable);
        }
        return incidents.map(IncidentService::toSnapshot);
    }

    @Override
    @Transactional
    public IncidentSnapshot transition(UUID incidentId, TransitionIncidentCommand command) {
        IncidentEntity incident = findEntity(incidentId);
        IncidentStatus previousStatus = incident.status();
        Instant now = clock.instant();
        incident.transitionTo(command.status(), command.expectedVersion(), now);
        IncidentEntity saved = repository.saveAndFlush(incident);

        if (previousStatus != saved.status()) {
            AuthenticatedActor actor = actorProvider.currentActor();
            timelineRepository.save(IncidentTimelineEntity.statusChanged(
                    incidentId, previousStatus, saved.status(), actor, now));
        }
        return toSnapshot(saved);
    }

    @Override
    public Page<IncidentTimelineEntry> timeline(UUID incidentId, Pageable pageable) {
        findEntity(incidentId);
        Pageable chronologicalPage = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "sequenceNumber"));
        return timelineRepository
                .findByIncidentId(incidentId, chronologicalPage)
                .map(IncidentService::toTimelineEntry);
    }

    private IncidentEntity findEntity(UUID incidentId) {
        return repository
                .findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "incident_not_found", "Incident %s was not found.".formatted(incidentId)));
    }

    private static IncidentSnapshot toSnapshot(IncidentEntity incident) {
        return new IncidentSnapshot(
                incident.id(),
                incident.title(),
                incident.description(),
                incident.severity(),
                incident.status(),
                incident.affectedServiceId(),
                incident.version(),
                incident.reportedAt(),
                incident.updatedAt(),
                incident.resolvedAt());
    }

    private static IncidentTimelineEntry toTimelineEntry(IncidentTimelineEntity event) {
        return new IncidentTimelineEntry(
                event.sequenceNumber(),
                event.incidentId(),
                event.eventType(),
                event.previousStatus(),
                event.newStatus(),
                event.actor(),
                event.occurredAt());
    }
}
