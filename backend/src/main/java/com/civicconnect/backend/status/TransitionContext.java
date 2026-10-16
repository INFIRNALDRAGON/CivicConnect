package com.civicconnect.backend.status;

import java.util.UUID;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.request.ServiceRequest;

public record TransitionContext(AuthenticatedUser actor, ServiceRequest request, String comment, UUID assigneeId) {

    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }
}
