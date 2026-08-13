package dev.rheinops.platform.incident.internal;

import dev.rheinops.platform.incident.IncidentStatus;
import dev.rheinops.platform.incident.IncidentTimelineEventType;
import dev.rheinops.platform.shared.AuthenticatedActor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "incident_timeline_events")
class IncidentTimelineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_number", nullable = false, updatable = false)
    private Long sequenceNumber;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 32)
    private IncidentTimelineEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", updatable = false, length = 32)
    private IncidentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, updatable = false, length = 32)
    private IncidentStatus newStatus;

    @Column(name = "actor_subject", nullable = false, updatable = false, columnDefinition = "text")
    private String actorSubject;

    @Column(
            name = "actor_display_name",
            nullable = false,
            updatable = false,
            columnDefinition = "text")
    private String actorDisplayName;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected IncidentTimelineEntity() {}

    static IncidentTimelineEntity incidentCreated(
            UUID incidentId, AuthenticatedActor actor, Instant occurredAt) {
        return create(
                incidentId,
                IncidentTimelineEventType.INCIDENT_CREATED,
                null,
                IncidentStatus.OPEN,
                actor,
                occurredAt);
    }

    static IncidentTimelineEntity statusChanged(
            UUID incidentId,
            IncidentStatus previousStatus,
            IncidentStatus newStatus,
            AuthenticatedActor actor,
            Instant occurredAt) {
        return create(
                incidentId,
                IncidentTimelineEventType.STATUS_CHANGED,
                previousStatus,
                newStatus,
                actor,
                occurredAt);
    }

    private static IncidentTimelineEntity create(
            UUID incidentId,
            IncidentTimelineEventType eventType,
            IncidentStatus previousStatus,
            IncidentStatus newStatus,
            AuthenticatedActor actor,
            Instant occurredAt) {
        IncidentTimelineEntity event = new IncidentTimelineEntity();
        event.incidentId = incidentId;
        event.eventType = eventType;
        event.previousStatus = previousStatus;
        event.newStatus = newStatus;
        event.actorSubject = actor.subject();
        event.actorDisplayName = actor.displayName();
        event.occurredAt = occurredAt;
        return event;
    }

    long sequenceNumber() {
        return sequenceNumber;
    }

    UUID incidentId() {
        return incidentId;
    }

    IncidentTimelineEventType eventType() {
        return eventType;
    }

    IncidentStatus previousStatus() {
        return previousStatus;
    }

    IncidentStatus newStatus() {
        return newStatus;
    }

    AuthenticatedActor actor() {
        return new AuthenticatedActor(actorSubject, actorDisplayName);
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
