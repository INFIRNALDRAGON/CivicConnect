package com.civicconnect.backend.request;

public enum RequestStatus {
    SUBMITTED("Submitted"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    REJECTED("Rejected");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isReadOnly() {
        return this == RESOLVED || this == CLOSED || this == REJECTED;
    }

    public boolean isOpen() {
        return !isReadOnly();
    }
}
