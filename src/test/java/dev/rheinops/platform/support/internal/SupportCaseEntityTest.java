package dev.rheinops.platform.support.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.rheinops.platform.shared.ConflictException;
import dev.rheinops.platform.shared.InvalidRequestException;
import dev.rheinops.platform.support.SlaHealth;
import dev.rheinops.platform.support.SlaObjective;
import dev.rheinops.platform.support.SupportCaseStatus;
import dev.rheinops.platform.support.SupportPriority;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupportCaseEntityTest {

    private static final Instant OPENED_AT = Instant.parse("2026-08-13T10:00:00Z");

    @Test
    void requiresResolutionNotesAndRejectsStaleUpdates() {
        SupportCaseEntity supportCase = supportCase(10, 60, 80);

        assertThatThrownBy(() -> supportCase.transitionTo(
                        SupportCaseStatus.RESOLVED, 0, null, OPENED_AT.plusSeconds(300)))
                .isInstanceOf(InvalidRequestException.class)
                .extracting(exception -> ((InvalidRequestException) exception).code())
                .isEqualTo("resolution_notes_required");

        assertThatThrownBy(() -> supportCase.transitionTo(
                        SupportCaseStatus.IN_PROGRESS, 1, null, OPENED_AT.plusSeconds(60)))
                .isInstanceOf(ConflictException.class)
                .extracting(exception -> ((ConflictException) exception).code())
                .isEqualTo("stale_support_case_version");
    }

    @Test
    void predictsRiskThenDetectsABreach() {
        SupportCaseEntity supportCase = supportCase(10, 60, 80);
        SlaCalculator calculator = new SlaCalculator();

        var atRisk = calculator.assess(supportCase, OPENED_AT.plusSeconds(8 * 60));
        var breached = calculator.assess(supportCase, OPENED_AT.plusSeconds(11 * 60));

        assertThat(atRisk.health()).isEqualTo(SlaHealth.AT_RISK);
        assertThat(atRisk.objective()).isEqualTo(SlaObjective.RESPONSE);
        assertThat(breached.health()).isEqualTo(SlaHealth.BREACHED);
    }

    @Test
    void keepsALateResponseBreachVisibleDuringResolutionWork() {
        SupportCaseEntity supportCase = supportCase(10, 60, 80);
        supportCase.transitionTo(
                SupportCaseStatus.IN_PROGRESS, 0, null, OPENED_AT.plusSeconds(11 * 60));

        var assessment = new SlaCalculator().assess(supportCase, OPENED_AT.plusSeconds(12 * 60));

        assertThat(assessment.health()).isEqualTo(SlaHealth.BREACHED);
        assertThat(assessment.objective()).isEqualTo(SlaObjective.RESPONSE);
    }

    @Test
    void reportsMetWhenResponseAndResolutionFinishInsideTheTargets() {
        SupportCaseEntity supportCase = supportCase(10, 60, 80);
        supportCase.transitionTo(
                SupportCaseStatus.RESOLVED,
                0,
                "Restarted the connection pool and verified recovery.",
                OPENED_AT.plusSeconds(5 * 60));

        var assessment = new SlaCalculator().assess(supportCase, OPENED_AT.plusSeconds(6 * 60));

        assertThat(assessment.health()).isEqualTo(SlaHealth.MET);
        assertThat(assessment.objective()).isEqualTo(SlaObjective.COMPLETE);
    }

    private static SupportCaseEntity supportCase(
            int responseMinutes, int resolutionMinutes, int warningThresholdPercent) {
        UUID organisationId = UUID.randomUUID();
        SlaPolicyEntity policy = SlaPolicyEntity.create(
                UUID.randomUUID(),
                organisationId,
                SupportPriority.P1,
                responseMinutes,
                resolutionMinutes,
                warningThresholdPercent,
                OPENED_AT);
        return SupportCaseEntity.create(
                UUID.randomUUID(),
                "SUP-20260813-ABC123",
                organisationId,
                UUID.randomUUID(),
                null,
                "Ada Customer",
                "ada@example.test",
                "Checkout is unavailable",
                "Customers receive a gateway error.",
                SupportPriority.P1,
                policy,
                "https://runbooks.example.test/checkout",
                OPENED_AT);
    }
}
