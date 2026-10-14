package com.civicconnect.backend.dto;

import com.civicconnect.backend.model.Priority;
import com.civicconnect.backend.model.RequestStatus;
import com.civicconnect.backend.model.ServiceRequest;
import java.time.Instant;
import java.util.UUID;

/** What the API exposes. Entities (and fields such as password hashes) never leave the service layer. */
public record RequestResponseDto(
        UUID id,
        String referenceNumber,
        String category,
        String description,
        RequestStatus status,
        Priority priority,
        String requesterName,
        String assignedStaffName,
        Instant updatedAt) {

    public static RequestResponseDto from(ServiceRequest r) {
        return new RequestResponseDto(
                r.getId(),
                r.getReferenceNumber(),
                r.getCategory().getName(),
                r.getDescription(),
                r.getStatus(),
                r.getPriority(),
                r.getRequester().getName(),
                r.getAssignedStaff() == null ? null : r.getAssignedStaff().getName(),
                r.getUpdatedAt());
    }
}
