package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestStatus;

final class InProgressState implements RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.IN_PROGRESS;
    }

    @Override
    public Set<RequestStatus> allowedNext() {
        return Set.of(RequestStatus.RESOLVED);
    }

    /** FR-012 / CR-05: resolving needs a recorded resolution note. */
    @Override
    public void checkPreconditions(RequestStatus target, TransitionContext ctx) {
        if (!ctx.hasComment()) {
            throw new ValidationException("A resolution note is required to resolve a request");
        }
    }
}
