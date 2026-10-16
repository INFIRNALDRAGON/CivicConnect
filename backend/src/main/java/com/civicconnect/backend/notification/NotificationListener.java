package com.civicconnect.backend.notification;

<<<<<<< Updated upstream
import java.time.Instant;

import org.springframework.stereotype.Component;
=======
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
>>>>>>> Stashed changes
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.civicconnect.backend.request.RequestStatus;
<<<<<<< Updated upstream
import com.civicconnect.backend.status.StatusChangedEvent;

/**
 * Observer that turns a status change into a requester-facing message (FR-005).
 * Runs AFTER COMMIT so a requester is never told about a change that was rolled back (ADR-M2-02).
 * {@code fallbackExecution} lets it also run when there is no surrounding transaction (e.g. unit tests).
 * Messages never name the staff member handling the request.
 */
@Component
public class NotificationListener {

    private final NotificationStore store;

    public NotificationListener(NotificationStore store) {
        this.store = store;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(StatusChangedEvent event) {
        String message = messageFor(event);
        store.add(new Notification(null, event.requestId(), event.referenceNumber(), event.requesterId(),
                message, Instant.now(), null));
    }

    static String messageFor(StatusChangedEvent e) {
        String ref = e.referenceNumber();
        return switch (e.newStatus()) {
            case ASSIGNED -> "Your request " + ref + " has been accepted and assigned to a staff member.";
            case IN_PROGRESS -> "Work has started on your request " + ref + ".";
            case RESOLVED -> "Your request " + ref + " has been resolved."
                    + (e.comment() == null || e.comment().isBlank() ? "" : " Resolution: " + e.comment().trim());
            case CLOSED -> "Your request " + ref + " has been closed.";
=======
import com.civicconnect.backend.request.RequestSubmittedEvent;
import com.civicconnect.backend.status.StatusChangedEvent;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationStore store;
    private final Clock clock;

    public NotificationListener(NotificationStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRequestSubmitted(RequestSubmittedEvent event) {
        save(event.requestId(), event.referenceNumber(), event.requesterId(),
                messageFor(RequestStatus.SUBMITTED, event.referenceNumber(), null));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStatusChanged(StatusChangedEvent event) {
        save(event.requestId(), event.referenceNumber(), event.requesterId(), messageFor(event));
    }

    private void save(java.util.UUID requestId, String reference, java.util.UUID recipient, String message) {
        try {
            store.add(new Notification(null, requestId, reference, recipient, message, clock.instant(), null));
        } catch (RuntimeException failed) {
            log.error("Could not store notification for request {}", requestId, failed);
        }
    }

    static String messageFor(StatusChangedEvent e) {
        return messageFor(e.newStatus(), e.referenceNumber(), e.comment());
    }

    static String messageFor(RequestStatus status, String ref, String comment) {
        boolean hasComment = comment != null && !comment.isBlank();
        return switch (status) {
            case ASSIGNED -> "Your request " + ref + " has been accepted and assigned to a staff member.";
            case IN_PROGRESS -> "Work has started on your request " + ref + ".";
            case RESOLVED -> "Your request " + ref + " has been resolved."
                    + (hasComment ? " Resolution: " + comment.trim() : "");
            case CLOSED -> "Your request " + ref + " has been closed.";
            case REJECTED -> "Your request " + ref + " could not be accepted."
                    + (hasComment ? " Reason: " + comment.trim() : "");
>>>>>>> Stashed changes
            case SUBMITTED -> "Your request " + ref + " has been received.";
        };
    }
}
