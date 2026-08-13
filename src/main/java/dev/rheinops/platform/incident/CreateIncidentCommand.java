package dev.rheinops.platform.incident;

import java.util.UUID;

public record CreateIncidentCommand(
        String title, String description, IncidentSeverity severity, UUID affectedServiceId) {}
