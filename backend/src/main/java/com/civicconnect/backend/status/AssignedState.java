package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.exception.ForbiddenException;
import com.civicconnect.backend.request.RequestStatus;

final class AssignedState implements RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.ASSIGNED;
    }

    @Override
    public Set<RequestStatus> allowedNext() {
        return Set.of(RequestStatus.IN_PROGRESS);
    }

    /** Only the owner (or a manager) starts the work. */
    @Override
    public void checkPreconditions(RequestStatus target, TransitionContext ctx) {
        boolean owner = ctx.actor().id().equals(ctx.request().getAssignedStaffId());
        if (!owner && !ctx.actor().isManager()) {
            throw new ForbiddenException("Only the assigned staff member or a manager can start this request");
        }
    }
}
