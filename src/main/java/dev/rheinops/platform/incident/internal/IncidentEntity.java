package dev.rheinops.platform.incident.internal;

import dev.rheinops.platform.incident.IncidentSeverity;
import dev.rheinops.platform.incident.IncidentStatus;
import dev.rheinops.platform.shared.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
class IncidentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentStatus status;

    @Column(name = "affected_service_id", nullable = false)
    private UUID affectedServiceId;

    @Version
    private long version;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private Instant reportedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected IncidentEntity() {}

    static IncidentEntity create(
            UUID id,
            String title,
            String description,
            IncidentSeverity severity,
            UUID affectedServiceId,
            Instant now) {
        IncidentEntity incident = new IncidentEntity();
        incident.id = id;
        incident.title = title;
        incident.description = description;
        incident.severity = severity;
        incident.status = IncidentStatus.OPEN;
        incident.affectedServiceId = affectedServiceId;
        incident.reportedAt = now;
        incident.updatedAt = now;
        return incident;
    }

    void transitionTo(IncidentStatus next, long expectedVersion, Instant now) {
        if (version != expectedVersion) {
            throw new ConflictException(
                    "stale_incident_version",
                    "Incident version is %d, but the request expected %d."
                            .formatted(version, expectedVersion));
        }
        if (!status.canTransitionTo(next)) {
            throw new ConflictException(
                    "invalid_incident_transition",
                    "Incident cannot transition from %s to %s.".formatted(status, next));
        }
        if (status == next) {
            return;
        }
        status = next;
        updatedAt = now;
        if (next == IncidentStatus.RESOLVED) {
            resolvedAt = now;
        }
    }

    UUID id() {
        return id;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }

    IncidentSeverity severity() {
        return severity;
    }

    IncidentStatus status() {
        return status;
    }

    UUID affectedServiceId() {
        return affectedServiceId;
    }

    long version() {
        return version;
    }

    Instant reportedAt() {
        return reportedAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    Instant resolvedAt() {
        return resolvedAt;
    }
}
