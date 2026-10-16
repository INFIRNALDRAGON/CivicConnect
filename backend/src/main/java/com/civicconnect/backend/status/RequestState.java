package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.exception.InvalidStatusTransitionException;
import com.civicconnect.backend.request.RequestStatus;

public interface RequestState {

    RequestStatus status();

    Set<RequestStatus> allowedNext();

    default boolean isTerminal() {
        return allowedNext().isEmpty();
    }

    default void validateTransition(RequestStatus target, TransitionContext ctx) {
        if (!allowedNext().contains(target)) {
            String reason = isTerminal()
                    ? status().label() + " is a final status"
                    : "allowed next status: " + allowedNext().stream()
                            .sorted().map(RequestStatus::label).toList();
            throw new InvalidStatusTransitionException(status(), target, reason);
        }
        checkPreconditions(target, ctx);
    }

    default void checkPreconditions(RequestStatus target, TransitionContext ctx) {
    }
}
