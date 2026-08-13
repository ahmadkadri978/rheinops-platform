package dev.rheinops.platform.servicecatalog;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceCatalog {

    ServiceSnapshot create(CreateServiceCommand command);

    ServiceSnapshot getRequired(UUID serviceId);

    Page<ServiceSnapshot> findAll(Pageable pageable);
}
