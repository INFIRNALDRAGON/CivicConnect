package com.civicconnect.backend.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "status_history")
class StatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "request_id", nullable = false)
    UUID requestId;

    @Column(name = "previous_status", length = 20)
    String previousStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    String newStatus;

    @Column(name = "changed_by", nullable = false)
    UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    Instant changedAt;

    @Column
    String comment;

    protected StatusHistoryEntity() {
    }
}
