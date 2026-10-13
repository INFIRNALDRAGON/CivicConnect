package com.civicconnect.backend.notification;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.civicconnect.backend.request.RequestStatus;
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
            case SUBMITTED -> "Your request " + ref + " has been received.";
        };
    }
}
