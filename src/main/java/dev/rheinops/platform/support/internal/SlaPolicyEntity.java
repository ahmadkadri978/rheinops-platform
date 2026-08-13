package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.support.SupportPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sla_policies")
class SlaPolicyEntity {

    @Id
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private SupportPriority priority;

    @Column(name = "response_target_minutes", nullable = false)
    private int responseTargetMinutes;

    @Column(name = "resolution_target_minutes", nullable = false)
    private int resolutionTargetMinutes;

    @Column(name = "warning_threshold_percent", nullable = false)
    private int warningThresholdPercent;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SlaPolicyEntity() {}

    static SlaPolicyEntity create(
            UUID id,
            UUID organisationId,
            SupportPriority priority,
            int responseTargetMinutes,
            int resolutionTargetMinutes,
            int warningThresholdPercent,
            Instant now) {
        SlaPolicyEntity policy = new SlaPolicyEntity();
        policy.id = id;
        policy.organisationId = organisationId;
        policy.priority = priority;
        policy.responseTargetMinutes = responseTargetMinutes;
        policy.resolutionTargetMinutes = resolutionTargetMinutes;
        policy.warningThresholdPercent = warningThresholdPercent;
        policy.active = true;
        policy.createdAt = now;
        return policy;
    }

    void deactivate() {
        active = false;
    }

    UUID id() { return id; }
    UUID organisationId() { return organisationId; }
    SupportPriority priority() { return priority; }
    int responseTargetMinutes() { return responseTargetMinutes; }
    int resolutionTargetMinutes() { return resolutionTargetMinutes; }
    int warningThresholdPercent() { return warningThresholdPercent; }
    Instant createdAt() { return createdAt; }
}
