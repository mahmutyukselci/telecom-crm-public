CREATE SCHEMA IF NOT EXISTS notification_schema;

CREATE TABLE IF NOT EXISTS notification_schema.processed_events (
                                                                    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
    );


CREATE TABLE IF NOT EXISTS notification_schema.notification_logs (
                                                 id SERIAL PRIMARY KEY,
                                                 subscription_id VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );