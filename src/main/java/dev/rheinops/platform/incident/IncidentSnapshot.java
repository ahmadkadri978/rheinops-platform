package dev.rheinops.platform.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentSnapshot(
        UUID id,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        UUID affectedServiceId,
        long version,
        Instant reportedAt,
        Instant updatedAt,
        Instant resolvedAt) {}
