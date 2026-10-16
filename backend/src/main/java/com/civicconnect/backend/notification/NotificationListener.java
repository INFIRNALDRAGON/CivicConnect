package com.civicconnect.backend.notification;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.civicconnect.backend.request.RequestStatus;
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
            case SUBMITTED -> "Your request " + ref + " has been received.";
        };
    }
}
