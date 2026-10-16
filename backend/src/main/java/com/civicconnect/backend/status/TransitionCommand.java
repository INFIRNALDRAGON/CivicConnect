package com.civicconnect.backend.status;

import java.util.UUID;

import com.civicconnect.backend.request.RequestStatus;

public record TransitionCommand(RequestStatus target, String comment, UUID assigneeId, long expectedVersion) {

    public TransitionCommand {
        comment = comment == null || comment.isBlank() ? null : comment.trim();
    }
}
