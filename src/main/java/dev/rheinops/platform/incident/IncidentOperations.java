package dev.rheinops.platform.incident;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentOperations {

    IncidentSnapshot create(CreateIncidentCommand command);

    IncidentSnapshot getRequired(UUID incidentId);

    Page<IncidentSnapshot> findAll(
            IncidentStatus status, IncidentSeverity severity, Pageable pageable);

    IncidentSnapshot transition(UUID incidentId, TransitionIncidentCommand command);
}
