package dev.rheinops.platform.support;

public record AddDiagnosticCommand(DiagnosticType type, String summary, String details) {}
