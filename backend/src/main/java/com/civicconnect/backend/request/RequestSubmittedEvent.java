package com.civicconnect.backend.request;

import java.time.Instant;
import java.util.UUID;

public record RequestSubmittedEvent(UUID requestId, String referenceNumber, UUID requesterId, Instant timestamp) {
}
