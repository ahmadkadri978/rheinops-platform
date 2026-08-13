package dev.rheinops.platform.incident;

public record TransitionIncidentCommand(IncidentStatus status, long expectedVersion) {}
