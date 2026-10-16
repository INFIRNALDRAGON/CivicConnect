package com.civicconnect.backend.exception;

<<<<<<< Updated upstream
/** Maps to HTTP 409. Used for optimistic-lock failures and read-only requests (ASR-03). */
public class ConflictException extends RuntimeException {
=======
public class ConflictException extends RuntimeException {

>>>>>>> Stashed changes
    public ConflictException(String message) {
        super(message);
    }
}
