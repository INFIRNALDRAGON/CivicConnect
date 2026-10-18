package com.civicconnect.backend.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.it.Api.Response;

class AuthApiIT extends DatabaseIntegrationTest {

    @Test
    void everyDemoAccountCanSignInWithTheDocumentedPassword() {
        Api api = api();
        Map<String, String> accounts = Map.of(
                "manager@civicconnect.test", "MANAGER",
                "staff1@civicconnect.test", "STAFF",
                "staff2@civicconnect.test", "STAFF",
                "staff3@civicconnect.test", "STAFF",
                "req1@civicconnect.test", "REQUESTER",
                "req2@civicconnect.test", "REQUESTER",
                "req3@civicconnect.test", "REQUESTER",
                "req4@civicconnect.test", "REQUESTER",
                "req5@civicconnect.test", "REQUESTER");

        accounts.forEach((email, role) -> {
            Response r = api.post("/api/auth/login", null,
                    "{\"email\":\"" + email + "\",\"password\":\"" + Api.PASSWORD + "\"}");
            assertEquals(200, r.status(), email);
            assertEquals(role, r.text("role"), email);
            assertFalse(r.has("passwordHash"), email);
            assertFalse(r.has("password_hash"), email);
        });
    }

    @Test
    void wrongPasswordsAndUnknownAccountsLookIdentical() {
        Api api = api();

        Response wrong = api.post("/api/auth/login", null,
                "{\"email\":\"req1@civicconnect.test\",\"password\":\"Wrong@1234\"}");
        Response unknown = api.post("/api/auth/login", null,
                "{\"email\":\"nobody@civicconnect.test\",\"password\":\"Wrong@1234\"}");
        Response empty = api.post("/api/auth/login", null, "{\"email\":\"\",\"password\":\"\"}");

        assertEquals(401, wrong.status());
        assertEquals(401, unknown.status());
        assertEquals(401, empty.status());
        assertEquals(wrong.text("message"), unknown.text("message"));
    }

    @Test
    void emailsAreNotCaseSensitive() {
        Response r = api().post("/api/auth/login", null,
                "{\"email\":\"  REQ1@CivicConnect.test \",\"password\":\"" + Api.PASSWORD + "\"}");
        assertEquals(200, r.status());
    }

    @Test
    void registrationAlwaysCreatesARequesterAndRefusesDuplicates() throws SQLException {
        Api api = api();
        String email = "it.user." + (System.nanoTime() % 100000000) + "@civicconnect.test";
        String body = "{\"name\":\"Integration User\",\"email\":\"" + email + "\",\"password\":\"Sufficient1pass\","
                + "\"role\":\"MANAGER\"}";

        Response created = api.post("/api/auth/register", null, body);

        assertEquals(201, created.status());
        assertEquals("REQUESTER", created.text("role"));
        assertEquals("REQUESTER", queryOne("SELECT role FROM users WHERE email = ?", email));
        assertFalse(queryOne("SELECT password_hash FROM users WHERE email = ?", email).contains("Sufficient1pass"));
        assertEquals(409, api.post("/api/auth/register", null, body).status());
        assertEquals(409, api.post("/api/auth/register", null, body.replace(email, email.toUpperCase())).status());
        assertEquals(200, api.get("/api/users/me", created.text("token")).status());
    }

    @Test
    void registrationRejectsBadInput() {
        Api api = api();
        assertEquals(400, api.post("/api/auth/register", null,
                "{\"name\":\"\",\"email\":\"a@b.test\",\"password\":\"Sufficient1pass\"}").status());
        assertEquals(400, api.post("/api/auth/register", null,
                "{\"name\":\"Someone\",\"email\":\"not-an-email\",\"password\":\"Sufficient1pass\"}").status());
        assertEquals(400, api.post("/api/auth/register", null,
                "{\"name\":\"Someone\",\"email\":\"short.pw@civicconnect.test\",\"password\":\"short\"}").status());
        assertEquals(400, api.post("/api/auth/register", null, "not json").status());
    }

    @Test
    void theProfileEndpointReturnsTheSignedInUserOnly() {
        Api api = api();
        Response me = api.get("/api/users/me", api.login("staff2@civicconnect.test"));

        assertEquals(200, me.status());
        assertEquals("staff2@civicconnect.test", me.text("email"));
        assertTrue(me.has("STAFF"));
        assertFalse(me.has("password"));
    }
}
