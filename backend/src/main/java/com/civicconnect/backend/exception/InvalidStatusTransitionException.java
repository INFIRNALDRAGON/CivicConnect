package com.civicconnect.backend.exception;

import com.civicconnect.backend.request.RequestStatus;

/** Maps to HTTP 409. Thrown by the State objects when FR-010 would be violated. */
public class InvalidStatusTransitionException extends RuntimeException {
    private final RequestStatus from;
    private final RequestStatus to;

    public InvalidStatusTransitionException(RequestStatus from, RequestStatus to, String reason) {
        super("Cannot move a request from " + from.label() + " to " + to.label() + ": " + reason);
        this.from = from;
        this.to = to;
    }

    public RequestStatus getFrom() {
        return from;
    }

    public RequestStatus getTo() {
        return to;
    }
}
