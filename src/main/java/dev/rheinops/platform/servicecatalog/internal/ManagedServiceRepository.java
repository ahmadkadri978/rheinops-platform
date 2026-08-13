package dev.rheinops.platform.servicecatalog.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ManagedServiceRepository extends JpaRepository<ManagedServiceEntity, UUID> {}
