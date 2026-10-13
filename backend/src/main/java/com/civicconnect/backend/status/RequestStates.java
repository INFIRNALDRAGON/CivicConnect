package com.civicconnect.backend.status;

import java.util.EnumMap;
import java.util.Map;

import com.civicconnect.backend.request.RequestStatus;

/** Lookup from the stored status value to its State object. States hold no data, so they are shared. */
public final class RequestStates {

    private static final Map<RequestStatus, RequestState> STATES = new EnumMap<>(RequestStatus.class);

    static {
        register(new SubmittedState());
        register(new AssignedState());
        register(new InProgressState());
        register(new ResolvedState());
        register(new ClosedState());
    }

    private RequestStates() {
    }

    private static void register(RequestState state) {
        STATES.put(state.status(), state);
    }

    public static RequestState of(RequestStatus status) {
        return STATES.get(status);
    }
}
