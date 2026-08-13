package dev.rheinops.platform.support;

import java.util.UUID;

public record CreateSupportCaseCommand(
        UUID organisationId,
        UUID serviceId,
        UUID incidentId,
        String requesterName,
        String requesterEmail,
        String subject,
        String description,
        SupportPriority priority,
        String runbookUrl) {}
