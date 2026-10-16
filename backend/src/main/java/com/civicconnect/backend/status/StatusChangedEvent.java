package com.civicconnect.backend.status;

import java.time.Instant;
import java.util.UUID;

import com.civicconnect.backend.request.RequestStatus;

public record StatusChangedEvent(
        UUID requestId,
        String referenceNumber,
        UUID requesterId,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        UUID actorId,
        Instant timestamp,
        String comment) {
}
