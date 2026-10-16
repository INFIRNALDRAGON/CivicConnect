package com.civicconnect.backend.request;

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
@Table(name = "request_comments")
class RequestCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "request_id", nullable = false)
    UUID requestId;

    @Column(name = "author_id", nullable = false)
    UUID authorId;

    @Column(nullable = false)
    String body;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected RequestCommentEntity() {
    }
}
