package dev.rheinops.platform.support.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SupportCaseDiagnosticRepository extends JpaRepository<SupportCaseDiagnosticEntity, UUID> {

    Page<SupportCaseDiagnosticEntity> findBySupportCaseId(UUID supportCaseId, Pageable pageable);
}
