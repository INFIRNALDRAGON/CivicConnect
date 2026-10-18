package com.civicconnect.backend.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.it.Api.Response;

class LifecycleIT extends DatabaseIntegrationTest {

    @Test
    void aRequestMovesThroughTheWholeLifecycleAndEveryStepIsAudited() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        String manager = api.login("manager@civicconnect.test");

        Response created = api.createRequest(requester, categoryId("IT support"), unique("Lifecycle wifi outage in hall"));
        String id = created.text("id");
        String reference = created.text("referenceNumber");

        Response assigned = api.assign(staff, id, created.number("version"));
        assertEquals(200, assigned.status());
        assertEquals("ASSIGNED", assigned.text("status"));
        assertEquals("Sam Staff-One", assigned.text("assignedStaffName"));

        Response noted = api.comment(staff, id, "Waiting for the router firmware", assigned.number("version"));
        assertEquals(200, noted.status());
        assertTrue(noted.has("Waiting for the router firmware"));

        Response started = api.status(staff, id, "IN_PROGRESS", null, noted.number("version"));
        assertEquals(200, started.status());

        Response noNote = api.status(staff, id, "RESOLVED", null, started.number("version"));
        assertEquals(400, noNote.status());
        assertEquals("IN_PROGRESS", queryOne("SELECT status FROM service_requests WHERE id = ?::uuid", id));

        Response resolved = api.status(staff, id, "RESOLVED", "Router replaced and tested", started.number("version"));
        assertEquals(200, resolved.status());
        assertEquals("RESOLVED", resolved.text("status"));

        assertEquals(409, api.comment(staff, id, "Too late for a note", resolved.number("version")).status());
        assertEquals(409, api.status(staff, id, "IN_PROGRESS", null, resolved.number("version")).status());

        assertEquals(List.of("SUBMITTED", "ASSIGNED", "IN_PROGRESS", "RESOLVED"),
                queryColumn("SELECT new_status FROM status_history WHERE request_id = ?::uuid ORDER BY changed_at, id", id));
        assertEquals(1, count("SELECT count(*) FROM request_comments WHERE request_id = ?::uuid", id));

        Response seenByRequester = api.get("/api/requests/" + id, requester);
        assertEquals(200, seenByRequester.status());
        assertEquals("RESOLVED", seenByRequester.text("status"));
        assertTrue(seenByRequester.has("Router replaced and tested"));
        assertFalse(seenByRequester.has("Waiting for the router firmware"));
        assertFalse(seenByRequester.has("Sam Staff-One"));

        Response seenByStaff = api.get("/api/requests/" + id, staff);
        assertTrue(seenByStaff.has("Waiting for the router firmware"));
        assertTrue(seenByStaff.has("Sam Staff-One"));

