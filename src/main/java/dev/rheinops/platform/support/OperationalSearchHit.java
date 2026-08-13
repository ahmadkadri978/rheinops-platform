package dev.rheinops.platform.support;

import java.time.Instant;
import java.util.UUID;

public record OperationalSearchHit(
        SearchResultType type,
        UUID resourceId,
        UUID parentId,
        String parentReference,
        String headline,
        String snippet,
        Instant occurredAt,
        double relevance) {}
