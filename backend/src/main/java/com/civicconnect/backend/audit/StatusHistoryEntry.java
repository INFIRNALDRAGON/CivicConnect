package com.civicconnect.backend.audit;

import java.time.Instant;

import com.civicconnect.backend.request.RequestStatus;

/** One immutable row of the audit trail (NFR-006). Mirrors the StatusHistory entity in the PED. */
public record StatusHistoryEntry(
        Long id,
        Long requestId,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        Long changedBy,
        Instant changedAt,
        String comment) {
}
