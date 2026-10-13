package com.civicconnect.backend.exception;

import java.util.List;

/** Maps to HTTP 400. Carries every problem found so the client can show them all at once. */
public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(String error) {
        this(List.of(error));
    }

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
