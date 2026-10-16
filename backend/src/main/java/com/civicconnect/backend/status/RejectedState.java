package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.request.RequestStatus;

final class RejectedState implements RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.REJECTED;
    }

    @Override
    public Set<RequestStatus> allowedNext() {
        return Set.of();
    }
}
