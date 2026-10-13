package com.civicconnect.backend.status;

import java.time.Instant;

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
        Instant timestamp,
        String comment) {
}
