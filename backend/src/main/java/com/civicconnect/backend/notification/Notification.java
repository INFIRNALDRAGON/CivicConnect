package com.civicconnect.backend.notification;

import java.time.Instant;
<<<<<<< Updated upstream

/** An in-app message for a requester (FR-005). Email, SMS and push are out of scope (CR-04). */
public record Notification(Long id, Long requestId, String referenceNumber, Long recipientId,
                           String message, Instant createdAt, Instant readAt) {

    Notification markRead(Instant when) {
        return new Notification(id, requestId, referenceNumber, recipientId, message, createdAt, when);
    }
=======
import java.util.UUID;

public record Notification(UUID id, UUID requestId, String referenceNumber, UUID recipientId,
                           String message, Instant createdAt, Instant readAt) {
>>>>>>> Stashed changes
}
