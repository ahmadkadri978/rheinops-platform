package dev.rheinops.platform.servicecatalog;

import java.time.Instant;
import java.util.UUID;

public record ServiceSnapshot(
        UUID id,
        String name,
        String slug,
        String description,
        ServiceStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
