package com.civicconnect.backend.status;

import com.civicconnect.backend.request.RequestStatus;

/**
 * A request to move a request to {@code target}.
 *
 * @param comment    resolution note or reason; required for Resolved
 * @param assigneeId required when target is Assigned
 * @param expectedVersion the version the caller last saw (optimistic locking)
 */
public record TransitionCommand(RequestStatus target, String comment, Long assigneeId, long expectedVersion) {
}
