package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.support.OperationalSearchHit;
import dev.rheinops.platform.support.SearchResultType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class OperationalSearchRepository {

    private static final String SEARCH_SQL = """
            WITH search_query AS (
                SELECT websearch_to_tsquery('english', ?) AS query
            ), ranked_results AS (
                SELECT
                    'INCIDENT' AS result_type,
                    i.id AS resource_id,
                    NULL::uuid AS parent_id,
                    NULL::varchar AS parent_reference,
                    i.title AS headline,
                    ts_headline(
                        'english',
                        i.description,
                        search_query.query,
                        'MaxFragments=1, MaxWords=24, MinWords=8, StartSel=[[, StopSel=]]'
                    ) AS snippet,
                    i.reported_at AS occurred_at,
                    ts_rank_cd(i.search_document, search_query.query) AS relevance
                FROM incidents i
                CROSS JOIN search_query
                WHERE i.search_document @@ search_query.query
                UNION ALL
                SELECT
                    'DIAGNOSTIC' AS result_type,
                    d.id AS resource_id,
                    d.support_case_id AS parent_id,
                    c.case_reference AS parent_reference,
                    d.summary AS headline,
                    ts_headline(
                        'english',
                        d.details,
                        search_query.query,
                        'MaxFragments=1, MaxWords=24, MinWords=8, StartSel=[[, StopSel=]]'
                    ) AS snippet,
                    d.recorded_at AS occurred_at,
                    ts_rank_cd(d.search_document, search_query.query) AS relevance
                FROM support_case_diagnostics d
                JOIN support_cases c ON c.id = d.support_case_id
                CROSS JOIN search_query
                WHERE d.search_document @@ search_query.query
            )
            SELECT result_type, resource_id, parent_id, parent_reference,
                   headline, snippet, occurred_at, relevance
            FROM ranked_results
            ORDER BY relevance DESC, occurred_at DESC, resource_id
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_SQL = """
            WITH search_query AS (
                SELECT websearch_to_tsquery('english', ?) AS query
            )
            SELECT
                (SELECT count(*) FROM incidents i, search_query
                    WHERE i.search_document @@ search_query.query)
                +
                (SELECT count(*) FROM support_case_diagnostics d, search_query
                    WHERE d.search_document @@ search_query.query)
            """;

    private final JdbcTemplate jdbcTemplate;

    OperationalSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<OperationalSearchHit> search(String query, int limit, long offset) {
        return jdbcTemplate.query(
                SEARCH_SQL,
                (resultSet, rowNumber) -> map(resultSet),
                query,
                limit,
                offset);
    }

    long count(String query) {
        Long count = jdbcTemplate.queryForObject(COUNT_SQL, Long.class, query);
        return count == null ? 0 : count;
    }

    private static OperationalSearchHit map(ResultSet resultSet) throws SQLException {
        Timestamp occurredAt = resultSet.getTimestamp("occurred_at");
        return new OperationalSearchHit(
                SearchResultType.valueOf(resultSet.getString("result_type")),
                resultSet.getObject("resource_id", UUID.class),
                resultSet.getObject("parent_id", UUID.class),
                resultSet.getString("parent_reference"),
                resultSet.getString("headline"),
                resultSet.getString("snippet"),
                occurredAt == null ? Instant.EPOCH : occurredAt.toInstant(),
                resultSet.getDouble("relevance"));
    }
}
