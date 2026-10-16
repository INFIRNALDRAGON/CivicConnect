package com.civicconnect.backend.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaNotificationStore implements NotificationStore {

    private final NotificationRepository notifications;

    JpaNotificationStore(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    @Transactional
    public Notification add(Notification n) {
        NotificationEntity entity = new NotificationEntity();
        entity.requestId = n.requestId();
        entity.recipientId = n.recipientId();
        entity.message = n.message().length() > 500 ? n.message().substring(0, 500) : n.message();
        entity.createdAt = n.createdAt();
        NotificationEntity saved = notifications.saveAndFlush(entity);
        return new Notification(saved.id, saved.requestId, n.referenceNumber(), saved.recipientId,
                saved.message, saved.createdAt, saved.readAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(UUID recipientId) {
        return notifications.findViewsByRecipient(recipientId);
    }

    @Override
    @Transactional
    public Optional<Notification> markRead(UUID id, UUID recipientId, Instant when) {
        Optional<NotificationEntity> found = notifications.findByIdAndRecipientId(id, recipientId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        NotificationEntity entity = found.get();
        if (entity.readAt == null) {
            entity.readAt = when;
            notifications.saveAndFlush(entity);
        }
        return notifications.findViewById(id);
    }
}
