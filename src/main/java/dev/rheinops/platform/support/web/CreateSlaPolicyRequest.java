package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.SupportPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record CreateSlaPolicyRequest(
        @NotNull SupportPriority priority,
        @Min(1) @Max(10080) int responseTargetMinutes,
        @Min(1) @Max(43200) int resolutionTargetMinutes,
        @Min(1) @Max(99) int warningThresholdPercent) {}
