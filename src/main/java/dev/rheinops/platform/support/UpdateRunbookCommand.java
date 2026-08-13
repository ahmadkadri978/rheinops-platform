package dev.rheinops.platform.support;

public record UpdateRunbookCommand(String runbookUrl, long expectedVersion) {}
