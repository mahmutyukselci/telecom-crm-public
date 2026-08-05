-- 1. Subscriptions Table
CREATE SCHEMA IF NOT EXISTS subscription_schema;

CREATE TABLE IF NOT EXISTS subscription_schema.subscriptions (
                                                                 id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    tariff_id VARCHAR(50) NOT NULL,
    keycloak_user_id VARCHAR(255),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    status VARCHAR(50) NOT NULL
    );

-- Critical indexes for fast querying (performance)
CREATE INDEX IF NOT EXISTS idx_subscriptions_customer_id ON subscription_schema.subscriptions(customer_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscription_schema.subscriptions(status);


-- 2. Subscription Add-ons Table
CREATE TABLE IF NOT EXISTS subscription_schema.subscription_addons (
                                                                       id VARCHAR(36) PRIMARY KEY,
    subscription_id VARCHAR(36) NOT NULL,
    tariff_id VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
    );

-- Critical indexes for fast querying (performance)
CREATE INDEX IF NOT EXISTS idx_subscription_addons_sub_id ON subscription_schema.subscription_addons(subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscription_addons_status ON subscription_schema.subscription_addons(status);