package com.civicconnect.backend.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationStore {

    Notification add(Notification notification);

    List<Notification> findByRecipient(UUID recipientId);

    Optional<Notification> markRead(UUID id, UUID recipientId, Instant when);
}
