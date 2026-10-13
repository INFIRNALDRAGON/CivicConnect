package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.request.RequestStatus;

/** Read-only. The only way out is Closed (CR-05). */
final class ResolvedState implements RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.RESOLVED;
    }

    @Override
    public Set<RequestStatus> allowedNext() {
        return Set.of(RequestStatus.CLOSED);
    }
}
