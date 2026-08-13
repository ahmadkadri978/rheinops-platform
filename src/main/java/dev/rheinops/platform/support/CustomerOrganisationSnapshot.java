package dev.rheinops.platform.support;

import java.time.Instant;
import java.util.UUID;

public record CustomerOrganisationSnapshot(
        UUID id,
        String name,
        String slug,
        CustomerOrganisationStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
