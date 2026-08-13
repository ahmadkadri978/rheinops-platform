package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.support.SlaAssessment;
import dev.rheinops.platform.support.SlaHealth;
import dev.rheinops.platform.support.SlaObjective;
import dev.rheinops.platform.support.SupportCaseStatus;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class SlaCalculator {

    SlaAssessment assess(SupportCaseEntity supportCase, Instant now) {
        if (supportCase.status() == SupportCaseStatus.RESOLVED
                || supportCase.status() == SupportCaseStatus.CLOSED) {
            return completedAssessment(supportCase);
        }
        if (supportCase.firstRespondedAt() != null
                && supportCase.firstRespondedAt().isAfter(supportCase.responseDueAt())) {
            return new SlaAssessment(
                    SlaHealth.BREACHED,
                    SlaObjective.RESPONSE,
                    supportCase.responseDueAt(),
                    Duration.between(now, supportCase.responseDueAt()).getSeconds());
        }
        if (supportCase.firstRespondedAt() == null) {
            return activeAssessment(
                    supportCase.createdAt(),
                    supportCase.responseDueAt(),
                    supportCase.warningThresholdPercent(),
                    SlaObjective.RESPONSE,
                    now);
        }
        return activeAssessment(
                supportCase.createdAt(),
                supportCase.resolutionDueAt(),
                supportCase.warningThresholdPercent(),
                SlaObjective.RESOLUTION,
                now);
    }

    private static SlaAssessment completedAssessment(SupportCaseEntity supportCase) {
        Instant respondedAt = supportCase.firstRespondedAt();
        Instant resolvedAt = supportCase.resolvedAt();
        boolean responseMet = respondedAt != null && !respondedAt.isAfter(supportCase.responseDueAt());
        boolean resolutionMet = resolvedAt != null && !resolvedAt.isAfter(supportCase.resolutionDueAt());
        SlaHealth health = responseMet && resolutionMet ? SlaHealth.MET : SlaHealth.BREACHED;
        return new SlaAssessment(health, SlaObjective.COMPLETE, null, 0);
    }

    private static SlaAssessment activeAssessment(
            Instant startedAt,
            Instant dueAt,
            int warningThresholdPercent,
            SlaObjective objective,
            Instant now) {
        long remaining = Duration.between(now, dueAt).getSeconds();
        if (remaining <= 0) {
            return new SlaAssessment(SlaHealth.BREACHED, objective, dueAt, remaining);
        }
        long budget = Math.max(1, Duration.between(startedAt, dueAt).getSeconds());
        long elapsed = Math.max(0, Duration.between(startedAt, now).getSeconds());
        int consumedPercent = (int) Math.min(100, (elapsed * 100) / budget);
        SlaHealth health = consumedPercent >= warningThresholdPercent
                ? SlaHealth.AT_RISK
                : SlaHealth.ON_TRACK;
        return new SlaAssessment(health, objective, dueAt, remaining);
    }
}
