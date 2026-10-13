package com.civicconnect.backend.exception;

/** Maps to HTTP 403. Thrown by the access checks (ASR-02). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
