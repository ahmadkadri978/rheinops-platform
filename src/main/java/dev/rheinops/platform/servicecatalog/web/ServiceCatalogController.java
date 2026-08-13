package dev.rheinops.platform.servicecatalog.web;

import dev.rheinops.platform.servicecatalog.CreateServiceCommand;
import dev.rheinops.platform.servicecatalog.ServiceCatalog;
import dev.rheinops.platform.servicecatalog.ServiceSnapshot;
import dev.rheinops.platform.shared.PageResponse;
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
@RequestMapping("/api/v1/services")
@Tag(name = "Service Catalog")
@SecurityRequirement(name = "bearerAuth")
public class ServiceCatalogController {

    private final ServiceCatalog serviceCatalog;

    public ServiceCatalogController(ServiceCatalog serviceCatalog) {
        this.serviceCatalog = serviceCatalog;
    }

    @PostMapping
    @Operation(summary = "Register a managed service")
    ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest request) {
        ServiceSnapshot service = serviceCatalog.create(
                new CreateServiceCommand(request.name(), request.slug(), request.description()));
        return ResponseEntity.created(URI.create("/api/v1/services/" + service.id()))
                .body(ServiceResponse.from(service));
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "Get a managed service")
    ServiceResponse get(@PathVariable UUID serviceId) {
        return ServiceResponse.from(serviceCatalog.getRequired(serviceId));
    }

    @GetMapping
    @Operation(summary = "List managed services")
    PageResponse<ServiceResponse> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(serviceCatalog.findAll(pageable), ServiceResponse::from);
    }
}
