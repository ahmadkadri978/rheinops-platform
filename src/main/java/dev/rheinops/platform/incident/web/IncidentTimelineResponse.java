package dev.rheinops.platform.incident.web;

import dev.rheinops.platform.incident.IncidentStatus;
import dev.rheinops.platform.incident.IncidentTimelineEntry;
import dev.rheinops.platform.incident.IncidentTimelineEventType;
import java.time.Instant;
import java.util.UUID;

record IncidentTimelineResponse(
        long sequenceNumber,
        UUID incidentId,
        IncidentTimelineEventType eventType,
        IncidentStatus previousStatus,
        IncidentStatus newStatus,
        ActorResponse actor,
        Instant occurredAt) {

    static IncidentTimelineResponse from(IncidentTimelineEntry entry) {
        return new IncidentTimelineResponse(
                entry.sequenceNumber(),
                entry.incidentId(),
                entry.eventType(),
                entry.previousStatus(),
                entry.newStatus(),
                new ActorResponse(entry.actor().subject(), entry.actor().displayName()),
                entry.occurredAt());
    }

    record ActorResponse(String subject, String displayName) {}
}
