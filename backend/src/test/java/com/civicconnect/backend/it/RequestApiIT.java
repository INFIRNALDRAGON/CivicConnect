package com.civicconnect.backend.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.it.Api.Response;

class RequestApiIT extends DatabaseIntegrationTest {

    @Test
    void a01_submissionStoresTheRequestAndItsFirstHistoryRow() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String description = unique("A-01 the printer on level two keeps jamming");

        Response created = api.createRequest(requester, categoryId("IT support"), description);

        assertEquals(201, created.status());
        String id = created.text("id");
        assertTrue(created.headers().firstValue("Location").orElseThrow().endsWith("/api/requests/" + id));
        assertTrue(created.text("referenceNumber").matches("CC-\\d{8}"));
        assertEquals("SUBMITTED", created.text("status"));
        assertEquals(1, count("SELECT count(*) FROM status_history WHERE request_id = ?::uuid", id));
        assertEquals("SUBMITTED", queryOne("SELECT new_status FROM status_history WHERE request_id = ?::uuid", id));
        assertEquals(created.text("referenceNumber"),
                queryOne("SELECT reference_number FROM service_requests WHERE id = ?::uuid", id));
        assertEquals("cccccccc-0000-0000-0000-000000000001",
                queryOne("SELECT requester_id::text FROM service_requests WHERE id = ?::uuid", id));
    }

    @Test
    void a01_theSubmittedRequestAppearsInTheRequestersOwnList() throws SQLException {
        Api api = api();
        String requester = api.login("req3@civicconnect.test");
        String description = unique("A-01 list check for the reception monitor");
        Response created = api.createRequest(requester, categoryId("Damaged equipment"), description);

        Response mine = api.get("/api/requests/mine", requester);

        assertEquals(200, mine.status());
        assertTrue(mine.has(created.text("referenceNumber")));
        assertFalse(api.get("/api/requests/mine", api.login("req4@civicconnect.test"))
                .has(created.text("referenceNumber")));
    }

    @Test
    void a02_aMissingOrUnknownCategoryIsRefusedAndNothingIsWritten() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String description = unique("A-02 category validation check");

        Response missing = api.post("/api/requests", requester,
                "{\"description\":\"" + description + "\",\"priority\":\"MEDIUM\"}");
        Response unknown = api.createRequest(requester, UUID.randomUUID().toString(), description);

        assertEquals(400, missing.status());
        assertEquals(400, unknown.status());
        assertEquals(0, count("SELECT count(*) FROM service_requests WHERE description = ?", description));
    }

    @Test
    void a02_anInactiveCategoryIsRefusedAndNothingIsWritten() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String description = unique("A-02 inactive category check");
        String other = categoryId("Other");
        execute("UPDATE categories SET active = FALSE WHERE id = ?::uuid", other);
        try {
            Response response = api.createRequest(requester, other, description);

            assertEquals(400, response.status());
            assertEquals(0, count("SELECT count(*) FROM service_requests WHERE description = ?", description));
            assertFalse(api.get("/api/categories", requester).has(other));
        } finally {
            execute("UPDATE categories SET active = TRUE WHERE id = ?::uuid", other);
        }
    }

    @Test
    void a02_theDescriptionLengthBoundariesAreEnforced() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String category = categoryId("Maintenance");
        String tag = Long.toString(System.nanoTime() % 100000);

        String nine = ("A02" + tag + "xxxxxxxxxxxxxxxx").substring(0, 9);
        String ten = ("A02" + tag + "xxxxxxxxxxxxxxxx").substring(0, 10);

        assertEquals(400, api.createRequest(requester, category, nine).status());
        assertEquals(400, api.createRequest(requester, category, "     " + nine + "     ").status());
        assertEquals(400, api.createRequest(requester, category, "          ").status());
        Response accepted = api.createRequest(requester, category, "   " + ten + "   ");
        assertEquals(201, accepted.status());
        assertEquals(ten, accepted.text("description"));
        assertEquals(400, api.createRequest(requester, category, "x".repeat(2001)).status());
        assertEquals(201, api.createRequest(requester, category, "x".repeat(2000)).status());
    }

    @Test
    void a03_anIllegalStatusJumpIsRefusedAndLeavesNoTrace() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        Response created = api.createRequest(requester, categoryId("IT support"), unique("A-03 illegal jump check"));
        String id = created.text("id");
        long version = created.number("version");

        long notificationsBefore = count("SELECT count(*) FROM notifications WHERE request_id = ?::uuid", id);

        Response jump = api.status(staff, id, "RESOLVED", "Fixed", version);

        assertEquals(409, jump.status());
        assertEquals("SUBMITTED", queryOne("SELECT status FROM service_requests WHERE id = ?::uuid", id));
        assertEquals(version, count("SELECT version FROM service_requests WHERE id = ?::uuid", id));
        assertEquals(1, count("SELECT count(*) FROM status_history WHERE request_id = ?::uuid", id));
        assertEquals(notificationsBefore, count("SELECT count(*) FROM notifications WHERE request_id = ?::uuid", id));
    }

    @Test
    void a03_aStaleVersionIsAConflict() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        Response created = api.createRequest(requester, categoryId("IT support"), unique("A-03 stale version check"));
        String id = created.text("id");
        long version = created.number("version");
        assertEquals(200, api.assign(staff, id, version).status());

        Response stale = api.status(staff, id, "IN_PROGRESS", null, version);

        assertEquals(409, stale.status());
        assertEquals("ASSIGNED", queryOne("SELECT status FROM service_requests WHERE id = ?::uuid", id));
    }

    @Test
    void a04_everyProtectedEndpointRefusesAnonymousCallers() throws SQLException {
        Api api = api();
        String id = "dddddddd-0000-0000-0000-000000000001";

        assertEquals(401, api.get("/api/requests/mine", null).status());
        assertEquals(401, api.get("/api/requests", null).status());
        assertEquals(401, api.get("/api/requests/" + id, null).status());
        assertEquals(401, api.get("/api/notifications", null).status());
        assertEquals(401, api.get("/api/reports/summary", null).status());
        assertEquals(401, api.get("/api/users/me", null).status());
        assertEquals(401, api.createRequest(null, categoryId("IT support"), "Anonymous submission attempt").status());
        assertEquals(401, api.get("/api/requests/mine", "not.a.real.token").status());
        assertEquals(200, api.get("/api/health", null).status());
    }

    @Test
    void a04_aRequesterCannotReadSomeoneElsesRequestOrLearnTheirDetails() throws SQLException {
        Api api = api();
        String owner = api.login("req1@civicconnect.test");
        String other = api.login("req2@civicconnect.test");
        Response created = api.createRequest(owner, categoryId("IT support"), unique("A-04 private request"));
        String id = created.text("id");

        Response denied = api.get("/api/requests/" + id, other);

        assertEquals(403, denied.status());
        assertFalse(denied.has("req1@civicconnect.test"));
        assertFalse(denied.has("Riley"));
        assertFalse(denied.has("A-04 private request"));
        assertFalse(api.get("/api/requests/mine", other).has(created.text("referenceNumber")));

        Response own = api.get("/api/requests/" + id, owner);
        assertEquals(200, own.status());
        assertFalse(own.has("req1@civicconnect.test"));
        assertFalse(own.has("assignedStaffName"));
        assertFalse(own.has("requesterReference"));
    }

    @Test
    void a04_rolesAreEnforcedOnTheServer() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        String manager = api.login("manager@civicconnect.test");
        String itRequest = api.createRequest(requester, categoryId("IT support"), unique("A-04 role check")).text("id");
        String lostProperty = "dddddddd-0000-0000-0000-000000000006";

        assertEquals(403, api.get("/api/requests", requester).status());
        assertEquals(403, api.get("/api/reports/summary", requester).status());
        assertEquals(403, api.get("/api/reports/summary", staff).status());
        assertEquals(403, api.assign(requester, itRequest, 0).status());
        assertEquals(403, api.get("/api/requests/" + lostProperty, staff).status());
        assertEquals(403, api.assign(api.login("staff2@civicconnect.test"), itRequest, 0).status());
        assertEquals(403, api.createRequest(staff, categoryId("IT support"), "Staff must not submit requests").status());
        assertEquals(200, api.get("/api/requests/" + lostProperty, manager).status());
        assertEquals(200, api.get("/api/reports/summary", manager).status());
    }

    @Test
    void a04_theStaffQueueOnlyContainsAuthorisedCategories() throws SQLException {
        Api api = api();
        String staff = api.login("staff1@civicconnect.test");
        Response queue = api.get("/api/requests", staff);

        assertEquals(200, queue.status());
        assertFalse(queue.has("Lost property"));
        assertFalse(queue.has("Security concern"));
        assertTrue(queue.has("IT support"));
    }
}
