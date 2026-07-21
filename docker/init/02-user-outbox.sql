CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    producer VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NULL,
    last_error TEXT NULL
    );

ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS producer VARCHAR(100);
UPDATE outbox_events SET producer = 'user-service' WHERE producer IS NULL;
ALTER TABLE outbox_events ALTER COLUMN producer SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_events(producer, status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_outbox_created_at
    ON outbox_events(created_at);
