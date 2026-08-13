package dev.rheinops.platform.incident.internal;

import dev.rheinops.platform.incident.IncidentSeverity;
import dev.rheinops.platform.incident.IncidentStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    Page<IncidentEntity> findByStatus(IncidentStatus status, Pageable pageable);

    Page<IncidentEntity> findBySeverity(IncidentSeverity severity, Pageable pageable);

    Page<IncidentEntity> findByStatusAndSeverity(
            IncidentStatus status, IncidentSeverity severity, Pageable pageable);
}
