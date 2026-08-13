package dev.rheinops.platform.support;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OperationalReadOperations {

    Page<OperationalSearchHit> search(String query, Pageable pageable);

    OperationalExport export(Instant fromInclusive, Instant toExclusive);
}
