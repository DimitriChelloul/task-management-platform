CREATE TABLE IF NOT EXISTS outbox_events (
                                             id UUID PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_events(status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_outbox_created_at
    ON outbox_events(created_at);
