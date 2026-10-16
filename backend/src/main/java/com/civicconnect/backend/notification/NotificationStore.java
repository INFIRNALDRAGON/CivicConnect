package com.civicconnect.backend.notification;

import java.time.Instant;
<<<<<<< Updated upstream
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/** In-memory stand-in for the notifications table. */
@Component
public class NotificationStore {

    private final List<Notification> items = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    synchronized Notification add(Notification n) {
        Notification stored = new Notification(ids.incrementAndGet(), n.requestId(), n.referenceNumber(),
                n.recipientId(), n.message(), n.createdAt(), null);
        items.add(stored);
        return stored;
    }

    synchronized List<Notification> findByRecipient(Long recipientId) {
        return items.stream().filter(n -> n.recipientId().equals(recipientId))
                .sorted(Comparator.comparing(Notification::createdAt).reversed()).toList();
    }

    synchronized Optional<Notification> markRead(Long id, Long recipientId, Instant when) {
        for (int i = 0; i < items.size(); i++) {
            Notification n = items.get(i);
            if (n.id().equals(id) && n.recipientId().equals(recipientId)) {
                Notification read = n.readAt() == null ? n.markRead(when) : n;
                items.set(i, read);
                return Optional.of(read);
            }
        }
        return Optional.empty();
    }
=======
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationStore {

    Notification add(Notification notification);

    List<Notification> findByRecipient(UUID recipientId);

    Optional<Notification> markRead(UUID id, UUID recipientId, Instant when);
>>>>>>> Stashed changes
}
