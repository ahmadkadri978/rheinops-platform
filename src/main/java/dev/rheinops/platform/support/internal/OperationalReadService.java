package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.shared.InvalidRequestException;
import dev.rheinops.platform.support.OperationalExport;
import dev.rheinops.platform.support.OperationalReadOperations;
import dev.rheinops.platform.support.OperationalSearchHit;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class OperationalReadService implements OperationalReadOperations {

    private static final Duration MAX_EXPORT_WINDOW = Duration.ofDays(31);
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final OperationalSearchRepository searchRepository;
    private final OperationalExportRepository exportRepository;

    OperationalReadService(
            OperationalSearchRepository searchRepository,
            OperationalExportRepository exportRepository) {
        this.searchRepository = searchRepository;
        this.exportRepository = exportRepository;
    }

    @Override
    public Page<OperationalSearchHit> search(String query, Pageable pageable) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            throw new InvalidRequestException(
                    "search_query_too_short", "Search query must contain at least two characters.");
        }
        List<OperationalSearchHit> hits = searchRepository.search(
                normalized, pageable.getPageSize(), pageable.getOffset());
        return new PageImpl<>(hits, pageable, searchRepository.count(normalized));
    }

    @Override
    public OperationalExport export(Instant fromInclusive, Instant toExclusive) {
        validateWindow(fromInclusive, toExclusive);
        StringBuilder csv = new StringBuilder(
                "caseReference,organisationSlug,serviceSlug,incidentId,priority,status,"
                        + "slaHealth,escalationLevel,responseDueAt,resolutionDueAt,createdAt,resolvedAt\n");
        exportRepository.findRows(fromInclusive, toExclusive).forEach(row -> csv
                .append(csv(row.caseReference())).append(',')
                .append(csv(row.organisationSlug())).append(',')
                .append(csv(row.serviceSlug())).append(',')
                .append(csv(row.incidentId())).append(',')
                .append(csv(row.priority())).append(',')
                .append(csv(row.status())).append(',')
                .append(csv(row.slaHealth())).append(',')
                .append(csv(row.escalationLevel())).append(',')
                .append(csv(row.responseDueAt())).append(',')
                .append(csv(row.resolutionDueAt())).append(',')
                .append(csv(row.createdAt())).append(',')
                .append(csv(row.resolvedAt())).append('\n'));
        String filename = "rheinops-operational-export-%s.csv".formatted(FILE_TIME.format(fromInclusive));
        return new OperationalExport(filename, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void validateWindow(Instant fromInclusive, Instant toExclusive) {
        if (fromInclusive == null || toExclusive == null || !fromInclusive.isBefore(toExclusive)) {
            throw new InvalidRequestException(
                    "invalid_export_window", "Export start must be before export end.");
        }
        if (Duration.between(fromInclusive, toExclusive).compareTo(MAX_EXPORT_WINDOW) > 0) {
            throw new InvalidRequestException(
                    "export_window_too_large", "Operational exports are limited to 31 days.");
        }
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
