package dev.rheinops.platform.incident.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.rheinops.platform.incident.IncidentSeverity;
import dev.rheinops.platform.incident.IncidentStatus;
import dev.rheinops.platform.shared.ConflictException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentEntityTest {

    private static final Instant REPORTED_AT = Instant.parse("2026-01-10T10:15:30Z");

    @Test
    void createsAnOpenIncident() {
        IncidentEntity incident = newIncident();

        assertThat(incident.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.severity()).isEqualTo(IncidentSeverity.SEV2);
        assertThat(incident.version()).isZero();
        assertThat(incident.reportedAt()).isEqualTo(REPORTED_AT);
        assertThat(incident.resolvedAt()).isNull();
    }

    @Test
    void followsTheSupportedLifecycleAndRecordsResolutionTime() {
        IncidentEntity incident = newIncident();
        Instant investigatingAt = REPORTED_AT.plusSeconds(60);
        Instant resolvedAt = investigatingAt.plusSeconds(300);

        incident.transitionTo(IncidentStatus.INVESTIGATING, 0, investigatingAt);
        incident.transitionTo(IncidentStatus.RESOLVED, 0, resolvedAt);

        assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(incident.updatedAt()).isEqualTo(resolvedAt);
        assertThat(incident.resolvedAt()).isEqualTo(resolvedAt);
    }

    @Test
    void rejectsUnsupportedLifecycleTransitions() {
        IncidentEntity incident = newIncident();

        assertThatThrownBy(() ->
                        incident.transitionTo(IncidentStatus.IDENTIFIED, 0, REPORTED_AT.plusSeconds(60)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot transition");
    }

    @Test
    void rejectsAStaleExpectedVersion() {
        IncidentEntity incident = newIncident();

        assertThatThrownBy(() ->
                        incident.transitionTo(IncidentStatus.INVESTIGATING, 7, REPORTED_AT.plusSeconds(60)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("expected 7");
    }

    private IncidentEntity newIncident() {
        return IncidentEntity.create(
                UUID.randomUUID(),
                "Login requests are timing out",
                "Customers cannot sign in.",
                IncidentSeverity.SEV2,
                UUID.randomUUID(),
                REPORTED_AT);
    }
}
