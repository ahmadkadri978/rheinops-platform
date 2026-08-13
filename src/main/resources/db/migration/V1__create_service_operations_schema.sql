CREATE TABLE managed_services (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_managed_service_status CHECK (
        status IN ('OPERATIONAL', 'DEGRADED', 'PARTIAL_OUTAGE', 'MAJOR_OUTAGE', 'MAINTENANCE')
    )
);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    affected_service_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    reported_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_incident_service
        FOREIGN KEY (affected_service_id) REFERENCES managed_services (id),
    CONSTRAINT chk_incident_severity CHECK (severity IN ('SEV1', 'SEV2', 'SEV3', 'SEV4')),
    CONSTRAINT chk_incident_status CHECK (
        status IN ('OPEN', 'INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED')
    )
);

CREATE INDEX idx_incidents_status_reported_at
    ON incidents (status, reported_at DESC);

CREATE INDEX idx_incidents_service
    ON incidents (affected_service_id);
