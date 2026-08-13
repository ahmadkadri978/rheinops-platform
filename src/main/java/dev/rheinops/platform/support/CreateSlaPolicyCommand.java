package dev.rheinops.platform.support;

public record CreateSlaPolicyCommand(
        SupportPriority priority,
        int responseTargetMinutes,
        int resolutionTargetMinutes,
        int warningThresholdPercent) {}
