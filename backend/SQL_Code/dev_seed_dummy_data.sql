-- =====================================================================
-- CivicConnect - DUMMY DATA for local testing ONLY
--   * Run in pgAdmin Query Tool as ccadmin (or civicconnect_owner), AFTER the
--     backend has started once so Flyway has created the tables (V1 + V2).
--   * NOT a Flyway migration: keep it out of src/main/resources/db/migration
--     so it can never reach staging or production.
--   * Safe to re-run (fixed IDs + ON CONFLICT DO NOTHING).
--   * Every dummy user has the password:  Test@1234   (BCrypt hash below)
--   * To wipe it again:  see the cleanup block at the bottom of this file.
-- =====================================================================
BEGIN;

-- ------------------------------------------------------------- users
-- 1 manager, 3 staff, 5 requesters. Emails use the reserved .test domain.
INSERT INTO users (id, name, email, password_hash, role) VALUES
 ('aaaaaaaa-0000-0000-0000-000000000001','Morgan Manager','manager@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','MANAGER'),
 ('bbbbbbbb-0000-0000-0000-000000000001','Sam Staff-One','staff1@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','STAFF'),
 ('bbbbbbbb-0000-0000-0000-000000000002','Sasha Staff-Two','staff2@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','STAFF'),
 ('bbbbbbbb-0000-0000-0000-000000000003','Sidney Staff-Three','staff3@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','STAFF'),
 ('cccccccc-0000-0000-0000-000000000001','Riley Requester-One','req1@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','REQUESTER'),
 ('cccccccc-0000-0000-0000-000000000002','Robin Requester-Two','req2@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','REQUESTER'),
 ('cccccccc-0000-0000-0000-000000000003','Reese Requester-Three','req3@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','REQUESTER'),
 ('cccccccc-0000-0000-0000-000000000004','Remi Requester-Four','req4@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','REQUESTER'),
 ('cccccccc-0000-0000-0000-000000000005','Rowan Requester-Five','req5@civicconnect.test','$2b$10$8jFYGVv7XaLxsVsIaY3/Zu2gteBTSM4rjKBZtaue37CZFscUJ4cjS','REQUESTER')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------- staff category access (FR-006)
INSERT INTO user_category (user_id, category_id)
SELECT s.user_id::uuid, c.id
FROM (VALUES
  ('bbbbbbbb-0000-0000-0000-000000000001','IT support'),
  ('bbbbbbbb-0000-0000-0000-000000000001','Maintenance'),
  ('bbbbbbbb-0000-0000-0000-000000000002','Facility fault'),
  ('bbbbbbbb-0000-0000-0000-000000000002','Damaged equipment'),
  ('bbbbbbbb-0000-0000-0000-000000000002','Security concern'),
  ('bbbbbbbb-0000-0000-0000-000000000003','Lost property'),
  ('bbbbbbbb-0000-0000-0000-000000000003','Other'),
  ('bbbbbbbb-0000-0000-0000-000000000003','IT support')
) AS s(user_id, category_name)
JOIN categories c ON c.name = s.category_name
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------- service requests
-- 14 requests covering every status. updated_ago = time since the last change.
-- Open requests older than 72h are overdue in v_request_overview.
INSERT INTO service_requests
       (id, category_id, description, status, priority, requester_id, assigned_staff_id, updated_at)
SELECT v.id::uuid, c.id, v.description, v.status, v.priority,
       v.requester::uuid, NULLIF(v.staff, '')::uuid, now() - v.updated_ago::interval
