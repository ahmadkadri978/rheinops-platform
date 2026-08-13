package dev.rheinops.platform.servicecatalog.web;

import dev.rheinops.platform.servicecatalog.ServiceSnapshot;
import dev.rheinops.platform.servicecatalog.ServiceStatus;
import java.time.Instant;
import java.util.UUID;

record ServiceResponse(
        UUID id,
        String name,
        String slug,
        String description,
        ServiceStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    static ServiceResponse from(ServiceSnapshot service) {
        return new ServiceResponse(
                service.id(),
                service.name(),
                service.slug(),
                service.description(),
                service.status(),
                service.version(),
                service.createdAt(),
                service.updatedAt());
    }
}
