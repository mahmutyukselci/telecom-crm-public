CREATE SCHEMA IF NOT EXISTS customer_schema;
CREATE TABLE IF NOT EXISTS customer_schema.customers (
                                                         id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(20) NOT NULL,
    keycloak_user_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP
    );