package dev.rheinops.platform.incident;

import dev.rheinops.platform.shared.AuthenticatedActor;
import java.time.Instant;
import java.util.UUID;

public record IncidentTimelineEntry(
        long sequenceNumber,
        UUID incidentId,
        IncidentTimelineEventType eventType,
        IncidentStatus previousStatus,
        IncidentStatus newStatus,
        AuthenticatedActor actor,
        Instant occurredAt) {}
