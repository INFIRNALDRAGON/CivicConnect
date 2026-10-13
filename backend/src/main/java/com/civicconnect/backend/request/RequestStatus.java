package com.civicconnect.backend.request;

/**
 * Lifecycle stages from ED-007. The enum is plain data and lives in the request module;
 * the rules about which stage may follow which live in the status module's State objects.
 */
public enum RequestStatus {
    SUBMITTED("Submitted"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Resolved and Closed requests accept no edits or comments (FR-012, CR-05). 
     *@return
     */
    public boolean isReadOnly() {
        return this == RESOLVED || this == CLOSED;
    }

    /** Still counts as "open" for overdue reporting (FR-014). 
     *@return
     */
    public boolean isOpen() {
        return !isReadOnly();
    }
}
