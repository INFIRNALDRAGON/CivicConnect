package com.civicconnect.backend.model;

/** Matches the CHECK constraint on service_requests.status / status_history. */
public enum RequestStatus {
    SUBMITTED, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REJECTED
}
