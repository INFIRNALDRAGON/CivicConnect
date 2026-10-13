package com.civicconnect.backend.dto;

/** Body of the "assign" call. Staff leave {@code assigneeId} empty to accept the request themselves. */
public record AssignRequestDto(Long assigneeId, Long version) {
}
