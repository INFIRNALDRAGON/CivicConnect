package com.civicconnect.backend.request;

import java.time.Instant;

/** An action, comment or resolution note recorded against a request (FR-011). */
public record RequestComment(Long id, Long requestId, Long authorId, String text, Instant createdAt) {
}
