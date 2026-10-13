package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.request.RequestStatus;

/** Terminal. Every further transition is refused. */
final class ClosedState implements RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.CLOSED;
    }

    @Override
    public Set<RequestStatus> allowedNext() {
        return Set.of();
    }
}
