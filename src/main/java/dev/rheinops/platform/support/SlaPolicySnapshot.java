package dev.rheinops.platform.support;

import java.time.Instant;
import java.util.UUID;

public record SlaPolicySnapshot(
        UUID id,
        UUID organisationId,
        SupportPriority priority,
        int responseTargetMinutes,
        int resolutionTargetMinutes,
        int warningThresholdPercent,
        Instant createdAt) {}
