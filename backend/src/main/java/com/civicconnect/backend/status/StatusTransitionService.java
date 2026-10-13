package com.civicconnect.backend.status;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;

/**
 * The single write path for request status (ASR-01, FR-010, FR-012).
 * <ol>
 *   <li>checks the caller may act on this request,</li>
 *   <li>asks the current State object whether the move is valid,</li>
 *   <li>saves with an optimistic-lock check (ASR-03),</li>
 *   <li>publishes {@link StatusChangedEvent} for the audit and notification observers.</li>
 * </ol>
 * The class knows nothing about audit or notifications (ED-008).
 */
@Service
public class StatusTransitionService {

    private static final Logger log = LoggerFactory.getLogger(StatusTransitionService.class);

    private final RequestService requests;
    private final AccessControlService access;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public StatusTransitionService(RequestService requests, AccessControlService access,
                                   ApplicationEventPublisher publisher, Clock clock) {
        this.requests = requests;
        this.access = access;
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * One transaction (ADR-M2-02): the status update and the synchronous audit listener succeed or fail together.
     * The notification listener runs after commit, so nobody is told about a change that was rolled back.
     */
    @Transactional
    public ServiceRequest transition(AuthenticatedUser actor, Long requestId, TransitionCommand cmd) {
        access.requireStaffOrManager(actor);
        ServiceRequest before = requests.requireById(requestId);
        access.requireCategoryAccess(actor, before.getCategoryId());

        RequestStatus from = before.getStatus();
        try {
            RequestStates.of(from).validateTransition(cmd.target(),
                    new TransitionContext(actor, before, cmd.comment(), cmd.assigneeId()));
        } catch (RuntimeException rejected) {
            // NFR-006: rejected attempts are visible too. Ids only, no personal data (NFR-003).
            log.warn("Rejected status change: request={} from={} to={} actor={} reason={}",
                    requestId, from, cmd.target(), actor.id(), rejected.getMessage());
            throw rejected;
        }

        Instant now = clock.instant();
        ServiceRequest updated = before.copy();
        updated.applyStatus(cmd.target(), now);
        if (cmd.target() == RequestStatus.ASSIGNED) {
            updated.assignTo(cmd.assigneeId(), now);
        }

        ServiceRequest saved = requests.saveStatusChange(updated, cmd.expectedVersion());
        try {
            publisher.publishEvent(new StatusChangedEvent(saved.getId(), saved.getReferenceNumber(),
                    saved.getRequesterId(), from, saved.getStatus(), actor.id(), now, cmd.comment()));
        } catch (RuntimeException listenerFailed) {
            // With a real database, @Transactional rolls the status update back by itself and this
            // manual undo is deleted. It exists only so the in-memory store keeps ASR-01 true:
            // a failed audit write must leave the status unchanged.
            requests.undoStatusChange(before);
            throw listenerFailed;
        }
        return saved;
    }
}
