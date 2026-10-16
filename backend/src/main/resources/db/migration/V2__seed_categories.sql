-- V2 - controlled category list (reference data). Edit names to match your
-- approved scope (PED Section 2.1). Thresholds use the V1 default (72h) until
-- the team agrees real values with the stakeholders (CR-02).
-- NOTE: user accounts are NOT created here. Real users register through the app
-- (REQUESTER) and staff/manager accounts are provisioned by an administrator.
-- Demo accounts for local development live in db/dev/R__dev_seed.sql.
INSERT INTO categories (name, overdue_after_hours) VALUES
    ('Facility fault',    72),
    ('Damaged equipment', 96),
    ('Security concern',  24),
    ('IT support',        48),
    ('Maintenance',      120),
    ('Lost property',    168),
    ('Other',            120)
ON CONFLICT (name) DO NOTHING;
