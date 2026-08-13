package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.CustomerOrganisationSnapshot;
import dev.rheinops.platform.support.CustomerOrganisationStatus;
import java.time.Instant;
import java.util.UUID;

record CustomerOrganisationResponse(
        UUID id,
        String name,
        String slug,
        CustomerOrganisationStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    static CustomerOrganisationResponse from(CustomerOrganisationSnapshot snapshot) {
        return new CustomerOrganisationResponse(
                snapshot.id(), snapshot.name(), snapshot.slug(), snapshot.status(), snapshot.version(),
                snapshot.createdAt(), snapshot.updatedAt());
    }
}
