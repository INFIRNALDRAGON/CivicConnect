package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestStatus;

final class SubmittedState implements RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.SUBMITTED;
    }

    @Override
    public Set<RequestStatus> allowedNext() {
        return Set.of(RequestStatus.ASSIGNED, RequestStatus.REJECTED);
    }

    @Override
    public void checkPreconditions(RequestStatus target, TransitionContext ctx) {
        if (target == RequestStatus.ASSIGNED && ctx.assigneeId() == null) {
            throw new ValidationException("An assignee is required to move a request to Assigned");
        }
        if (target == RequestStatus.REJECTED && !ctx.hasComment()) {
            throw new ValidationException("A reason is required to reject a request");
        }
    }
}
