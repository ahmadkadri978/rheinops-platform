package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.EscalationLevel;
import dev.rheinops.platform.support.SlaAssessment;
import dev.rheinops.platform.support.SupportCaseSnapshot;
import dev.rheinops.platform.support.SupportCaseStatus;
import dev.rheinops.platform.support.SupportPriority;
import java.time.Instant;
import java.util.UUID;

record SupportCaseResponse(
        UUID id,
        String caseReference,
        UUID organisationId,
        UUID serviceId,
        UUID incidentId,
        String requesterName,
        String requesterEmail,
        String subject,
        String description,
        SupportPriority priority,
        SupportCaseStatus status,
        UUID slaPolicyId,
        SlaAssessment sla,
        EscalationLevel escalationLevel,
        Instant escalatedAt,
        String runbookUrl,
        String resolutionNotes,
        long version,
        Instant responseDueAt,
        Instant resolutionDueAt,
        Instant firstRespondedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt) {

    static SupportCaseResponse from(SupportCaseSnapshot snapshot) {
        return new SupportCaseResponse(
                snapshot.id(), snapshot.caseReference(), snapshot.organisationId(), snapshot.serviceId(),
                snapshot.incidentId(), snapshot.requesterName(), snapshot.requesterEmail(), snapshot.subject(),
                snapshot.description(), snapshot.priority(), snapshot.status(), snapshot.slaPolicyId(),
                snapshot.sla(), snapshot.escalationLevel(), snapshot.escalatedAt(), snapshot.runbookUrl(),
                snapshot.resolutionNotes(), snapshot.version(), snapshot.responseDueAt(),
                snapshot.resolutionDueAt(), snapshot.firstRespondedAt(), snapshot.resolvedAt(),
                snapshot.createdAt(), snapshot.updatedAt());
    }
}
