package dev.rheinops.platform.support.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class OperationalExportRepository {

    record ExportRow(
            String caseReference,
            String organisationSlug,
            String serviceSlug,
            String incidentId,
            String priority,
            String status,
            String slaHealth,
            String escalationLevel,
            Instant responseDueAt,
            Instant resolutionDueAt,
            Instant createdAt,
            Instant resolvedAt) {}

    private static final String SQL = """
            SELECT
                c.case_reference,
                o.slug AS organisation_slug,
                s.slug AS service_slug,
                c.incident_id,
                c.priority,
                c.status,
                CASE
                    WHEN c.status IN ('RESOLVED', 'CLOSED')
                        AND c.first_responded_at IS NOT NULL
                        AND c.first_responded_at <= c.response_due_at
                        AND c.resolved_at IS NOT NULL
                        AND c.resolved_at <= c.resolution_due_at THEN 'MET'
                    WHEN c.status IN ('RESOLVED', 'CLOSED') THEN 'BREACHED'
                    WHEN c.first_responded_at IS NULL AND CURRENT_TIMESTAMP > c.response_due_at
                        THEN 'BREACHED'
                    WHEN c.first_responded_at IS NOT NULL AND CURRENT_TIMESTAMP > c.resolution_due_at
                        THEN 'BREACHED'
                    ELSE 'ON_TRACK'
                END AS sla_health,
                c.escalation_level,
                c.response_due_at,
                c.resolution_due_at,
                c.created_at,
                c.resolved_at
            FROM support_cases c
            JOIN customer_organisations o ON o.id = c.organisation_id
            JOIN managed_services s ON s.id = c.service_id
            WHERE c.created_at >= ? AND c.created_at < ?
            ORDER BY c.created_at, c.id
            """;

    private final JdbcTemplate jdbcTemplate;

    OperationalExportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<ExportRow> findRows(Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.query(
                SQL,
                (resultSet, rowNumber) -> new ExportRow(
                        resultSet.getString("case_reference"),
                        resultSet.getString("organisation_slug"),
                        resultSet.getString("service_slug"),
                        resultSet.getString("incident_id"),
                        resultSet.getString("priority"),
                        resultSet.getString("status"),
                        resultSet.getString("sla_health"),
                        resultSet.getString("escalation_level"),
                        resultSet.getTimestamp("response_due_at").toInstant(),
                        resultSet.getTimestamp("resolution_due_at").toInstant(),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("resolved_at") == null
                                ? null
                                : resultSet.getTimestamp("resolved_at").toInstant()),
                java.sql.Timestamp.from(fromInclusive),
                java.sql.Timestamp.from(toExclusive));
    }
}
