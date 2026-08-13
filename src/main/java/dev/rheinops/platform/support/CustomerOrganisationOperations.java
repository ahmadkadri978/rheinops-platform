package dev.rheinops.platform.support;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerOrganisationOperations {

    CustomerOrganisationSnapshot create(CreateCustomerOrganisationCommand command);

    CustomerOrganisationSnapshot getRequired(UUID organisationId);

    Page<CustomerOrganisationSnapshot> findAll(Pageable pageable);

    SlaPolicySnapshot createPolicy(UUID organisationId, CreateSlaPolicyCommand command);

    Page<SlaPolicySnapshot> findPolicies(UUID organisationId, Pageable pageable);
}
