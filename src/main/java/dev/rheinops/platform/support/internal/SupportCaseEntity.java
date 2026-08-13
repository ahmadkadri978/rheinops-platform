package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.shared.ConflictException;
import dev.rheinops.platform.shared.InvalidRequestException;
import dev.rheinops.platform.support.EscalationLevel;
import dev.rheinops.platform.support.SlaHealth;
import dev.rheinops.platform.support.SupportCaseStatus;
import dev.rheinops.platform.support.SupportPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_cases")
class SupportCaseEntity {

    @Id
    private UUID id;

    @Column(name = "case_reference", nullable = false, unique = true, length = 32)
    private String caseReference;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "requester_name", nullable = false, length = 120)
    private String requesterName;

    @Column(name = "requester_email", nullable = false, length = 254)
    private String requesterEmail;

    @Column(nullable = false, length = 180)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private SupportPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SupportCaseStatus status;

    @Column(name = "sla_policy_id", nullable = false)
    private UUID slaPolicyId;

    @Column(name = "response_due_at", nullable = false)
    private Instant responseDueAt;

    @Column(name = "resolution_due_at", nullable = false)
    private Instant resolutionDueAt;

    @Column(name = "warning_threshold_percent", nullable = false)
    private int warningThresholdPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false, length = 16)
    private EscalationLevel escalationLevel;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "runbook_url", length = 500)
    private String runbookUrl;

    @Column(name = "resolution_notes", length = 4000)
    private String resolutionNotes;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected SupportCaseEntity() {}

    static SupportCaseEntity create(
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
            SlaPolicyEntity policy,
            String runbookUrl,
            Instant now) {
        SupportCaseEntity supportCase = new SupportCaseEntity();
        supportCase.id = id;
        supportCase.caseReference = caseReference;
        supportCase.organisationId = organisationId;
        supportCase.serviceId = serviceId;
        supportCase.incidentId = incidentId;
        supportCase.requesterName = requesterName;
        supportCase.requesterEmail = requesterEmail;
        supportCase.subject = subject;
        supportCase.description = description;
        supportCase.priority = priority;
        supportCase.status = SupportCaseStatus.OPEN;
        supportCase.slaPolicyId = policy.id();
        supportCase.responseDueAt = now.plus(Duration.ofMinutes(policy.responseTargetMinutes()));
        supportCase.resolutionDueAt = now.plus(Duration.ofMinutes(policy.resolutionTargetMinutes()));
        supportCase.warningThresholdPercent = policy.warningThresholdPercent();
        supportCase.escalationLevel = EscalationLevel.NONE;
        supportCase.runbookUrl = runbookUrl;
        supportCase.createdAt = now;
        supportCase.updatedAt = now;
        return supportCase;
    }

    void transitionTo(
            SupportCaseStatus next, long expectedVersion, String newResolutionNotes, Instant now) {
        requireVersion(expectedVersion);
        if (!status.canTransitionTo(next)) {
            throw new ConflictException(
                    "invalid_support_case_transition",
                    "Support case cannot transition from %s to %s.".formatted(status, next));
        }
        if ((next == SupportCaseStatus.RESOLVED || next == SupportCaseStatus.CLOSED)
                && (newResolutionNotes == null || newResolutionNotes.isBlank())
                && (resolutionNotes == null || resolutionNotes.isBlank())) {
            throw new InvalidRequestException(
                    "resolution_notes_required",
                    "Resolution notes are required before resolving or closing a support case.");
        }
        if (newResolutionNotes != null && !newResolutionNotes.isBlank()) {
            resolutionNotes = newResolutionNotes.trim();
        }
        if (status == next) {
            return;
        }
        status = next;
        updatedAt = now;
        if (firstRespondedAt == null && next != SupportCaseStatus.OPEN) {
            firstRespondedAt = now;
        }
        if (next == SupportCaseStatus.RESOLVED) {
            resolvedAt = now;
        }
    }

    void updateRunbook(String nextRunbookUrl, long expectedVersion, Instant now) {
        requireVersion(expectedVersion);
        if (status.isTerminal()) {
            throw new ConflictException(
                    "support_case_closed", "A closed support case cannot be modified.");
        }
        runbookUrl = nextRunbookUrl;
        updatedAt = now;
    }

    boolean applyEscalation(SlaHealth health, Instant now) {
        EscalationLevel required = EscalationLevel.requiredFor(health);
        if (required.ordinal() > escalationLevel.ordinal()) {
            escalationLevel = required;
            escalatedAt = now;
            return true;
        }
        return false;
    }

    private void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new ConflictException(
                    "stale_support_case_version",
                    "Support case version is %d, but the request expected %d."
                            .formatted(version, expectedVersion));
        }
    }

    UUID id() { return id; }
    String caseReference() { return caseReference; }
    UUID organisationId() { return organisationId; }
    UUID serviceId() { return serviceId; }
    UUID incidentId() { return incidentId; }
    String requesterName() { return requesterName; }
    String requesterEmail() { return requesterEmail; }
    String subject() { return subject; }
    String description() { return description; }
    SupportPriority priority() { return priority; }
    SupportCaseStatus status() { return status; }
    UUID slaPolicyId() { return slaPolicyId; }
    Instant responseDueAt() { return responseDueAt; }
    Instant resolutionDueAt() { return resolutionDueAt; }
    int warningThresholdPercent() { return warningThresholdPercent; }
    EscalationLevel escalationLevel() { return escalationLevel; }
    Instant escalatedAt() { return escalatedAt; }
    Instant firstRespondedAt() { return firstRespondedAt; }
    String runbookUrl() { return runbookUrl; }
    String resolutionNotes() { return resolutionNotes; }
    long version() { return version; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
    Instant resolvedAt() { return resolvedAt; }
}
