package com.civicconnect.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.civicconnect.backend.auth.AuthService;
import com.civicconnect.backend.auth.JwtService;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.auth.UserAccount;
import com.civicconnect.backend.exception.ConflictException;
import com.civicconnect.backend.exception.UnauthorizedException;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.support.Ids;
import com.civicconnect.backend.support.InMemoryUserDirectory;

class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final InMemoryUserDirectory users = new InMemoryUserDirectory();
    private final JwtService jwt = new JwtService("unit-test-secret-that-is-long-enough-123456", 60,
            new TestFixture.MutableClock());
    private final AuthService auth = new AuthService(users, encoder, jwt);

    AuthServiceTest() {
        users.registerAccount(new UserAccount(Ids.of(1), "Sam Staff", "sam@civicconnect.test",
                encoder.encode("Test@1234"), Role.STAFF));
    }

    @Test
    void loginWithCorrectCredentialsIssuesAVerifiableToken() {
        AuthService.Session s = auth.login(" SAM@civicconnect.test ", "Test@1234");
        assertEquals(Role.STAFF, s.user().role());
        assertEquals(Ids.of(1), jwt.verify(s.token()).orElseThrow());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() {
        UnauthorizedException wrong = assertThrows(UnauthorizedException.class,
                () -> auth.login("sam@civicconnect.test", "nope"));
        UnauthorizedException unknown = assertThrows(UnauthorizedException.class,
                () -> auth.login("ghost@civicconnect.test", "Test@1234"));
        assertEquals(wrong.getMessage(), unknown.getMessage());
        assertThrows(UnauthorizedException.class, () -> auth.login(null, null));
    }

    @Test
    void registrationCreatesARequesterWithAHashedPassword() {
        AuthService.Session s = auth.register("  Nia Citizen ", "Nia@Example.com", "longenough1");
        assertEquals(Role.REQUESTER, s.user().role());
        assertEquals("nia@example.com", s.user().email());
        assertEquals("Nia Citizen", s.user().name());
        assertNotEquals("longenough1", s.user().passwordHash());
        assertTrue(encoder.matches("longenough1", s.user().passwordHash()));
        assertEquals(s.user().id(), jwt.verify(s.token()).orElseThrow());
    }

    @Test
    void registrationValidatesInputAndRejectsDuplicates() {
        assertThrows(ValidationException.class, () -> auth.register("", "a@b.co", "longenough1"));
        assertThrows(ValidationException.class, () -> auth.register("Nia", "not-an-email", "longenough1"));
        assertThrows(ValidationException.class, () -> auth.register("Nia", "a@b.co", "short"));
        assertThrows(ValidationException.class, () -> auth.register("Nia", "a@b.co", "x".repeat(73)));
        assertThrows(ConflictException.class, () -> auth.register("Other Sam", "sam@civicconnect.test", "longenough1"));
    }
}
