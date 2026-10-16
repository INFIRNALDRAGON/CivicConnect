package com.civicconnect.backend.dto;

<<<<<<< Updated upstream
import com.civicconnect.backend.request.Priority;

/** Body of the "submit a request" call (FR-001). Validation happens in RequestService. */
public record CreateRequestDto(Long categoryId, String description, Priority priority) {
=======
import java.util.UUID;

import com.civicconnect.backend.request.Priority;

public record CreateRequestDto(UUID categoryId, String description, Priority priority) {
>>>>>>> Stashed changes
}