FROM (VALUES
 ('dddddddd-0000-0000-0000-000000000001','IT support','Wi-Fi drops every few minutes in the main hall','SUBMITTED','HIGH','cccccccc-0000-0000-0000-000000000001','','6 hours'),
 ('dddddddd-0000-0000-0000-000000000002','Facility fault','Burst pipe in the east wing bathroom','ASSIGNED','HIGH','cccccccc-0000-0000-0000-000000000002','bbbbbbbb-0000-0000-0000-000000000002','36 hours'),
 ('dddddddd-0000-0000-0000-000000000003','Maintenance','Door hinge on the storeroom is loose','IN_PROGRESS','LOW','cccccccc-0000-0000-0000-000000000001','bbbbbbbb-0000-0000-0000-000000000001','2 days'),
 ('dddddddd-0000-0000-0000-000000000004','Damaged equipment','Projector in room 4 will not power on','RESOLVED','MEDIUM','cccccccc-0000-0000-0000-000000000003','bbbbbbbb-0000-0000-0000-000000000002','3 days'),
 ('dddddddd-0000-0000-0000-000000000005','Security concern','Side gate lock is broken and does not close','CLOSED','HIGH','cccccccc-0000-0000-0000-000000000004','bbbbbbbb-0000-0000-0000-000000000002','6 days'),
 ('dddddddd-0000-0000-0000-000000000006','Lost property','Left a blue backpack in the hall on Friday','REJECTED','LOW','cccccccc-0000-0000-0000-000000000005','bbbbbbbb-0000-0000-0000-000000000003','4 days'),
 ('dddddddd-0000-0000-0000-000000000007','IT support','Printer on the second floor jams on every job','ASSIGNED','MEDIUM','cccccccc-0000-0000-0000-000000000002','bbbbbbbb-0000-0000-0000-000000000001','3 days'),
 ('dddddddd-0000-0000-0000-000000000008','Other','Request for extra chairs for Saturday event','SUBMITTED','LOW','cccccccc-0000-0000-0000-000000000003','','30 hours'),
 ('dddddddd-0000-0000-0000-000000000009','Maintenance','Light flickering in corridor B','RESOLVED','MEDIUM','cccccccc-0000-0000-0000-000000000004','bbbbbbbb-0000-0000-0000-000000000001','5 days'),
 ('dddddddd-0000-0000-0000-000000000010','Facility fault','Air conditioning is leaking onto the floor','IN_PROGRESS','HIGH','cccccccc-0000-0000-0000-000000000005','bbbbbbbb-0000-0000-0000-000000000002','20 hours'),
 ('dddddddd-0000-0000-0000-000000000011','Damaged equipment','Cracked screen on the reception monitor','SUBMITTED','MEDIUM','cccccccc-0000-0000-0000-000000000001','','5 days'),
 ('dddddddd-0000-0000-0000-000000000012','Security concern','Unknown person seen near the storage shed','CLOSED','HIGH','cccccccc-0000-0000-0000-000000000002','bbbbbbbb-0000-0000-0000-000000000002','12 days'),
 ('dddddddd-0000-0000-0000-000000000013','IT support','Cannot log in to the shared desktop','REJECTED','LOW','cccccccc-0000-0000-0000-000000000004','bbbbbbbb-0000-0000-0000-000000000003','2 days'),
 ('dddddddd-0000-0000-0000-000000000014','Other','Suggestion box key is missing','SUBMITTED','LOW','cccccccc-0000-0000-0000-000000000005','','20 minutes')
) AS v(id, category_name, description, status, priority, requester, staff, updated_ago)
JOIN categories c ON c.name = v.category_name
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------- status history
-- Valid transitions only. The first row of each request is its submission
-- (previous_status empty) and defines its submitted_at in the reporting view.
INSERT INTO status_history (id, request_id, previous_status, new_status, changed_by, changed_at, comment)
SELECT md5(h.req_id || h.new_status)::uuid, h.req_id::uuid, NULLIF(h.prev_status, ''),
       h.new_status, h.actor::uuid, now() - h.ago::interval, NULLIF(h.note, '')
