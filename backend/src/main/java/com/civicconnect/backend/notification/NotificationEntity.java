package com.civicconnect.backend.notification;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "request_id", nullable = false)
    UUID requestId;

    @Column(name = "recipient_id", nullable = false)
    UUID recipientId;

    @Column(nullable = false, length = 500)
    String message;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "read_at")
    Instant readAt;

    protected NotificationEntity() {
    }
}
