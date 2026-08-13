package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.SupportCaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

record TransitionSupportCaseRequest(
        @NotNull SupportCaseStatus status,
        @NotNull @PositiveOrZero Long expectedVersion,
        @Size(max = 4000) String resolutionNotes) {}
