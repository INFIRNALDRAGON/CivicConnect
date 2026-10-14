-- V2 - controlled category list (reference data). Edit names to match your
-- approved scope (PED Section 2.1). Thresholds use the V1 default (72h) until
-- the team agrees real values with the stakeholders.
INSERT INTO categories (name) VALUES
    ('Facility fault'),
    ('Damaged equipment'),
    ('Security concern'),
    ('IT support'),
    ('Maintenance'),
    ('Lost property'),
    ('Other')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users
(name, email, password_hash, role)
VALUES
('Admin User', 'admin@civicconnect.co.za', '$2a$10$adminhash', 'MANAGER'),

('John Staff', 'john.staff@civicconnect.co.za', '$2a$10$johnhash', 'STAFF'),

('Sarah Staff', 'sarah.staff@civicconnect.co.za', '$2a$10$sarahhash', 'STAFF'),

('David Staff', 'david.staff@civicconnect.co.za', '$2a$10$davidhash', 'STAFF'),

('Alice Brown', 'alice@gmail.com', '$2a$10$alicehash', 'REQUESTER'),

('Bob Smith', 'bob@gmail.com', '$2a$10$bobhash', 'REQUESTER'),

('Charlie Jones', 'charlie@gmail.com', '$2a$10$charliehash', 'REQUESTER'),

('Emma Wilson', 'emma@gmail.com', '$2a$10$emmahash', 'REQUESTER');