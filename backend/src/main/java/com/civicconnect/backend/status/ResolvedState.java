package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.request.RequestStatus;

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
