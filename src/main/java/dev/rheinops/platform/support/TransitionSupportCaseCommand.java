package dev.rheinops.platform.support;

public record TransitionSupportCaseCommand(
        SupportCaseStatus status, long expectedVersion, String resolutionNotes) {}
