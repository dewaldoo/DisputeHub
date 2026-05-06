-- V2__Seed_Data.sql
-- EXPLANATION: Seed initial data for development and testing
-- Creates default admin user and sample customer with test transactions

-- Insert default users
-- Admin password: Admin@123
-- Customer passwords: Password@123
-- SECURITY NOTE: Change these passwords in production!
INSERT INTO users (username, password, role, full_name, email, created_at)
VALUES
    ('admin@disputehub.com', '$2a$10$tLB4btqgcsgfVxQvK7FOdOhEoo0E1wGLFOSrkGh1EaXw.Mo1K6vFq', 'ADMIN', 'System Administrator', 'admin@disputehub.com', NOW()),
    ('john.doe@example.com', '$2a$10$VVLvFEesePR6Odw91anuu.0sfKkAtH7o6JGRH5Itb6G4Y1c9zk1XS', 'CUSTOMER', 'John Doe', 'john.doe@example.com', NOW()),
    ('jane.smith@example.com', '$2a$10$VVLvFEesePR6Odw91anuu.0sfKkAtH7o6JGRH5Itb6G4Y1c9zk1XS', 'CUSTOMER', 'Jane Smith', 'jane.smith@example.com', NOW());

-- Insert sample transactions for customer1 (user_id = 2)
INSERT INTO transactions (user_id, merchant_name, amount, transaction_date, category, description, reference_number, created_at)
VALUES
    (2, 'Amazon', 599.99, NOW() - INTERVAL '5 days', 'Online Shopping', 'Electronics purchase', 'TXN001234', NOW() - INTERVAL '5 days'),
    (2, 'Woolworths', 1250.50, NOW() - INTERVAL '10 days', 'Groceries', 'Weekly shopping', 'TXN001235', NOW() - INTERVAL '10 days'),
    (2, 'Netflix', 199.00, NOW() - INTERVAL '15 days', 'Entertainment', 'Monthly subscription', 'TXN001236', NOW() - INTERVAL '15 days'),
    (2, 'Shell', 750.00, NOW() - INTERVAL '3 days', 'Transport', 'Fuel purchase', 'TXN001237', NOW() - INTERVAL '3 days'),
    (2, 'Takealot', 2500.00, NOW() - INTERVAL '7 days', 'Online Shopping', 'Laptop accessories', 'TXN001238', NOW() - INTERVAL '7 days');

-- Insert sample transactions for customer2 (user_id = 3)
INSERT INTO transactions (user_id, merchant_name, amount, transaction_date, category, description, reference_number, created_at)
VALUES
    (3, 'Pick n Pay', 850.75, NOW() - INTERVAL '4 days', 'Groceries', 'Grocery shopping', 'TXN001239', NOW() - INTERVAL '4 days'),
    (3, 'Uber', 125.00, NOW() - INTERVAL '2 days', 'Transport', 'Ride to work', 'TXN001240', NOW() - INTERVAL '2 days'),
    (3, 'Spotify', 59.99, NOW() - INTERVAL '8 days', 'Entertainment', 'Music subscription', 'TXN001241', NOW() - INTERVAL '8 days');

-- Insert a sample dispute for customer1
INSERT INTO disputes (transaction_id, user_id, reason, description, status, created_at, updated_at)
VALUES
    (1, 2, 'UNAUTHORIZED', 'I did not authorize this purchase. My card was in my possession at the time.', 'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

-- Insert audit log for the dispute
INSERT INTO audit_logs (dispute_id, actor_id, action, old_value, new_value, notes, timestamp)
VALUES
    (1, 2, 'CREATED', NULL, 'PENDING', 'Dispute submitted for transaction TXN001234', NOW() - INTERVAL '2 days');
