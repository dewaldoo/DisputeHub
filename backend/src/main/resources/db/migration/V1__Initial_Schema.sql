-- V1__Initial_Schema.sql
-- EXPLANATION: Initial database schema for DisputeHub
-- Creates tables for users, transactions, disputes, and audit logs

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    full_name VARCHAR(255),
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    category VARCHAR(255),
    description VARCHAR(255),
    reference_number VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create indexes for transactions
CREATE INDEX idx_transaction_user_id ON transactions(user_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_transaction_user_date ON transactions(user_id, transaction_date);

-- Disputes table
CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'MERCHANT_CONTACTED', 'RESOLVED', 'REJECTED')),
    resolution_notes VARCHAR(1000),
    evidence_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_dispute_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_dispute_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'MERCHANT_CONTACTED', 'RESOLVED', 'REJECTED'))
);

-- Create indexes for disputes
CREATE INDEX idx_dispute_user_id ON disputes(user_id);
CREATE INDEX idx_dispute_status ON disputes(status);
CREATE INDEX idx_dispute_created_at ON disputes(created_at);
CREATE INDEX idx_dispute_transaction_id ON disputes(transaction_id);

-- Audit logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    dispute_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    notes VARCHAR(500),
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_log_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(id),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

-- Create indexes for audit logs
CREATE INDEX idx_audit_log_dispute_id ON audit_logs(dispute_id);
CREATE INDEX idx_audit_log_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_log_timestamp ON audit_logs(timestamp);

-- Comments explaining the schema:
-- 1. Users: Stores both customers and admin users with role-based access
-- 2. Transactions: Bank transactions that can be disputed by customers
-- 3. Disputes: Customer challenges to transactions with status tracking
-- 4. Audit Logs: Complete audit trail of all dispute status changes