FROM (VALUES
 -- r01 SUBMITTED
 ('dddddddd-0000-0000-0000-000000000001','','SUBMITTED','cccccccc-0000-0000-0000-000000000001','6 hours',''),
 -- r02 ASSIGNED
 ('dddddddd-0000-0000-0000-000000000002','','SUBMITTED','cccccccc-0000-0000-0000-000000000002','2 days',''),
 ('dddddddd-0000-0000-0000-000000000002','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000002','36 hours','Taking this one'),
 -- r03 IN_PROGRESS (overdue)
 ('dddddddd-0000-0000-0000-000000000003','','SUBMITTED','cccccccc-0000-0000-0000-000000000001','4 days',''),
 ('dddddddd-0000-0000-0000-000000000003','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000001','3 days',''),
 ('dddddddd-0000-0000-0000-000000000003','ASSIGNED','IN_PROGRESS','bbbbbbbb-0000-0000-0000-000000000001','2 days','Waiting on replacement hinge'),
 -- r04 RESOLVED
 ('dddddddd-0000-0000-0000-000000000004','','SUBMITTED','cccccccc-0000-0000-0000-000000000003','5 days',''),
 ('dddddddd-0000-0000-0000-000000000004','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000002','4 days',''),
 ('dddddddd-0000-0000-0000-000000000004','ASSIGNED','IN_PROGRESS','bbbbbbbb-0000-0000-0000-000000000002','4 days',''),
 ('dddddddd-0000-0000-0000-000000000004','IN_PROGRESS','RESOLVED','bbbbbbbb-0000-0000-0000-000000000002','3 days','Replaced the power supply'),
 -- r05 CLOSED (closed by the manager)
 ('dddddddd-0000-0000-0000-000000000005','','SUBMITTED','cccccccc-0000-0000-0000-000000000004','10 days',''),
 ('dddddddd-0000-0000-0000-000000000005','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000002','9 days',''),
 ('dddddddd-0000-0000-0000-000000000005','ASSIGNED','IN_PROGRESS','bbbbbbbb-0000-0000-0000-000000000002','9 days',''),
 ('dddddddd-0000-0000-0000-000000000005','IN_PROGRESS','RESOLVED','bbbbbbbb-0000-0000-0000-000000000002','7 days','New lock fitted'),
 ('dddddddd-0000-0000-0000-000000000005','RESOLVED','CLOSED','aaaaaaaa-0000-0000-0000-000000000001','6 days','Confirmed on site'),
 -- r06 REJECTED (reason is mandatory)
 ('dddddddd-0000-0000-0000-000000000006','','SUBMITTED','cccccccc-0000-0000-0000-000000000005','4 days',''),
 ('dddddddd-0000-0000-0000-000000000006','SUBMITTED','REJECTED','bbbbbbbb-0000-0000-0000-000000000003','4 days','Duplicate of an earlier report for the same item'),
 -- r07 ASSIGNED (overdue)
 ('dddddddd-0000-0000-0000-000000000007','','SUBMITTED','cccccccc-0000-0000-0000-000000000002','4 days',''),
 ('dddddddd-0000-0000-0000-000000000007','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000001','3 days',''),
 -- r08 SUBMITTED
 ('dddddddd-0000-0000-0000-000000000008','','SUBMITTED','cccccccc-0000-0000-0000-000000000003','30 hours',''),
 -- r09 RESOLVED
 ('dddddddd-0000-0000-0000-000000000009','','SUBMITTED','cccccccc-0000-0000-0000-000000000004','7 days',''),
 ('dddddddd-0000-0000-0000-000000000009','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000001','6 days',''),
 ('dddddddd-0000-0000-0000-000000000009','ASSIGNED','IN_PROGRESS','bbbbbbbb-0000-0000-0000-000000000001','6 days',''),
 ('dddddddd-0000-0000-0000-000000000009','IN_PROGRESS','RESOLVED','bbbbbbbb-0000-0000-0000-000000000001','5 days','Starter replaced'),
 -- r10 IN_PROGRESS
 ('dddddddd-0000-0000-0000-000000000010','','SUBMITTED','cccccccc-0000-0000-0000-000000000005','1 day',''),
 ('dddddddd-0000-0000-0000-000000000010','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000002','22 hours',''),
 ('dddddddd-0000-0000-0000-000000000010','ASSIGNED','IN_PROGRESS','bbbbbbbb-0000-0000-0000-000000000002','20 hours','Contractor called'),
 -- r11 SUBMITTED but old (overdue, unassigned)
 ('dddddddd-0000-0000-0000-000000000011','','SUBMITTED','cccccccc-0000-0000-0000-000000000001','5 days',''),
 -- r12 CLOSED
 ('dddddddd-0000-0000-0000-000000000012','','SUBMITTED','cccccccc-0000-0000-0000-000000000002','14 days',''),
 ('dddddddd-0000-0000-0000-000000000012','SUBMITTED','ASSIGNED','bbbbbbbb-0000-0000-0000-000000000002','14 days',''),
 ('dddddddd-0000-0000-0000-000000000012','ASSIGNED','IN_PROGRESS','bbbbbbbb-0000-0000-0000-000000000002','13 days',''),
 ('dddddddd-0000-0000-0000-000000000012','IN_PROGRESS','RESOLVED','bbbbbbbb-0000-0000-0000-000000000002','13 days','Patrol increased, nothing found'),
 ('dddddddd-0000-0000-0000-000000000012','RESOLVED','CLOSED','bbbbbbbb-0000-0000-0000-000000000002','12 days',''),
 -- r13 REJECTED
 ('dddddddd-0000-0000-0000-000000000013','','SUBMITTED','cccccccc-0000-0000-0000-000000000004','2 days',''),
 ('dddddddd-0000-0000-0000-000000000013','SUBMITTED','REJECTED','bbbbbbbb-0000-0000-0000-000000000003','2 days','Account issue, please contact the IT helpdesk directly'),
 -- r14 SUBMITTED (brand new)
 ('dddddddd-0000-0000-0000-000000000014','','SUBMITTED','cccccccc-0000-0000-0000-000000000005','20 minutes','')
) AS h(req_id, prev_status, new_status, actor, ago, note)
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------- notifications
-- One in-app message to the requester for every change after submission (FR-005).
-- Messages older than 2 days are marked as read; newer ones stay unread.
INSERT INTO notifications (id, request_id, recipient_id, message, created_at, read_at)
SELECT md5('n-' || h.id::text)::uuid,
       r.id,
       r.requester_id,
       'Your request ' || r.reference_number || ' is now ' || replace(h.new_status, '_', ' ')
         || COALESCE(': ' || h.comment, '.'),
       h.changed_at,
       CASE WHEN h.changed_at < now() - interval '2 days' THEN h.changed_at + interval '1 hour' END
