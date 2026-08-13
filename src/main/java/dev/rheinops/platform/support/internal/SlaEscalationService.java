package dev.rheinops.platform.support.internal;

import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SlaEscalationService {

    private final SupportCaseRepository cases;
    private final Clock clock;

    SlaEscalationService(SupportCaseRepository cases, Clock clock) {
        this.cases = cases;
        this.clock = clock;
    }

    @Transactional
    void refresh(Instant now) {
        cases.escalateBreached(now);
        cases.escalateWarnings(now);
    }

    @Scheduled(
            initialDelayString = "${rheinops.support.sla-evaluation-interval:60000}",
            fixedDelayString = "${rheinops.support.sla-evaluation-interval:60000}")
    @Transactional
    void refreshScheduled() {
        refresh(clock.instant());
    }
}
