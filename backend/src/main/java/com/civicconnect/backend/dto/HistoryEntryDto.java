package com.civicconnect.backend.dto;

import com.civicconnect.backend.model.RequestStatus;
import com.civicconnect.backend.model.StatusHistory;
import java.time.Instant;

public record HistoryEntryDto(
        RequestStatus previousStatus,
        RequestStatus newStatus,
        String changedBy,
        Instant changedAt,
        String comment) {

    public static HistoryEntryDto from(StatusHistory h) {
        return new HistoryEntryDto(
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getChangedBy().getName(),
                h.getChangedAt(),
                h.getComment());
    }
}
