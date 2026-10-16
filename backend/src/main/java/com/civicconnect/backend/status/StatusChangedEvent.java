package com.civicconnect.backend.status;

import java.time.Instant;
<<<<<<< Updated upstream

import com.civicconnect.backend.request.RequestStatus;

/**
 * Published once for every committed status change (ED-008, Observer pattern).
 * Beyond the five fields in the PED it carries the reference, requester and comment, so the
 * observers need no lookup back into the request module.
 */
public record StatusChangedEvent(
        Long requestId,
        String referenceNumber,
        Long requesterId,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        Long actorId,
=======
import java.util.UUID;

import com.civicconnect.backend.request.RequestStatus;

public record StatusChangedEvent(
        UUID requestId,
        String referenceNumber,
        UUID requesterId,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        UUID actorId,
>>>>>>> Stashed changes
        Instant timestamp,
        String comment) {
}
