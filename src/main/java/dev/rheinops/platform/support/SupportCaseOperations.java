package dev.rheinops.platform.support;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupportCaseOperations {

    SupportCaseSnapshot create(CreateSupportCaseCommand command);

    SupportCaseSnapshot getRequired(UUID supportCaseId);

    Page<SupportCaseSnapshot> findAll(
            UUID organisationId,
            SupportCaseStatus status,
            SupportPriority priority,
            EscalationLevel escalationLevel,
            Pageable pageable);

    SupportCaseSnapshot transition(UUID supportCaseId, TransitionSupportCaseCommand command);

    SupportCaseSnapshot updateRunbook(UUID supportCaseId, UpdateRunbookCommand command);

    SupportCaseDiagnosticSnapshot addDiagnostic(UUID supportCaseId, AddDiagnosticCommand command);

    Page<SupportCaseDiagnosticSnapshot> diagnostics(UUID supportCaseId, Pageable pageable);
}
