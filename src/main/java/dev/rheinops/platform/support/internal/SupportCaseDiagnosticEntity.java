package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.shared.AuthenticatedActor;
import dev.rheinops.platform.support.DiagnosticType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "support_case_diagnostics")
class SupportCaseDiagnosticEntity {

    @Id
    private UUID id;

    @Column(name = "support_case_id", nullable = false)
    private UUID supportCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DiagnosticType type;

    @Column(nullable = false, length = 240)
    private String summary;

    @Column(nullable = false, length = 8000)
    private String details;

    @Column(name = "actor_subject", nullable = false)
    private String actorSubject;

    @Column(name = "actor_display_name", nullable = false)
    private String actorDisplayName;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected SupportCaseDiagnosticEntity() {}

    static SupportCaseDiagnosticEntity create(
            UUID id,
            UUID supportCaseId,
            DiagnosticType type,
            String summary,
            String details,
            AuthenticatedActor actor,
            Instant now) {
        SupportCaseDiagnosticEntity diagnostic = new SupportCaseDiagnosticEntity();
        diagnostic.id = id;
        diagnostic.supportCaseId = supportCaseId;
        diagnostic.type = type;
        diagnostic.summary = summary;
        diagnostic.details = details;
        diagnostic.actorSubject = actor.subject();
        diagnostic.actorDisplayName = actor.displayName();
        diagnostic.recordedAt = now;
        return diagnostic;
    }

    UUID id() { return id; }
    UUID supportCaseId() { return supportCaseId; }
    DiagnosticType type() { return type; }
    String summary() { return summary; }
    String details() { return details; }
    AuthenticatedActor actor() { return new AuthenticatedActor(actorSubject, actorDisplayName); }
    Instant recordedAt() { return recordedAt; }
}
