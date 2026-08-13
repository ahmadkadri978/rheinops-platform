package dev.rheinops.platform.support.web;

import dev.rheinops.platform.shared.PageResponse;
import dev.rheinops.platform.support.AddDiagnosticCommand;
import dev.rheinops.platform.support.CreateSupportCaseCommand;
import dev.rheinops.platform.support.EscalationLevel;
import dev.rheinops.platform.support.SupportCaseOperations;
import dev.rheinops.platform.support.SupportCaseStatus;
import dev.rheinops.platform.support.SupportPriority;
import dev.rheinops.platform.support.TransitionSupportCaseCommand;
import dev.rheinops.platform.support.UpdateRunbookCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support-cases")
@Tag(name = "Support Cases")
@SecurityRequirement(name = "bearerAuth")
public class SupportCaseController {

    private final SupportCaseOperations supportCases;

    public SupportCaseController(SupportCaseOperations supportCases) {
        this.supportCases = supportCases;
    }

    @PostMapping
    @Operation(summary = "Open a customer support case")
    ResponseEntity<SupportCaseResponse> create(@Valid @RequestBody CreateSupportCaseRequest request) {
        var supportCase = supportCases.create(new CreateSupportCaseCommand(
                request.organisationId(), request.serviceId(), request.incidentId(),
                request.requesterName(), request.requesterEmail(), request.subject(),
                request.description(), request.priority(), request.runbookUrl()));
        return ResponseEntity.created(URI.create("/api/v1/support-cases/" + supportCase.id()))
                .body(SupportCaseResponse.from(supportCase));
    }

    @GetMapping("/{supportCaseId}")
    @Operation(summary = "Get a support case with its live SLA assessment")
    SupportCaseResponse get(@PathVariable UUID supportCaseId) {
        return SupportCaseResponse.from(supportCases.getRequired(supportCaseId));
    }

    @GetMapping
    @Operation(summary = "List and filter support cases")
    PageResponse<SupportCaseResponse> findAll(
            @RequestParam(required = false) UUID organisationId,
            @RequestParam(required = false) SupportCaseStatus status,
            @RequestParam(required = false) SupportPriority priority,
            @RequestParam(required = false) EscalationLevel escalationLevel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.from(
                supportCases.findAll(organisationId, status, priority, escalationLevel, pageable),
                SupportCaseResponse::from);
    }

    @PatchMapping("/{supportCaseId}/status")
    @Operation(summary = "Move a support case through its lifecycle")
    SupportCaseResponse transition(
            @PathVariable UUID supportCaseId,
            @Valid @RequestBody TransitionSupportCaseRequest request) {
        return SupportCaseResponse.from(supportCases.transition(
                supportCaseId,
                new TransitionSupportCaseCommand(
                        request.status(), request.expectedVersion(), request.resolutionNotes())));
    }

    @PatchMapping("/{supportCaseId}/runbook")
    @Operation(summary = "Attach or replace the support runbook link")
    SupportCaseResponse updateRunbook(
            @PathVariable UUID supportCaseId,
            @Valid @RequestBody UpdateRunbookRequest request) {
        return SupportCaseResponse.from(supportCases.updateRunbook(
                supportCaseId,
                new UpdateRunbookCommand(request.runbookUrl(), request.expectedVersion())));
    }

    @PostMapping("/{supportCaseId}/diagnostics")
    @Operation(summary = "Record a diagnostic observation")
    ResponseEntity<SupportCaseDiagnosticResponse> addDiagnostic(
            @PathVariable UUID supportCaseId,
            @Valid @RequestBody AddDiagnosticRequest request) {
        var diagnostic = supportCases.addDiagnostic(
                supportCaseId,
                new AddDiagnosticCommand(request.type(), request.summary(), request.details()));
        return ResponseEntity.created(URI.create(
                        "/api/v1/support-cases/%s/diagnostics/%s"
                                .formatted(supportCaseId, diagnostic.id())))
                .body(SupportCaseDiagnosticResponse.from(diagnostic));
    }

    @GetMapping("/{supportCaseId}/diagnostics")
    @Operation(summary = "Read support diagnostics")
    PageResponse<SupportCaseDiagnosticResponse> diagnostics(
            @PathVariable UUID supportCaseId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(
                supportCases.diagnostics(supportCaseId, pageable),
                SupportCaseDiagnosticResponse::from);
    }
}
