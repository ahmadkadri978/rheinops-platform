package dev.rheinops.platform.support;

import dev.rheinops.platform.shared.AuthenticatedActor;
import java.time.Instant;
import java.util.UUID;

public record SupportCaseDiagnosticSnapshot(
        UUID id,
        UUID supportCaseId,
        DiagnosticType type,
        String summary,
        String details,
        AuthenticatedActor actor,
        Instant recordedAt) {}
