CREATE TABLE webhook_events (
                                id UUID PRIMARY KEY,
                                source VARCHAR(50) NOT NULL,
                                event_id VARCHAR(200) NOT NULL,
                                signature_valid BOOLEAN NOT NULL DEFAULT TRUE,
                                payload TEXT NOT NULL,
                                status VARCHAR(30) NOT NULL,
                                received_at TIMESTAMPTZ NOT NULL,
                                processed_at TIMESTAMPTZ NULL,
                                error_message TEXT NULL
);

CREATE UNIQUE INDEX ux_webhook_events_source_event
    ON webhook_events (source, event_id);

CREATE INDEX ix_webhook_events_status_received
    ON webhook_events (status, received_at);