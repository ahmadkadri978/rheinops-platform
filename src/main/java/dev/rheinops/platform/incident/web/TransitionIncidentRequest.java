package dev.rheinops.platform.incident.web;

import dev.rheinops.platform.incident.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

record TransitionIncidentRequest(
        @NotNull IncidentStatus status, @NotNull @PositiveOrZero Long expectedVersion) {}
