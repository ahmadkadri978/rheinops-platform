package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.SlaPolicySnapshot;
import dev.rheinops.platform.support.SupportPriority;
import java.time.Instant;
import java.util.UUID;

record SlaPolicyResponse(
        UUID id,
        UUID organisationId,
        SupportPriority priority,
        int responseTargetMinutes,
        int resolutionTargetMinutes,
        int warningThresholdPercent,
        Instant createdAt) {

    static SlaPolicyResponse from(SlaPolicySnapshot snapshot) {
        return new SlaPolicyResponse(
                snapshot.id(), snapshot.organisationId(), snapshot.priority(),
                snapshot.responseTargetMinutes(), snapshot.resolutionTargetMinutes(),
                snapshot.warningThresholdPercent(), snapshot.createdAt());
    }
}
