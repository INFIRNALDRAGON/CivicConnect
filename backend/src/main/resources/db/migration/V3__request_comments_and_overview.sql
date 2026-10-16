-- =====================================================================
-- V3 - internal staff notes + assigned_at in the reporting view
--
-- Why: status_history rows must change the status (ck_status_history_changed),
-- so a free-text staff note ("waiting for the supplier") cannot live there.
-- FR-011 notes get their own append-only table. They are visible to staff and
-- managers only; requesters never see them (NFR-003).
-- =====================================================================
CREATE TABLE request_comments (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id  UUID         NOT NULL REFERENCES service_requests (id),
    author_id   UUID         NOT NULL REFERENCES users (id),
    body        TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_request_comments_body CHECK (length(btrim(body)) > 0 AND length(body) <= 1000)
);
CREATE INDEX ix_request_comments_request ON request_comments (request_id, created_at);

CREATE FUNCTION forbid_request_comment_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'request_comments is append-only: % is not allowed', TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END
$$;

CREATE TRIGGER trg_request_comments_append_only
    BEFORE UPDATE OR DELETE ON request_comments
    FOR EACH ROW EXECUTE FUNCTION forbid_request_comment_change();

GRANT SELECT, INSERT ON request_comments TO civicconnect_app;

-- The overview view gains assigned_at (appended LAST so CREATE OR REPLACE is allowed).
-- The request module reads submitted_at / assigned_at / resolved_at from here because
-- there is deliberately no created_at column on service_requests.
CREATE OR REPLACE VIEW v_request_overview AS
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
        AND sub.submitted_at < now() - make_interval(hours => c.overdue_after_hours)) AS is_overdue,
       asg.assigned_at
FROM service_requests r
JOIN categories c ON c.id = r.category_id
LEFT JOIN LATERAL (SELECT min(h.changed_at) AS submitted_at
                   FROM status_history h
                   WHERE h.request_id = r.id) sub ON TRUE
LEFT JOIN LATERAL (SELECT min(h.changed_at) AS resolved_at
                   FROM status_history h
                   WHERE h.request_id = r.id AND h.new_status = 'RESOLVED') res ON TRUE
LEFT JOIN LATERAL (SELECT min(h.changed_at) AS assigned_at
                   FROM status_history h
                   WHERE h.request_id = r.id AND h.new_status = 'ASSIGNED') asg ON TRUE;
