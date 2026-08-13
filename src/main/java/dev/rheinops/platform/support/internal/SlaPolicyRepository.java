package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.support.SupportPriority;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SlaPolicyRepository extends JpaRepository<SlaPolicyEntity, UUID> {

    Optional<SlaPolicyEntity> findByOrganisationIdAndPriorityAndActiveTrue(
            UUID organisationId, SupportPriority priority);

    Page<SlaPolicyEntity> findByOrganisationIdAndActiveTrue(UUID organisationId, Pageable pageable);
}
