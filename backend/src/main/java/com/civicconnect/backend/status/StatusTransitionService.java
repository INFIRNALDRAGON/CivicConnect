package com.civicconnect.backend.status;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;

@Service
public class StatusTransitionService {

    static final int MAX_COMMENT_LENGTH = 2000;

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

    @Transactional
    public ServiceRequest transition(AuthenticatedUser actor, UUID requestId, TransitionCommand cmd) {
        access.requireStaffOrManager(actor);
        ServiceRequest before = requests.requireById(requestId);
        access.requireCategoryAccess(actor, before.getCategoryId());

        if (cmd.comment() != null && cmd.comment().length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("Keep the note under " + MAX_COMMENT_LENGTH + " characters");
        }

        RequestStatus from = before.getStatus();
        try {
            RequestStates.of(from).validateTransition(cmd.target(),
                    new TransitionContext(actor, before, cmd.comment(), cmd.assigneeId()));
        } catch (RuntimeException rejected) {
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
        publisher.publishEvent(new StatusChangedEvent(saved.getId(), saved.getReferenceNumber(),
                saved.getRequesterId(), from, saved.getStatus(), actor.id(), now, cmd.comment()));
        return saved;
    }
}
