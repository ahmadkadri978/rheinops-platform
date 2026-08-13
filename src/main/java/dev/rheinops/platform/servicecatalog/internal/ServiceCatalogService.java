package dev.rheinops.platform.servicecatalog.internal;

import dev.rheinops.platform.servicecatalog.CreateServiceCommand;
import dev.rheinops.platform.servicecatalog.ServiceCatalog;
import dev.rheinops.platform.servicecatalog.ServiceSnapshot;
import dev.rheinops.platform.shared.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ServiceCatalogService implements ServiceCatalog {

    private final ManagedServiceRepository repository;
    private final Clock clock;

    ServiceCatalogService(ManagedServiceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ServiceSnapshot create(CreateServiceCommand command) {
        Instant now = clock.instant();
        ManagedServiceEntity service = ManagedServiceEntity.create(
                UUID.randomUUID(),
                command.name().trim(),
                command.slug().trim().toLowerCase(),
                normalizeNullable(command.description()),
                now);
        return toSnapshot(repository.save(service));
    }

    @Override
    public ServiceSnapshot getRequired(UUID serviceId) {
        return repository
                .findById(serviceId)
                .map(ServiceCatalogService::toSnapshot)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "service_not_found", "Managed service %s was not found.".formatted(serviceId)));
    }

    @Override
    public Page<ServiceSnapshot> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(ServiceCatalogService::toSnapshot);
    }

    private static ServiceSnapshot toSnapshot(ManagedServiceEntity service) {
        return new ServiceSnapshot(
                service.id(),
                service.name(),
                service.slug(),
                service.description(),
                service.status(),
                service.version(),
                service.createdAt(),
                service.updatedAt());
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
