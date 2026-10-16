package com.civicconnect.backend.request;

import java.time.Instant;
import java.util.UUID;

public record RequestComment(UUID id, UUID requestId, UUID authorId, String text, Instant createdAt) {
}
