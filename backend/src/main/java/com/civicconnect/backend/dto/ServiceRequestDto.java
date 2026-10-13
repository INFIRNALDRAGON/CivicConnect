package com.civicconnect.backend.dto;

import java.time.Instant;

import com.civicconnect.backend.request.Priority;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;

/**
 * What the API returns for a request. {@code assignedStaffId} is only filled for staff and managers,
 * because requesters may not see who is handling their request (stakeholder analysis, NFR-003).
 */
public record ServiceRequestDto(
        Long id,
        String referenceNumber,
        Long categoryId,
        String categoryName,
        String description,
        RequestStatus status,
        String statusLabel,
        Priority priority,
        Long assignedStaffId,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static ServiceRequestDto from(ServiceRequest r, String categoryName, boolean includeAssignee) {
        return new ServiceRequestDto(
                r.getId(), r.getReferenceNumber(), r.getCategoryId(), categoryName, r.getDescription(),
                r.getStatus(), r.getStatus().label(), r.getPriority(),
                includeAssignee ? r.getAssignedStaffId() : null,
                r.getCreatedAt(), r.getUpdatedAt(), r.getVersion());
    }
}
