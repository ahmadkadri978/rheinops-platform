package dev.rheinops.platform.support.web;

import dev.rheinops.platform.shared.AuthenticatedActor;
import dev.rheinops.platform.support.DiagnosticType;
import dev.rheinops.platform.support.SupportCaseDiagnosticSnapshot;
import java.time.Instant;
import java.util.UUID;

record SupportCaseDiagnosticResponse(
        UUID id,
        UUID supportCaseId,
        DiagnosticType type,
        String summary,
        String details,
        AuthenticatedActor actor,
        Instant recordedAt) {

    static SupportCaseDiagnosticResponse from(SupportCaseDiagnosticSnapshot snapshot) {
        return new SupportCaseDiagnosticResponse(
                snapshot.id(), snapshot.supportCaseId(), snapshot.type(), snapshot.summary(),
                snapshot.details(), snapshot.actor(), snapshot.recordedAt());
    }
}
