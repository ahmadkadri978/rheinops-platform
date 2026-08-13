package dev.rheinops.platform.incident.web;

import dev.rheinops.platform.incident.IncidentSeverity;
import dev.rheinops.platform.incident.IncidentSnapshot;
import dev.rheinops.platform.incident.IncidentStatus;
import java.time.Instant;
import java.util.UUID;

record IncidentResponse(
        UUID id,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        UUID affectedServiceId,
        long version,
        Instant reportedAt,
        Instant updatedAt,
        Instant resolvedAt) {

    static IncidentResponse from(IncidentSnapshot incident) {
        return new IncidentResponse(
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
