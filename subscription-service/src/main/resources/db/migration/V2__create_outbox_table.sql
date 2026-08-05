-- V2__create_outbox_table.sql
CREATE TABLE IF NOT EXISTS subscription_schema.outbox_events (
                                                                 id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT DEFAULT 0 NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created
    ON subscription_schema.outbox_events(status, created_at);