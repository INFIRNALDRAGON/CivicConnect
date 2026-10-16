package com.civicconnect.backend.notification;

import java.time.Instant;
import java.util.UUID;

public record Notification(UUID id, UUID requestId, String referenceNumber, UUID recipientId,
                           String message, Instant createdAt, Instant readAt) {
}
