package com.civicconnect.backend.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "service_requests")
class RequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "reference_number", insertable = false, updatable = false)
    String referenceNumber;

    @Column(name = "category_id", nullable = false)
    UUID categoryId;

    @Column(nullable = false)
    String description;

    @Column(nullable = false, length = 20)
    String status;

    @Column(nullable = false, length = 10)
    String priority;

    @Column(name = "requester_id", nullable = false)
    UUID requesterId;

    @Column(name = "assigned_staff_id")
    UUID assignedStaffId;

    @Column(name = "updated_at", insertable = false, updatable = false)
    Instant updatedAt;

    @Version
    @Column(nullable = false)
    Long version;

    protected RequestEntity() {
    }
}
