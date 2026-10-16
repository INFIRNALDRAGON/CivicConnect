package com.civicconnect.backend.audit;

import java.time.Instant;
<<<<<<< Updated upstream

import com.civicconnect.backend.request.RequestStatus;

/** One immutable row of the audit trail (NFR-006). Mirrors the StatusHistory entity in the PED. */
public record StatusHistoryEntry(
        Long id,
        Long requestId,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        Long changedBy,
=======
import java.util.UUID;

import com.civicconnect.backend.request.RequestStatus;

public record StatusHistoryEntry(
        UUID id,
        UUID requestId,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        UUID changedBy,
>>>>>>> Stashed changes
        Instant changedAt,
        String comment) {
}
