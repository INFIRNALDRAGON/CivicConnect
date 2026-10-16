package com.civicconnect.backend.dto;

import java.util.UUID;

import com.civicconnect.backend.request.Priority;

public record CreateRequestDto(UUID categoryId, String description, Priority priority) {
}
