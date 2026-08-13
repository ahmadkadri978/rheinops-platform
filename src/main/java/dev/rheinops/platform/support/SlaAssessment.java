package dev.rheinops.platform.support;

import java.time.Instant;

public record SlaAssessment(
        SlaHealth health,
        SlaObjective objective,
        Instant nextDeadlineAt,
        long secondsRemaining) {}
