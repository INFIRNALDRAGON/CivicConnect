package com.civicconnect.backend.status;

<<<<<<< Updated upstream
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.request.ServiceRequest;

/** Everything a State object may need to decide whether a transition is allowed. */
public record TransitionContext(AuthenticatedUser actor, ServiceRequest request, String comment, Long assigneeId) {
=======
import java.util.UUID;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.request.ServiceRequest;

public record TransitionContext(AuthenticatedUser actor, ServiceRequest request, String comment, UUID assigneeId) {
>>>>>>> Stashed changes

    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }
}
