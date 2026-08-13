package dev.rheinops.platform.incident.web;

import dev.rheinops.platform.incident.CreateIncidentCommand;
import dev.rheinops.platform.incident.IncidentOperations;
import dev.rheinops.platform.incident.IncidentSeverity;
import dev.rheinops.platform.incident.IncidentSnapshot;
import dev.rheinops.platform.incident.IncidentStatus;
import dev.rheinops.platform.incident.TransitionIncidentCommand;
import dev.rheinops.platform.shared.PageResponse;
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
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incident Management")
@SecurityRequirement(name = "bearerAuth")
public class IncidentController {

    private final IncidentOperations incidents;

    public IncidentController(IncidentOperations incidents) {
        this.incidents = incidents;
    }

    @PostMapping
    @Operation(summary = "Report an incident")
    ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentSnapshot incident = incidents.create(new CreateIncidentCommand(
                request.title(),
                request.description(),
                request.severity(),
                request.affectedServiceId()));
        return ResponseEntity.created(URI.create("/api/v1/incidents/" + incident.id()))
                .body(IncidentResponse.from(incident));
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get an incident")
    IncidentResponse get(@PathVariable UUID incidentId) {
        return IncidentResponse.from(incidents.getRequired(incidentId));
    }

    @GetMapping
    @Operation(summary = "List and filter incidents")
    PageResponse<IncidentResponse> findAll(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @PageableDefault(size = 20, sort = "reportedAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.from(incidents.findAll(status, severity, pageable), IncidentResponse::from);
    }

    @PatchMapping("/{incidentId}/status")
    @Operation(summary = "Move an incident through its lifecycle")
    IncidentResponse transition(
            @PathVariable UUID incidentId,
            @Valid @RequestBody TransitionIncidentRequest request) {
        return IncidentResponse.from(incidents.transition(
                incidentId,
                new TransitionIncidentCommand(request.status(), request.expectedVersion().longValue())));
    }

    @GetMapping("/{incidentId}/timeline")
    @Operation(summary = "Read the immutable incident timeline")
    PageResponse<IncidentTimelineResponse> timeline(
            @PathVariable UUID incidentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(
                incidents.timeline(incidentId, pageable), IncidentTimelineResponse::from);
    }
}
