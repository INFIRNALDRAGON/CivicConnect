package com.civicconnect.backend.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.civicconnect.backend.it.Api.Response;

class RollbackIT extends DatabaseIntegrationTest {

    @AfterEach
    void switchTheFaultOff() {
        FaultInjection.FAIL_STATUS_EVENTS.set(false);
    }

    @Test
    void aFailureWhileRecordingTheChangeLeavesTheRequestUntouched() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        Response created = api.createRequest(requester, categoryId("IT support"), unique("Rollback check on assignment"));
        String id = created.text("id");
        long version = created.number("version");
        long notificationsBefore = count("SELECT count(*) FROM notifications WHERE request_id = ?::uuid", id);

        FaultInjection.FAIL_STATUS_EVENTS.set(true);
        Response failed = api.assign(staff, id, version);
        FaultInjection.FAIL_STATUS_EVENTS.set(false);

        assertEquals(500, failed.status());
        assertEquals("SUBMITTED", queryOne("SELECT status FROM service_requests WHERE id = ?::uuid", id));
        assertEquals(version, count("SELECT version FROM service_requests WHERE id = ?::uuid", id));
        assertEquals(1, count("SELECT count(*) FROM status_history WHERE request_id = ?::uuid", id));
        assertEquals(0, count("SELECT count(*) FROM service_requests WHERE id = ?::uuid"
                + " AND assigned_staff_id IS NOT NULL", id));
        assertEquals(notificationsBefore, count("SELECT count(*) FROM notifications WHERE request_id = ?::uuid", id));
        assertFalse(api.get("/api/requests/" + id, requester).has("ASSIGNED"));
    }

    @Test
    void theSameRequestCanBeChangedOnceTheFaultIsGone() throws SQLException {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String staff = api.login("staff1@civicconnect.test");
        Response created = api.createRequest(requester, categoryId("IT support"), unique("Recovery after rollback check"));
        String id = created.text("id");

        FaultInjection.FAIL_STATUS_EVENTS.set(true);
        assertEquals(500, api.assign(staff, id, created.number("version")).status());
        FaultInjection.FAIL_STATUS_EVENTS.set(false);

        Response retried = api.assign(staff, id, created.number("version"));
        assertEquals(200, retried.status());
        assertEquals(2, count("SELECT count(*) FROM status_history WHERE request_id = ?::uuid", id));
    }
}
