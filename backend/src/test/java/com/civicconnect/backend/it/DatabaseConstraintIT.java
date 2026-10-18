package com.civicconnect.backend.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

class DatabaseConstraintIT extends DatabaseIntegrationTest {

    private static final String DUPLICATE = "CC-99999999";

    @Test
    void a05_theDatabaseRefusesTwoRequestsWithTheSameReference() throws SQLException {
        String category = categoryId("Other");
        String requester = queryOne("SELECT id::text FROM users WHERE email = 'req1@civicconnect.test'");
        String insert = "INSERT INTO service_requests (reference_number, category_id, description, requester_id)"
                + " VALUES (?, ?::uuid, 'A-05 duplicate reference check', ?::uuid)";
        execute("DELETE FROM service_requests WHERE reference_number = ?", DUPLICATE);
        try {
            execute(insert, DUPLICATE, category, requester);

            SQLException second = assertThrows(SQLException.class, () -> execute(insert, DUPLICATE, category, requester));

            assertEquals("23505", second.getSQLState());
            assertTrue(second.getMessage().contains("uq_service_requests_reference"));
            assertEquals(1, count("SELECT count(*) FROM service_requests WHERE reference_number = ?", DUPLICATE));
        } finally {
            execute("DELETE FROM service_requests WHERE reference_number = ?", DUPLICATE);
        }
    }

    @Test
    void everyStoredReferenceIsUnique() throws SQLException {
        assertEquals(0, count("SELECT count(*) - count(DISTINCT reference_number) FROM service_requests"));
    }

    @Test
    void theHistoryTableIsAppendOnlyEvenForTheOwner() throws SQLException {
        String row = queryOne("SELECT id::text FROM status_history LIMIT 1");

        SQLException update = assertThrows(SQLException.class,
                () -> execute("UPDATE status_history SET comment = 'tampered' WHERE id = ?::uuid", row));
        SQLException delete = assertThrows(SQLException.class,
                () -> execute("DELETE FROM status_history WHERE id = ?::uuid", row));

        assertTrue(update.getMessage().contains("append-only"));
        assertTrue(delete.getMessage().contains("append-only"));
    }

    @Test
    void staffNotesAreAppendOnlyToo() throws SQLException {
        String row = queryOne("SELECT id::text FROM request_comments LIMIT 1");

        SQLException update = assertThrows(SQLException.class,
                () -> execute("UPDATE request_comments SET body = 'tampered' WHERE id = ?::uuid", row));

        assertTrue(update.getMessage().contains("append-only"));
    }

    @Test
    void aStatusChangeThatChangesNothingIsRefused() throws SQLException {
        String request = queryOne("SELECT id::text FROM service_requests LIMIT 1");
        String user = queryOne("SELECT id::text FROM users LIMIT 1");

        SQLException same = assertThrows(SQLException.class, () -> execute(
                "INSERT INTO status_history (request_id, previous_status, new_status, changed_by)"
                        + " VALUES (?::uuid, 'SUBMITTED', 'SUBMITTED', ?::uuid)", request, user));
        SQLException noReason = assertThrows(SQLException.class, () -> execute(
                "INSERT INTO status_history (request_id, previous_status, new_status, changed_by)"
                        + " VALUES (?::uuid, 'SUBMITTED', 'REJECTED', ?::uuid)", request, user));

        assertEquals("23514", same.getSQLState());
        assertEquals("23514", noReason.getSQLState());
    }

    @Test
    void theApplicationAccountHasOnlyTheRightsItNeeds() throws SQLException {
        try (Connection c = app(); Statement s = c.createStatement()) {
            SQLException ddl = assertThrows(SQLException.class, () -> s.execute("CREATE TABLE should_not_exist (id int)"));
            SQLException deleteHistory = assertThrows(SQLException.class, () -> s.execute("DELETE FROM status_history"));
            SQLException deleteRequests = assertThrows(SQLException.class, () -> s.execute("DELETE FROM service_requests"));
            SQLException truncate = assertThrows(SQLException.class, () -> s.execute("TRUNCATE users"));

            assertEquals("42501", ddl.getSQLState());
            assertEquals("42501", deleteHistory.getSQLState());
            assertEquals("42501", deleteRequests.getSQLState());
            assertEquals("42501", truncate.getSQLState());
        }
    }

    @Test
    void everyMigrationWasApplied() throws SQLException {
        assertEquals(0, count("SELECT count(*) FROM flyway_schema_history WHERE success = FALSE"));
        assertEquals(3, count("SELECT count(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3') AND success"));
        assertEquals(7, count("SELECT count(*) FROM categories WHERE active"));
    }
}
