-- ==============================================================================
-- V3: PostgreSQL Declarative Table Partitioning for Subscription Event Store
-- Enables zero-bloat append-only Event Sourcing and sub-millisecond Partition Pruning
-- ==============================================================================

-- 1. Create Parent Partitioned Table
CREATE TABLE IF NOT EXISTS subscription_schema.subscription_event_store (
    id UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_payload TEXT NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_subscription_event_store PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

-- 2. Create Monthly Partitions
CREATE TABLE IF NOT EXISTS subscription_schema.subscription_event_store_2026_07
    PARTITION OF subscription_schema.subscription_event_store
    FOR VALUES FROM ('2026-07-01 00:00:00') TO ('2026-08-01 00:00:00');

CREATE TABLE IF NOT EXISTS subscription_schema.subscription_event_store_2026_08
    PARTITION OF subscription_schema.subscription_event_store
    FOR VALUES FROM ('2026-08-01 00:00:00') TO ('2026-09-01 00:00:00');

CREATE TABLE IF NOT EXISTS subscription_schema.subscription_event_store_2026_09
    PARTITION OF subscription_schema.subscription_event_store
    FOR VALUES FROM ('2026-09-01 00:00:00') TO ('2026-10-01 00:00:00');

CREATE TABLE IF NOT EXISTS subscription_schema.subscription_event_store_default
    PARTITION OF subscription_schema.subscription_event_store
    DEFAULT;

-- 3. High-Performance Composite Indexes for Point-in-Time Temporal Queries & Partition Pruning
CREATE INDEX IF NOT EXISTS idx_sub_event_store_aggregate_time
    ON subscription_schema.subscription_event_store (aggregate_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_sub_event_store_aggregate_version
    ON subscription_schema.subscription_event_store (aggregate_id, version);
