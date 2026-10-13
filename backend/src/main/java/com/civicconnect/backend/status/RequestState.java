package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.exception.InvalidStatusTransitionException;
import com.civicconnect.backend.request.RequestStatus;

/**
 * State pattern (ED-007). Each status is an object that knows its own valid next statuses
 * and any extra rules for entering them, so no if/switch chains spread through the services.
 */
public interface RequestState {

    RequestStatus status();

    Set<RequestStatus> allowedNext();

    default boolean isTerminal() {
        return allowedNext().isEmpty();
    }

    /** Throws if the move is not allowed. Template method: shape check first, then state-specific rules. */
    default void validateTransition(RequestStatus target, TransitionContext ctx) {
        if (!allowedNext().contains(target)) {
            String reason = isTerminal()
                    ? status().label() + " is a final status"
                    : "allowed next status: " + allowedNext().stream().map(RequestStatus::label).toList();
            throw new InvalidStatusTransitionException(status(), target, reason);
        }
        checkPreconditions(target, ctx);
    }

    /** Hook for rules beyond "is this target allowed", e.g. a mandatory resolution note. */
    default void checkPreconditions(RequestStatus target, TransitionContext ctx) {
    }
}
