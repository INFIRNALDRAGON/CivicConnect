package com.civicconnect.backend.request;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Unique, hard-to-guess tracking references like {@code CC-7K2M9-QX4TB} (FR-001, NFR-002).
 * Ten characters from a 32-symbol alphabet is 50 bits, so collisions are astronomically rare;
 * {@link RequestService} still checks the store and retries, and the database will add a UNIQUE
 * constraint as the last line of defence.
 */
@Component
public class ReferenceNumberGenerator {

    /** No 0/O/1/I so references survive being read over the phone. */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder("CC-");
        for (int i = 0; i < LENGTH; i++) {
            if (i == LENGTH / 2) {
                sb.append('-');
            }
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
