package com.civicconnect.backend.audit;

import java.time.Instant;
import java.util.UUID;

import com.civicconnect.backend.request.RequestStatus;

public record StatusHistoryEntry(
        UUID id,
        UUID requestId,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        UUID changedBy,
        Instant changedAt,
        String comment) {
}
