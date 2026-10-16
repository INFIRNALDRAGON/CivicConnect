package com.civicconnect.backend.request;

<<<<<<< Updated upstream
/**
 * Lifecycle stages from ED-007. The enum is plain data and lives in the request module;
 * the rules about which stage may follow which live in the status module's State objects.
 */
=======
>>>>>>> Stashed changes
public enum RequestStatus {
    SUBMITTED("Submitted"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
<<<<<<< Updated upstream
    CLOSED("Closed");
=======
    CLOSED("Closed"),
    REJECTED("Rejected");
>>>>>>> Stashed changes

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

<<<<<<< Updated upstream
    /** Resolved and Closed requests accept no edits or comments (FR-012, CR-05). 
     *@return
     */
    public boolean isReadOnly() {
        return this == RESOLVED || this == CLOSED;
    }

    /** Still counts as "open" for overdue reporting (FR-014). 
     *@return
     */
=======
    public boolean isReadOnly() {
        return this == RESOLVED || this == CLOSED || this == REJECTED;
    }

>>>>>>> Stashed changes
    public boolean isOpen() {
        return !isReadOnly();
    }
}
