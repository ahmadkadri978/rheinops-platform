package dev.rheinops.platform.incident.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface IncidentTimelineRepository extends JpaRepository<IncidentTimelineEntity, Long> {

    Page<IncidentTimelineEntity> findByIncidentId(UUID incidentId, Pageable pageable);
}
