package com.civicconnect.backend.notification;

import java.time.Instant;

/** An in-app message for a requester (FR-005). Email, SMS and push are out of scope (CR-04). */
public record Notification(Long id, Long requestId, String referenceNumber, Long recipientId,
                           String message, Instant createdAt, Instant readAt) {

    Notification markRead(Instant when) {
        return new Notification(id, requestId, referenceNumber, recipientId, message, createdAt, when);
    }
}
