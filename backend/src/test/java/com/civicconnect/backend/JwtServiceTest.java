package com.civicconnect.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.auth.JwtService;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-123456";

    private final TestFixture.MutableClock clock = new TestFixture.MutableClock();
    private final JwtService jwt = new JwtService(SECRET, 60, clock);

    @Test
    void roundTripReturnsTheUser() {
        UUID id = UUID.randomUUID();
        assertEquals(Optional.of(id), jwt.verify(jwt.issue(id)));
    }

    @Test
    void expiredTokensAreRejected() {
        String token = jwt.issue(UUID.randomUUID());
        clock.advance(Duration.ofMinutes(61));
        assertTrue(jwt.verify(token).isEmpty());
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = jwt.issue(UUID.randomUUID());
        String[] parts = token.split("\\.");
        String forged = new String(Base64.getUrlDecoder().decode(parts[1])).replaceFirst("\"sub\":\"[^\"]+\"",
                "\"sub\":\"" + UUID.randomUUID() + "\"");
        String swapped = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes())
                + "." + parts[2];
        assertTrue(jwt.verify(swapped).isEmpty());
    }

    @Test
    void tokensFromAnotherSecretAreRejected() {
        JwtService other = new JwtService("a-completely-different-secret-value-9876543210", 60, clock);
        assertTrue(jwt.verify(other.issue(UUID.randomUUID())).isEmpty());
    }

    @Test
    void unsignedAndMalformedTokensAreRejected() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + UUID.randomUUID() + "\",\"iat\":1,\"exp\":9999999999}").getBytes());
        assertTrue(jwt.verify(header + "." + payload + ".").isEmpty());
        assertTrue(jwt.verify("garbage").isEmpty());
        assertTrue(jwt.verify("a.b.c").isEmpty());
        assertTrue(jwt.verify("").isEmpty());
        assertTrue(jwt.verify(null).isEmpty());
    }

    @Test
    void shortOrMissingSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 60, clock));
        assertThrows(IllegalStateException.class, () -> new JwtService("", 60, clock));
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 60, clock));
    }
}
