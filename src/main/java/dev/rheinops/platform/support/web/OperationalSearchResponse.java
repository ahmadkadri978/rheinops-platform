package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.OperationalSearchHit;
import dev.rheinops.platform.support.SearchResultType;
import java.time.Instant;
import java.util.UUID;

record OperationalSearchResponse(
        SearchResultType type,
        UUID resourceId,
        UUID parentId,
        String parentReference,
        String headline,
        String snippet,
        Instant occurredAt,
        double relevance) {

    static OperationalSearchResponse from(OperationalSearchHit hit) {
        return new OperationalSearchResponse(
                hit.type(), hit.resourceId(), hit.parentId(), hit.parentReference(), hit.headline(),
                hit.snippet(), hit.occurredAt(), hit.relevance());
    }
}
