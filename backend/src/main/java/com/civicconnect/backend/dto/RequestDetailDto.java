package com.civicconnect.backend.dto;

import java.time.Instant;
import java.util.List;

import com.civicconnect.backend.request.RequestStatus;

/** Full request detail (FR-003, FR-008): the request, its status history and, for staff only, comments. */
public record RequestDetailDto(
        ServiceRequestDto request,
        List<HistoryEntry> history,
        List<CommentEntry> comments) {

    public record HistoryEntry(RequestStatus previousStatus, RequestStatus newStatus, Instant changedAt, String comment) {
    }

    public record CommentEntry(Long authorId, String text, Instant createdAt) {
    }
}
