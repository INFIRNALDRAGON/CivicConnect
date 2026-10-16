package com.civicconnect.backend.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.civicconnect.backend.notification.Notification;
import com.civicconnect.backend.notification.NotificationStore;

public class InMemoryNotificationStore implements NotificationStore {

    private final List<Notification> items = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(3000);

    @Override
    public synchronized Notification add(Notification n) {
        Notification stored = new Notification(Ids.of(ids.incrementAndGet()), n.requestId(), n.referenceNumber(),
                n.recipientId(), n.message(), n.createdAt(), null);
        items.add(stored);
        return stored;
    }

    @Override
    public synchronized List<Notification> findByRecipient(UUID recipientId) {
        return items.stream().filter(n -> n.recipientId().equals(recipientId))
                .sorted(Comparator.comparing(Notification::createdAt).reversed()
                        .thenComparing(Notification::id, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public synchronized Optional<Notification> markRead(UUID id, UUID recipientId, Instant when) {
        for (int i = 0; i < items.size(); i++) {
            Notification n = items.get(i);
            if (n.id().equals(id) && n.recipientId().equals(recipientId)) {
                Notification read = n.readAt() == null
                        ? new Notification(n.id(), n.requestId(), n.referenceNumber(), n.recipientId(),
                                n.message(), n.createdAt(), when)
                        : n;
                items.set(i, read);
                return Optional.of(read);
            }
        }
        return Optional.empty();
    }
}
