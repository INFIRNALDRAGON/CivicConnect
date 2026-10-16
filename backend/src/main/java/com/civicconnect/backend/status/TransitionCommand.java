package com.civicconnect.backend.status;

<<<<<<< Updated upstream
import com.civicconnect.backend.request.RequestStatus;

/**
 * A request to move a request to {@code target}.
 *
 * @param comment    resolution note or reason; required for Resolved
 * @param assigneeId required when target is Assigned
 * @param expectedVersion the version the caller last saw (optimistic locking)
 */
public record TransitionCommand(RequestStatus target, String comment, Long assigneeId, long expectedVersion) {
=======
import java.util.UUID;

import com.civicconnect.backend.request.RequestStatus;

public record TransitionCommand(RequestStatus target, String comment, UUID assigneeId, long expectedVersion) {

    public TransitionCommand {
        comment = comment == null || comment.isBlank() ? null : comment.trim();
    }
>>>>>>> Stashed changes
}
