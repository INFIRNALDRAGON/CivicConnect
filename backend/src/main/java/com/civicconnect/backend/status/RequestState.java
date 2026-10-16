package com.civicconnect.backend.status;

import java.util.Set;

import com.civicconnect.backend.exception.InvalidStatusTransitionException;
import com.civicconnect.backend.request.RequestStatus;

<<<<<<< Updated upstream
/**
 * State pattern (ED-007). Each status is an object that knows its own valid next statuses
 * and any extra rules for entering them, so no if/switch chains spread through the services.
 */
=======
>>>>>>> Stashed changes
public interface RequestState {

    RequestStatus status();

    Set<RequestStatus> allowedNext();

    default boolean isTerminal() {
        return allowedNext().isEmpty();
    }

<<<<<<< Updated upstream
    /** Throws if the move is not allowed. Template method: shape check first, then state-specific rules. 
     *@param target
     *@param ctx
     */
=======
>>>>>>> Stashed changes
    default void validateTransition(RequestStatus target, TransitionContext ctx) {
        if (!allowedNext().contains(target)) {
            String reason = isTerminal()
                    ? status().label() + " is a final status"
<<<<<<< Updated upstream
                    : "allowed next status: " + allowedNext().stream().map(RequestStatus::label).toList();
=======
                    : "allowed next status: " + allowedNext().stream()
                            .sorted().map(RequestStatus::label).toList();
>>>>>>> Stashed changes
            throw new InvalidStatusTransitionException(status(), target, reason);
        }
        checkPreconditions(target, ctx);
    }

<<<<<<< Updated upstream
    /** Hook for rules beyond "is this target allowed", e.g. a mandatory resolution note. 
     *@param target
     *@param ctx     
     */
=======
>>>>>>> Stashed changes
    default void checkPreconditions(RequestStatus target, TransitionContext ctx) {
    }
}
