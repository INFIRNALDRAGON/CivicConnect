-- =====================================================================
-- V1 - CivicConnect core schema
-- Source: PED v2.0 Section 6 (data model) + approved changes in Section 20.3
-- Run as civicconnect_owner (Flyway). Location: src/main/resources/db/migration/
-- Timestamps are TIMESTAMPTZ and stored in UTC (PED Section 23).
-- Allowed values live in CHECK constraints (not PG enums) so Hibernate can use
-- @Enumerated(EnumType.STRING) and new states are a simple migration.
-- =====================================================================

-- Reference numbers (NFR-002 / ASR-03). Sequence-based: no collisions possible.
-- The UNIQUE constraint below is mandatory either way (PED ASR-03).
CREATE SEQUENCE request_reference_seq START 1 INCREMENT 1;

-- ---------------------------------------------------------------- users
-- Application roles (REQUESTER / STAFF / MANAGER) are DATA here, not DB roles.
CREATE TABLE users (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(254) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,            -- BCrypt hash, never plain text
    role           VARCHAR(20)  NOT NULL,
    CONSTRAINT ck_users_role CHECK (role IN ('REQUESTER', 'STAFF', 'MANAGER'))
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

-- ----------------------------------------------------------- categories
-- Controlled category list (DEC-01 / FR-002).
CREATE TABLE categories (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(100) NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    -- CR-02 / FR-014: per-category overdue threshold. 72 is a PLACEHOLDER default.
    overdue_after_hours  INTEGER      NOT NULL DEFAULT 72,
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT ck_categories_overdue CHECK (overdue_after_hours > 0)
);

-- -------------------------------------------------------- user_category
-- CR-06 / FR-006: which categories a staff member may see.
CREATE TABLE user_category (
    user_id      UUID NOT NULL REFERENCES users (id)      ON DELETE CASCADE,
    category_id  UUID NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, category_id)
);
CREATE INDEX ix_user_category_category ON user_category (category_id);

-- ------------------------------------------------------ service_requests
-- No created_at column (team decision). Submission time is the first
-- status_history row; see view v_request_overview below.
CREATE TABLE service_requests (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number   VARCHAR(20)  NOT NULL
                       DEFAULT ('CC-' || to_char(nextval('request_reference_seq'), 'FM00000000')),
    category_id        UUID         NOT NULL REFERENCES categories (id),
    description        TEXT         NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    priority           VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    requester_id       UUID         NOT NULL REFERENCES users (id),
    assigned_staff_id  UUID         REFERENCES users (id),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version            BIGINT       NOT NULL DEFAULT 0,   -- optimistic lock (ASR-03), JPA @Version
    CONSTRAINT uq_service_requests_reference UNIQUE (reference_number),
    CONSTRAINT ck_service_requests_status CHECK
        (status IN ('SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED')),
    CONSTRAINT ck_service_requests_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_service_requests_description CHECK (length(btrim(description)) > 0)
);
-- Filtering / sorting for FR-007 and ownership lookups.
CREATE INDEX ix_service_requests_requester ON service_requests (requester_id);
CREATE INDEX ix_service_requests_assignee  ON service_requests (assigned_staff_id);
CREATE INDEX ix_service_requests_category  ON service_requests (category_id);
CREATE INDEX ix_service_requests_status    ON service_requests (status);

-- -------------------------------------------------------- status_history
-- Append-only audit trail (NFR-006 / ASR-01).
CREATE TABLE status_history (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id       UUID         NOT NULL REFERENCES service_requests (id),
    previous_status  VARCHAR(20),                       -- NULL for the first (submission) row
    new_status       VARCHAR(20)  NOT NULL,
    changed_by       UUID         NOT NULL REFERENCES users (id),
    changed_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    comment          TEXT,
    CONSTRAINT ck_status_history_new CHECK
        (new_status IN ('SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED')),
    CONSTRAINT ck_status_history_prev CHECK
        (previous_status IS NULL OR previous_status IN
         ('SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED')),
    CONSTRAINT ck_status_history_changed CHECK (previous_status IS DISTINCT FROM new_status),
    -- CR-03: a rejection must carry a reason.
    CONSTRAINT ck_status_history_reject_reason CHECK
        (new_status <> 'REJECTED' OR (comment IS NOT NULL AND length(btrim(comment)) > 0))
);
CREATE INDEX ix_status_history_request ON status_history (request_id, changed_at);

-- --------------------------------------------------------- notifications
-- CR-04 / FR-005: in-app messages only (no email/SMS).
CREATE TABLE notifications (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id    UUID          NOT NULL REFERENCES service_requests (id),
    recipient_id  UUID          NOT NULL REFERENCES users (id),
    message       VARCHAR(500)  NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    read_at       TIMESTAMPTZ
);
CREATE INDEX ix_notifications_recipient ON notifications (recipient_id, read_at);

-- ------------------------------------------------------------- triggers
CREATE FUNCTION touch_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_service_requests_touch
    BEFORE UPDATE ON service_requests
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- Makes "append-only" true at the database level, even for the owner role.
CREATE FUNCTION forbid_status_history_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'status_history is append-only (NFR-006): % is not allowed', TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END
$$;

CREATE TRIGGER trg_status_history_append_only
    BEFORE UPDATE OR DELETE ON status_history
    FOR EACH ROW EXECUTE FUNCTION forbid_status_history_change();

-- ----------------------------------------------------------------- view
-- Reporting module (FR-013..FR-016): submission time, resolution time, overdue flag.
-- Contains no personal data. "Overdue" = still open and older than the category threshold.
CREATE VIEW v_request_overview AS
SELECT r.id,
       r.reference_number,
       r.category_id,
       c.name                AS category_name,
       r.status,
       r.priority,
       r.requester_id,
       r.assigned_staff_id,
       r.updated_at,
       sub.submitted_at,
       res.resolved_at,
       (r.status IN ('SUBMITTED', 'ASSIGNED', 'IN_PROGRESS')
        AND sub.submitted_at < now() - make_interval(hours => c.overdue_after_hours)) AS is_overdue
FROM service_requests r
JOIN categories c ON c.id = r.category_id
LEFT JOIN LATERAL (SELECT min(h.changed_at) AS submitted_at
                   FROM status_history h
                   WHERE h.request_id = r.id) sub ON TRUE
LEFT JOIN LATERAL (SELECT min(h.changed_at) AS resolved_at
                   FROM status_history h
                   WHERE h.request_id = r.id AND h.new_status = 'RESOLVED') res ON TRUE;

-- ------------------------------------------------- privileges (least privilege)
-- Mapped to module table ownership in PED Section 22.2.
-- Run order: 00_roles.sql first, otherwise these GRANTs fail.

-- civicconnect_app = the running application
GRANT SELECT, INSERT, UPDATE ON users, categories, service_requests, notifications
    TO civicconnect_app;                       -- no DELETE: requests/users are never hard-deleted
GRANT SELECT, INSERT, DELETE ON user_category TO civicconnect_app;   -- Auth/RBAC scope
GRANT SELECT, INSERT         ON status_history TO civicconnect_app;  -- append-only (ASR-01)
GRANT USAGE ON SEQUENCE request_reference_seq TO civicconnect_app;
GRANT SELECT ON v_request_overview TO civicconnect_app;              -- Reporting module

-- civicconnect_readonly = inspection / dashboards. No password hashes, no emails,
-- no free-text descriptions.
GRANT SELECT ON v_request_overview, categories TO civicconnect_readonly;
GRANT SELECT (id, name, role) ON users TO civicconnect_readonly;
