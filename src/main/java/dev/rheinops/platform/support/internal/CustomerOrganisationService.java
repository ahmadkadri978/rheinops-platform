package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.shared.ResourceNotFoundException;
import dev.rheinops.platform.support.CreateCustomerOrganisationCommand;
import dev.rheinops.platform.support.CreateSlaPolicyCommand;
import dev.rheinops.platform.support.CustomerOrganisationOperations;
import dev.rheinops.platform.support.CustomerOrganisationSnapshot;
import dev.rheinops.platform.support.SlaPolicySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class CustomerOrganisationService implements CustomerOrganisationOperations {

    private final CustomerOrganisationRepository organisations;
    private final SlaPolicyRepository policies;
    private final Clock clock;

    CustomerOrganisationService(
            CustomerOrganisationRepository organisations,
            SlaPolicyRepository policies,
            Clock clock) {
        this.organisations = organisations;
        this.policies = policies;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CustomerOrganisationSnapshot create(CreateCustomerOrganisationCommand command) {
        Instant now = clock.instant();
        CustomerOrganisationEntity organisation = CustomerOrganisationEntity.create(
                UUID.randomUUID(), command.name().trim(), command.slug().trim().toLowerCase(), now);
        return toSnapshot(organisations.save(organisation));
    }

    @Override
    public CustomerOrganisationSnapshot getRequired(UUID organisationId) {
        return toSnapshot(findEntity(organisationId));
    }

    @Override
    public Page<CustomerOrganisationSnapshot> findAll(Pageable pageable) {
        return organisations.findAll(pageable).map(CustomerOrganisationService::toSnapshot);
    }

    @Override
    @Transactional
    public SlaPolicySnapshot createPolicy(UUID organisationId, CreateSlaPolicyCommand command) {
        findEntity(organisationId);
        if (command.resolutionTargetMinutes() < command.responseTargetMinutes()) {
            throw new dev.rheinops.platform.shared.InvalidRequestException(
                    "invalid_sla_targets",
                    "Resolution target must be greater than or equal to the response target.");
        }
        Instant now = clock.instant();
        policies.findByOrganisationIdAndPriorityAndActiveTrue(organisationId, command.priority())
                .ifPresent(existing -> {
                    existing.deactivate();
                    policies.save(existing);
                    policies.flush();
                });
        SlaPolicyEntity policy = SlaPolicyEntity.create(
                UUID.randomUUID(),
                organisationId,
                command.priority(),
                command.responseTargetMinutes(),
                command.resolutionTargetMinutes(),
                command.warningThresholdPercent(),
                now);
        return toSnapshot(policies.save(policy));
    }

    @Override
    public Page<SlaPolicySnapshot> findPolicies(UUID organisationId, Pageable pageable) {
        findEntity(organisationId);
        return policies.findByOrganisationIdAndActiveTrue(organisationId, pageable)
                .map(CustomerOrganisationService::toSnapshot);
    }

    private CustomerOrganisationEntity findEntity(UUID organisationId) {
        return organisations.findById(organisationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer_organisation_not_found",
                        "Customer organisation %s was not found.".formatted(organisationId)));
    }

    private static CustomerOrganisationSnapshot toSnapshot(CustomerOrganisationEntity organisation) {
        return new CustomerOrganisationSnapshot(
                organisation.id(),
                organisation.name(),
                organisation.slug(),
                organisation.status(),
                organisation.version(),
                organisation.createdAt(),
                organisation.updatedAt());
    }

    private static SlaPolicySnapshot toSnapshot(SlaPolicyEntity policy) {
        return new SlaPolicySnapshot(
                policy.id(),
                policy.organisationId(),
                policy.priority(),
                policy.responseTargetMinutes(),
                policy.resolutionTargetMinutes(),
                policy.warningThresholdPercent(),
                policy.createdAt());
    }
}
