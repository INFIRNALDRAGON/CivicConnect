package com.civicconnect.backend.exception;

<<<<<<< Updated upstream
/** Maps to HTTP 403. Thrown by the access checks (ASR-02). */
public class ForbiddenException extends RuntimeException {
=======
public class ForbiddenException extends RuntimeException {

>>>>>>> Stashed changes
    public ForbiddenException(String message) {
        super(message);
    }
}
