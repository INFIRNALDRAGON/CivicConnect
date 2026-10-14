package com.civicconnect.backend.dto;

import com.civicconnect.backend.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body of POST /api/requests (FR-001, FR-002).
 * TEMPORARY: requesterId is sent by the client until authentication exists.
 * Once the RBAC decision is implemented, remove it and take the requester
 * from the authenticated user instead.
 */
public record CreateRequestDto(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 2000) String description,
        Priority priority,
        @NotNull UUID requesterId) {
}
