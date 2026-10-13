package com.civicconnect.backend.dto;

import com.civicconnect.backend.request.Priority;

/** Body of the "submit a request" call (FR-001). Validation happens in RequestService. */
public record CreateRequestDto(Long categoryId, String description, Priority priority) {
}
