package dev.rheinops.platform.incident.internal;

import dev.rheinops.platform.incident.CreateIncidentCommand;
import dev.rheinops.platform.incident.IncidentOperations;
import dev.rheinops.platform.incident.IncidentSeverity;
import dev.rheinops.platform.incident.IncidentSnapshot;
import dev.rheinops.platform.incident.IncidentStatus;
import dev.rheinops.platform.incident.TransitionIncidentCommand;
import dev.rheinops.platform.servicecatalog.ServiceCatalog;
import dev.rheinops.platform.shared.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class IncidentService implements IncidentOperations {

    private final IncidentRepository repository;
    private final ServiceCatalog serviceCatalog;
    private final Clock clock;

    IncidentService(IncidentRepository repository, ServiceCatalog serviceCatalog, Clock clock) {
        this.repository = repository;
        this.serviceCatalog = serviceCatalog;
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
        return toSnapshot(repository.save(incident));
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
        incident.transitionTo(command.status(), command.expectedVersion(), clock.instant());
        return toSnapshot(repository.saveAndFlush(incident));
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
}
