package dev.rheinops.platform.support;

import java.time.Instant;
import java.util.UUID;

public record SupportCaseSnapshot(
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
        Instant updatedAt) {}
