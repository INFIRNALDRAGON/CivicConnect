package com.civicconnect.backend.status;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.request.ServiceRequest;

/** Everything a State object may need to decide whether a transition is allowed. */
public record TransitionContext(AuthenticatedUser actor, ServiceRequest request, String comment, Long assigneeId) {

    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }
}
