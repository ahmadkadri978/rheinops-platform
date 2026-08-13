package dev.rheinops.platform.incident.web;

import dev.rheinops.platform.incident.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record CreateIncidentRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull IncidentSeverity severity,
        @NotNull UUID affectedServiceId) {}
