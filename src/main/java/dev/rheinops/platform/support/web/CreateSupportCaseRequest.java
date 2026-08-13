package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.SupportPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record CreateSupportCaseRequest(
        @NotNull UUID organisationId,
        @NotNull UUID serviceId,
        UUID incidentId,
        @NotBlank @Size(max = 120) String requesterName,
        @NotBlank @Email @Size(max = 254) String requesterEmail,
        @NotBlank @Size(max = 180) String subject,
        @NotBlank @Size(max = 4000) String description,
        @NotNull SupportPriority priority,
        @Size(max = 500)
        @Pattern(regexp = "^(?:|https://.*)$", message = "must use HTTPS")
                String runbookUrl) {}
