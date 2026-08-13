package dev.rheinops.platform.support.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CustomerOrganisationRepository extends JpaRepository<CustomerOrganisationEntity, UUID> {}