FROM status_history h
JOIN service_requests r ON r.id = h.request_id
WHERE h.new_status <> 'SUBMITTED'
  AND h.request_id::text LIKE 'dddddddd-0000-0000-0000-0000000000%'
ON CONFLICT DO NOTHING;

COMMIT;

-- ------------------------------------------------------------ quick check
SELECT reference_number, category_name, status, priority, is_overdue
FROM v_request_overview ORDER BY submitted_at;

-- =====================================================================
-- CLEANUP (run manually, as ccadmin, to remove ONLY this dummy data).
-- status_history is append-only, so the trigger is disabled briefly.
-- =====================================================================
-- BEGIN;
-- ALTER TABLE status_history DISABLE TRIGGER trg_status_history_append_only;
-- DELETE FROM notifications   WHERE request_id::text LIKE 'dddddddd-0000-0000-0000-0000000000%';
-- DELETE FROM status_history  WHERE request_id::text LIKE 'dddddddd-0000-0000-0000-0000000000%';
-- ALTER TABLE status_history ENABLE TRIGGER trg_status_history_append_only;
-- DELETE FROM service_requests WHERE id::text LIKE 'dddddddd-0000-0000-0000-0000000000%';
-- DELETE FROM user_category   WHERE user_id::text LIKE 'bbbbbbbb-0000-0000-0000-0000000000%';
-- DELETE FROM users WHERE id::text LIKE ANY (ARRAY['aaaaaaaa-%','bbbbbbbb-%','cccccccc-%']);
-- COMMIT;
