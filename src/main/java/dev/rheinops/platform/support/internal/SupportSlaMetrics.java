package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.support.EscalationLevel;
import dev.rheinops.platform.support.SupportCaseStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.transaction.annotation.Transactional;

@Component
class SupportSlaMetrics {

    private static final List<SupportCaseStatus> ACTIVE_STATUSES = List.of(
            SupportCaseStatus.OPEN,
            SupportCaseStatus.IN_PROGRESS,
            SupportCaseStatus.WAITING_FOR_CUSTOMER);

    private final SupportCaseRepository cases;
    private final Map<EscalationLevel, AtomicLong> activeCases =
            new EnumMap<>(EscalationLevel.class);

    SupportSlaMetrics(SupportCaseRepository cases, MeterRegistry registry) {
        this.cases = cases;
        for (EscalationLevel level : EscalationLevel.values()) {
            AtomicLong value = new AtomicLong();
            activeCases.put(level, value);
            Gauge.builder("rheinops.support.active.cases", value, AtomicLong::get)
                    .description("Active support cases grouped by persisted SLA escalation level")
                    .tag("escalation", level.name().toLowerCase(Locale.ROOT))
                    .register(registry);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    void refreshWhenReady() {
        refresh();
    }

    @Scheduled(
            initialDelayString = "${rheinops.support.metrics-refresh-interval:15000}",
            fixedDelayString = "${rheinops.support.metrics-refresh-interval:15000}")
    @Transactional(readOnly = true)
    void refreshScheduled() {
        refresh();
    }

    private void refresh() {
        activeCases.forEach((level, value) ->
                value.set(cases.countByStatusInAndEscalationLevel(ACTIVE_STATUSES, level)));
    }
}
