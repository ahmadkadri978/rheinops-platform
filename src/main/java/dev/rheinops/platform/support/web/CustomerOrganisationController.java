package dev.rheinops.platform.support.web;

import dev.rheinops.platform.shared.PageResponse;
import dev.rheinops.platform.support.CreateCustomerOrganisationCommand;
import dev.rheinops.platform.support.CreateSlaPolicyCommand;
import dev.rheinops.platform.support.CustomerOrganisationOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-organisations")
@Tag(name = "Customer Organisations")
@SecurityRequirement(name = "bearerAuth")
public class CustomerOrganisationController {

    private final CustomerOrganisationOperations organisations;

    public CustomerOrganisationController(CustomerOrganisationOperations organisations) {
        this.organisations = organisations;
    }

    @PostMapping
    @Operation(summary = "Register a customer organisation")
    ResponseEntity<CustomerOrganisationResponse> create(
            @Valid @RequestBody CreateCustomerOrganisationRequest request) {
        var organisation = organisations.create(
                new CreateCustomerOrganisationCommand(request.name(), request.slug()));
        return ResponseEntity.created(URI.create("/api/v1/customer-organisations/" + organisation.id()))
                .body(CustomerOrganisationResponse.from(organisation));
    }

    @GetMapping("/{organisationId}")
    @Operation(summary = "Get a customer organisation")
    CustomerOrganisationResponse get(@PathVariable UUID organisationId) {
        return CustomerOrganisationResponse.from(organisations.getRequired(organisationId));
    }

    @GetMapping
    @Operation(summary = "List customer organisations")
    PageResponse<CustomerOrganisationResponse> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(organisations.findAll(pageable), CustomerOrganisationResponse::from);
    }

    @PostMapping("/{organisationId}/sla-policies")
    @Operation(summary = "Create the active SLA policy for a priority")
    ResponseEntity<SlaPolicyResponse> createPolicy(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateSlaPolicyRequest request) {
        var policy = organisations.createPolicy(
                organisationId,
                new CreateSlaPolicyCommand(
                        request.priority(), request.responseTargetMinutes(),
                        request.resolutionTargetMinutes(), request.warningThresholdPercent()));
        return ResponseEntity.created(URI.create(
                        "/api/v1/customer-organisations/%s/sla-policies/%s"
                                .formatted(organisationId, policy.id())))
                .body(SlaPolicyResponse.from(policy));
    }

    @GetMapping("/{organisationId}/sla-policies")
    @Operation(summary = "List active SLA policies")
    PageResponse<SlaPolicyResponse> findPolicies(
            @PathVariable UUID organisationId,
            @PageableDefault(size = 20, sort = "priority") Pageable pageable) {
        return PageResponse.from(
                organisations.findPolicies(organisationId, pageable), SlaPolicyResponse::from);
    }
}
