package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.support.EscalationLevel;
import dev.rheinops.platform.support.SupportCaseStatus;
import dev.rheinops.platform.support.SupportPriority;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SupportCaseRepository
        extends JpaRepository<SupportCaseEntity, UUID>, JpaSpecificationExecutor<SupportCaseEntity> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE support_cases
            SET escalation_level = 'BREACHED',
                escalated_at = COALESCE(escalated_at, :now),
                updated_at = :now,
                version = version + 1
            WHERE status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER')
              AND escalation_level <> 'BREACHED'
              AND (
                  (first_responded_at IS NULL AND response_due_at <= :now)
                  OR resolution_due_at <= :now
                  OR (first_responded_at IS NOT NULL AND first_responded_at > response_due_at)
              )
            """, nativeQuery = true)
    int escalateBreached(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE support_cases
            SET escalation_level = 'WARNING',
                escalated_at = COALESCE(escalated_at, :now),
                updated_at = :now,
                version = version + 1
            WHERE status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER')
              AND escalation_level = 'NONE'
              AND (
                  (
                      first_responded_at IS NULL
                      AND :now >= created_at
                          + (response_due_at - created_at) * (warning_threshold_percent / 100.0)
                  )
                  OR
                  (
                      first_responded_at IS NOT NULL
                      AND first_responded_at <= response_due_at
                      AND :now >= created_at
                          + (resolution_due_at - created_at) * (warning_threshold_percent / 100.0)
                  )
              )
            """, nativeQuery = true)
    int escalateWarnings(@Param("now") Instant now);

    static Specification<SupportCaseEntity> matches(
            UUID organisationId,
            SupportCaseStatus status,
            SupportPriority priority,
            EscalationLevel escalationLevel) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (organisationId != null) {
                predicates.add(builder.equal(root.get("organisationId"), organisationId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(builder.equal(root.get("priority"), priority));
            }
            if (escalationLevel != null) {
                predicates.add(builder.equal(root.get("escalationLevel"), escalationLevel));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