        Response report = api.get("/api/reports/summary", manager);
        assertEquals(200, report.status());
        assertTrue(report.has(reference));
    }

    @Test
    void theRequesterIsToldAboutEachChangeAndCanMarkTheMessagesRead() throws SQLException {
        Api api = api();
        String requester = api.login("req2@civicconnect.test");
        String intruder = api.login("req3@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");

        Response created = api.createRequest(requester, categoryId("Maintenance"), unique("Notification check door hinge"));
        String id = created.text("id");
        String reference = created.text("referenceNumber");
        Response assigned = api.assign(staff, id, created.number("version"));
        Response started = api.status(staff, id, "IN_PROGRESS", null, assigned.number("version"));
        api.status(staff, id, "RESOLVED", "Hinge tightened", started.number("version"));

        Response notifications = api.get("/api/notifications", requester);
        assertEquals(200, notifications.status());
        assertTrue(notifications.has(reference));
        assertEquals(4, count("SELECT count(*) FROM notifications WHERE request_id = ?::uuid", id));
        assertFalse(api.get("/api/notifications", intruder).has(reference));

        String notificationId = queryOne("SELECT id::text FROM notifications WHERE request_id = ?::uuid"
                + " ORDER BY created_at LIMIT 1", id);
        assertTrue(api.post("/api/notifications/" + notificationId + "/read", intruder, "{}").status() >= 400);
        assertEquals(1, count("SELECT count(*) FROM notifications WHERE id = ?::uuid AND read_at IS NULL", notificationId));

        Response read = api.post("/api/notifications/" + notificationId + "/read", requester, "{}");
        assertEquals(200, read.status());
        assertEquals(1, count("SELECT count(*) FROM notifications WHERE id = ?::uuid AND read_at IS NOT NULL", notificationId));
    }

    @Test
    void aRejectionNeedsAReasonAndEndsTheRequest() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        Response created = api.createRequest(requester, categoryId("IT support"), unique("Rejection check duplicate report"));
        String id = created.text("id");

        Response noReason = api.status(staff, id, "REJECTED", null, created.number("version"));
        assertEquals(400, noReason.status());
        assertEquals("SUBMITTED", queryOne("SELECT status FROM service_requests WHERE id = ?::uuid", id));

        Response rejected = api.status(staff, id, "REJECTED", "Duplicate of an earlier report", created.number("version"));
        assertEquals(200, rejected.status());
        assertEquals("REJECTED", rejected.text("status"));

        assertEquals(409, api.assign(staff, id, rejected.number("version")).status());
        assertEquals(409, api.status(staff, id, "IN_PROGRESS", null, rejected.number("version")).status());
        assertEquals(409, api.comment(staff, id, "Note after rejection", rejected.number("version")).status());

        Response seen = api.get("/api/requests/" + id, requester);
        assertTrue(seen.has("Duplicate of an earlier report"));
    }

    @Test
    void theQueueSearchesFiltersAndSorts() throws SQLException {
        Api api = api();
        String requester = api.login("req5@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        String marker = "Qsearch" + (System.nanoTime() % 1000000);
        Response first = api.createRequest(requester, categoryId("IT support"), marker + " first request");
        Response second = api.createRequest(requester, categoryId("Maintenance"), marker + " second request");

        Response found = api.get("/api/requests?search=" + marker, staff);
        assertTrue(found.has(first.text("referenceNumber")));
        assertTrue(found.has(second.text("referenceNumber")));

        Response onlyIt = api.get("/api/requests?search=" + marker + "&categoryId=" + categoryId("IT support"), staff);
        assertTrue(onlyIt.has(first.text("referenceNumber")));
        assertFalse(onlyIt.has(second.text("referenceNumber")));

        Response oldest = api.get("/api/requests?search=" + marker, staff);
        assertTrue(oldest.body().indexOf(first.text("referenceNumber")) < oldest.body().indexOf(second.text("referenceNumber")));
        Response newest = api.get("/api/requests?search=" + marker + "&sort=age_asc", staff);
        assertTrue(newest.body().indexOf(second.text("referenceNumber")) < newest.body().indexOf(first.text("referenceNumber")));

        assertEquals(400, api.get("/api/requests?status=NOPE", staff).status());
    }

    @Test
    void theManagerSummaryCanBeNarrowedToACategory() throws SQLException {
        Api api = api();
        String manager = api.login("manager@civicconnect.test");
        String requester = api.login("req4@civicconnect.test");
        Response created = api.createRequest(requester, categoryId("Lost property"), unique("Summary check lost umbrella"));

        Response everything = api.get("/api/reports/summary", manager);
        Response lostOnly = api.get("/api/reports/summary?categoryId=" + categoryId("Lost property"), manager);
        Response itOnly = api.get("/api/reports/summary?categoryId=" + categoryId("IT support"), manager);

        assertTrue(everything.has(created.text("referenceNumber")));
        assertTrue(lostOnly.has(created.text("referenceNumber")));
        assertFalse(itOnly.has(created.text("referenceNumber")));
        assertEquals(400, api.get("/api/reports/summary?from=2026-13-45", manager).status());
        assertEquals(400, api.get("/api/reports/summary?from=2026-10-20&to=2026-10-01", manager).status());
    }
}
