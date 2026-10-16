package com.civicconnect.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    static final int MIN_SECRET_BYTES = 32;

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Pattern PAYLOAD =
            Pattern.compile("^\\{\"sub\":\"([0-9a-fA-F-]{36})\",\"iat\":(\\d{1,12}),\"exp\":(\\d{1,12})}$");
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final SecretKeySpec key;
    private final Duration ttl;
    private final Clock clock;
    private final String encodedHeader;

    public JwtService(@Value("${app.auth.secret:}") String secret,
                      @Value("${app.auth.token-ttl-minutes:480}") long ttlMinutes,
                      Clock clock) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "APP_AUTH_SECRET must be set to a random value of at least 32 characters");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.clock = clock;
        this.encodedHeader = ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(UUID userId) {
        long issuedAt = clock.instant().getEpochSecond();
        long expires = issuedAt + ttl.toSeconds();
        String payload = "{\"sub\":\"" + userId + "\",\"iat\":" + issuedAt + ",\"exp\":" + expires + "}";
        String signingInput = encodedHeader + "." + ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + ENCODER.encodeToString(sign(signingInput));
    }

    public Optional<UUID> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !parts[0].equals(encodedHeader)) {
            return Optional.empty();
        }
        try {
            byte[] given = DECODER.decode(parts[2]);
            byte[] expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(given, expected)) {
                return Optional.empty();
            }
            String payload = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            Matcher m = PAYLOAD.matcher(payload);
            if (!m.matches()) {
                return Optional.empty();
            }
            long expires = Long.parseLong(m.group(3));
            if (clock.instant().getEpochSecond() >= expires) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(m.group(1)));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
