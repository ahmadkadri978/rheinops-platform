CREATE TABLE customer_organisations (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_customer_organisation_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_customer_organisation_name
        CHECK (length(btrim(name)) > 0),
    CONSTRAINT chk_customer_organisation_slug
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

ALTER TABLE incidents
    ADD CONSTRAINT uq_incident_service UNIQUE (id, affected_service_id);

CREATE TABLE sla_policies (
    id UUID PRIMARY KEY,
    organisation_id UUID NOT NULL,
    priority VARCHAR(8) NOT NULL,
    response_target_minutes INTEGER NOT NULL,
    resolution_target_minutes INTEGER NOT NULL,
    warning_threshold_percent INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sla_policy_scope UNIQUE (id, organisation_id, priority),
    CONSTRAINT fk_sla_policy_organisation
        FOREIGN KEY (organisation_id) REFERENCES customer_organisations (id),
    CONSTRAINT chk_sla_policy_priority CHECK (priority IN ('P1', 'P2', 'P3', 'P4')),
    CONSTRAINT chk_sla_policy_response CHECK (response_target_minutes BETWEEN 1 AND 10080),
    CONSTRAINT chk_sla_policy_resolution CHECK (
        resolution_target_minutes BETWEEN 1 AND 43200
        AND resolution_target_minutes >= response_target_minutes
    ),
    CONSTRAINT chk_sla_policy_warning CHECK (warning_threshold_percent BETWEEN 1 AND 99)
);

CREATE UNIQUE INDEX uq_active_sla_policy_priority
    ON sla_policies (organisation_id, priority)
    WHERE active;

CREATE TABLE support_cases (
    id UUID PRIMARY KEY,
    case_reference VARCHAR(32) NOT NULL UNIQUE,
    organisation_id UUID NOT NULL,
    service_id UUID NOT NULL,
    incident_id UUID,
    requester_name VARCHAR(120) NOT NULL,
    requester_email VARCHAR(254) NOT NULL,
    subject VARCHAR(180) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    priority VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sla_policy_id UUID NOT NULL,
    response_due_at TIMESTAMPTZ NOT NULL,
    resolution_due_at TIMESTAMPTZ NOT NULL,
    warning_threshold_percent INTEGER NOT NULL,
    escalation_level VARCHAR(16) NOT NULL DEFAULT 'NONE',
    escalated_at TIMESTAMPTZ,
    first_responded_at TIMESTAMPTZ,
    runbook_url VARCHAR(500),
    resolution_notes VARCHAR(4000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_support_case_organisation
        FOREIGN KEY (organisation_id) REFERENCES customer_organisations (id),
    CONSTRAINT fk_support_case_service
        FOREIGN KEY (service_id) REFERENCES managed_services (id),
    CONSTRAINT fk_support_case_incident_service
        FOREIGN KEY (incident_id, service_id) REFERENCES incidents (id, affected_service_id),
    CONSTRAINT fk_support_case_sla_scope
        FOREIGN KEY (sla_policy_id, organisation_id, priority)
        REFERENCES sla_policies (id, organisation_id, priority),
    CONSTRAINT chk_support_case_priority CHECK (priority IN ('P1', 'P2', 'P3', 'P4')),
    CONSTRAINT chk_support_case_status CHECK (
        status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED')
    ),
    CONSTRAINT chk_support_case_escalation CHECK (
        escalation_level IN ('NONE', 'WARNING', 'BREACHED')
    ),
    CONSTRAINT chk_support_case_requester_name CHECK (length(btrim(requester_name)) > 0),
    CONSTRAINT chk_support_case_requester_email CHECK (length(btrim(requester_email)) > 3),
    CONSTRAINT chk_support_case_subject CHECK (length(btrim(subject)) > 0),
    CONSTRAINT chk_support_case_description CHECK (length(btrim(description)) > 0),
    CONSTRAINT chk_support_case_deadlines CHECK (
        response_due_at > created_at AND resolution_due_at >= response_due_at
    ),
    CONSTRAINT chk_support_case_warning CHECK (warning_threshold_percent BETWEEN 1 AND 99),
    CONSTRAINT chk_support_case_runbook CHECK (
        runbook_url IS NULL OR runbook_url LIKE 'https://%'
    ),
    CONSTRAINT chk_support_case_resolution CHECK (
        status NOT IN ('RESOLVED', 'CLOSED')
        OR (resolved_at IS NOT NULL AND length(btrim(resolution_notes)) > 0)
    )
);

CREATE INDEX idx_support_cases_queue
    ON support_cases (status, priority, response_due_at, resolution_due_at);

CREATE INDEX idx_support_cases_organisation_created
    ON support_cases (organisation_id, created_at DESC);

CREATE INDEX idx_support_cases_incident
    ON support_cases (incident_id)
    WHERE incident_id IS NOT NULL;

CREATE TABLE support_case_diagnostics (
    id UUID PRIMARY KEY,
    support_case_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    summary VARCHAR(240) NOT NULL,
    details VARCHAR(8000) NOT NULL,
    actor_subject TEXT NOT NULL,
    actor_display_name TEXT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    search_document TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(summary, '')), 'A')
        || setweight(to_tsvector('english', coalesce(details, '')), 'B')
    ) STORED,
    CONSTRAINT fk_support_diagnostic_case
        FOREIGN KEY (support_case_id) REFERENCES support_cases (id),
    CONSTRAINT chk_support_diagnostic_type CHECK (
        type IN ('CUSTOMER_REPORT', 'LOG_EXCERPT', 'METRIC_OBSERVATION', 'INVESTIGATION_NOTE')
    ),
    CONSTRAINT chk_support_diagnostic_summary CHECK (length(btrim(summary)) > 0),
    CONSTRAINT chk_support_diagnostic_details CHECK (length(btrim(details)) > 0),
    CONSTRAINT chk_support_diagnostic_actor CHECK (
        length(btrim(actor_subject)) > 0 AND length(btrim(actor_display_name)) > 0
    )
);

CREATE INDEX idx_support_diagnostics_case_recorded
    ON support_case_diagnostics (support_case_id, recorded_at);

CREATE INDEX idx_support_diagnostics_search
    ON support_case_diagnostics USING GIN (search_document);

CREATE FUNCTION reject_support_diagnostic_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'support case diagnostics are append-only'
        USING ERRCODE = '23000';
END;
$$;

CREATE TRIGGER trg_support_diagnostics_append_only
    BEFORE UPDATE OR DELETE ON support_case_diagnostics
    FOR EACH ROW
    EXECUTE FUNCTION reject_support_diagnostic_mutation();

ALTER TABLE incidents ADD COLUMN search_document TSVECTOR GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A')
    || setweight(to_tsvector('english', coalesce(description, '')), 'B')
) STORED;

CREATE INDEX idx_incidents_search
    ON incidents USING GIN (search_document);
