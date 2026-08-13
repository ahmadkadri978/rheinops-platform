CREATE TABLE incident_timeline_events (
    sequence_number BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor_subject TEXT NOT NULL,
    actor_display_name TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_timeline_incident
        FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT chk_timeline_event_type
        CHECK (event_type IN ('INCIDENT_CREATED', 'STATUS_CHANGED')),
    CONSTRAINT chk_timeline_previous_status
        CHECK (previous_status IS NULL OR previous_status IN (
            'OPEN', 'INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED'
        )),
    CONSTRAINT chk_timeline_new_status
        CHECK (new_status IN ('OPEN', 'INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED')),
    CONSTRAINT chk_timeline_event_shape CHECK (
        (event_type = 'INCIDENT_CREATED' AND previous_status IS NULL AND new_status = 'OPEN')
        OR
        (event_type = 'STATUS_CHANGED' AND previous_status IS NOT NULL AND previous_status <> new_status)
    ),
    CONSTRAINT chk_timeline_actor_subject
        CHECK (length(btrim(actor_subject)) > 0),
    CONSTRAINT chk_timeline_actor_display_name
        CHECK (length(btrim(actor_display_name)) > 0)
);

CREATE INDEX idx_timeline_incident_sequence
    ON incident_timeline_events (incident_id, sequence_number);

CREATE FUNCTION reject_incident_timeline_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'incident timeline events are append-only'
        USING ERRCODE = '23000';
END;
$$;

CREATE TRIGGER trg_incident_timeline_append_only
    BEFORE UPDATE OR DELETE ON incident_timeline_events
    FOR EACH ROW
    EXECUTE FUNCTION reject_incident_timeline_mutation();
