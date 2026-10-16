package com.civicconnect.backend.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_request_overview")
class RequestTimesEntity {

    @Id
    UUID id;

    @Column(name = "submitted_at")
    Instant submittedAt;

    @Column(name = "assigned_at")
    Instant assignedAt;

    @Column(name = "resolved_at")
    Instant resolvedAt;

    protected RequestTimesEntity() {
    }
}
